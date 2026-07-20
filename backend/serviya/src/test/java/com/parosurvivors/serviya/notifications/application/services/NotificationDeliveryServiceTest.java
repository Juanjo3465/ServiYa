package com.parosurvivors.serviya.notifications.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.notifications.application.events.EmailDeliveryRequestedEvent;
import com.parosurvivors.serviya.notifications.application.ports.output.EmailPort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationChannelPersistencePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationDeliveryPersistencePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationPersistencePort;
import com.parosurvivors.serviya.notifications.domain.DeliveryStatus;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import com.parosurvivors.serviya.shared.events.application.ports.output.DomainEventPublisherPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Reglas del reintento programado de entregas fallidas (módulo 8): contabilización de intentos en cada
 * envío y reintento de las FAILED con {@code attempts < maxDeliveryAttempts}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock private NotificationDeliveryPersistencePort deliveryPort;
    @Mock private NotificationChannelPersistencePort channelPort;
    @Mock private NotificationPersistencePort notificationPort;
    @Mock private ObjectProvider<EmailPort> emailPortProvider;
    @Mock private DomainEventPublisherPort eventPublisher;

    @InjectMocks private NotificationDeliveryService service;

    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setMaxAttempts() {
        ReflectionTestUtils.setField(service, "maxDeliveryAttempts", MAX_ATTEMPTS);
    }

    private NotificationChannel channel(int id, String name) {
        return NotificationChannel.builder().id(id).name(name).build();
    }

    @Test
    void deliver_internal_marksSentAndCountsFirstAttempt() {
        when(channelPort.findById(1)).thenReturn(Optional.of(channel(1, "INTERNAL")));
        when(deliveryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationDelivery result = service.deliver(10L, 1L, Map.of());

        assertThat(result.getDeliveryStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(result.getAttempts()).isEqualTo(1);
    }

    @Test
    void deliver_email_isDeferred_savesPendingAndPublishesEvent() {
        when(channelPort.findById(2)).thenReturn(Optional.of(channel(2, "EMAIL")));
        when(deliveryPort.save(any())).thenAnswer(inv -> {
            NotificationDelivery d = inv.getArgument(0);
            d.setId(99L);
            return d;
        });

        NotificationDelivery result = service.deliver(10L, 2L, Map.of("actionUrl", "https://x"));

        // No se envía en línea: queda PENDING sin intentos, y el envío se delega vía evento AFTER_COMMIT.
        assertThat(result.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(result.getAttempts()).isZero();
        verify(emailPortProvider, never()).getIfUnique();
        ArgumentCaptor<EmailDeliveryRequestedEvent> captor = ArgumentCaptor.forClass(EmailDeliveryRequestedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().deliveryId()).isEqualTo(99L);
    }

    @Test
    void sendPendingEmail_emailWithoutPort_marksFailedAndCountsAttempt() {
        NotificationDelivery pending = NotificationDelivery.builder()
                .id(7L).notificationId(10L).channelId(2)
                .deliveryStatus(DeliveryStatus.PENDING).attempts(0).build();
        when(deliveryPort.findById(7L)).thenReturn(Optional.of(pending));
        when(channelPort.findById(2)).thenReturn(Optional.of(channel(2, "EMAIL")));
        when(emailPortProvider.getIfUnique()).thenReturn(null);

        service.sendPendingEmail(7L, Map.of());

        assertThat(pending.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(pending.getAttempts()).isEqualTo(1);
        verify(deliveryPort).update(pending);
    }

    @Test
    void retry_resendsFailedBelowMax_andSucceedsOnInternal() {
        NotificationDelivery failed = NotificationDelivery.builder()
                .id(5L).notificationId(10L).channelId(1)
                .deliveryStatus(DeliveryStatus.FAILED).attempts(1).build();
        when(deliveryPort.findByStatusAndAttemptsLessThan(DeliveryStatus.FAILED, MAX_ATTEMPTS))
                .thenReturn(List.of(failed));
        when(channelPort.findById(1)).thenReturn(Optional.of(channel(1, "INTERNAL")));

        int retried = service.retryFailedDeliveries();

        assertThat(retried).isEqualTo(1);
        assertThat(failed.getDeliveryStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(failed.getAttempts()).isEqualTo(2);
        verify(deliveryPort).update(failed);
    }

    @Test
    void retry_emailWithoutPort_staysFailed_butCountsTheAttempt() {
        NotificationDelivery failed = NotificationDelivery.builder()
                .id(6L).notificationId(11L).channelId(2)
                .deliveryStatus(DeliveryStatus.FAILED).attempts(2).build();
        when(deliveryPort.findByStatusAndAttemptsLessThan(DeliveryStatus.FAILED, MAX_ATTEMPTS))
                .thenReturn(List.of(failed));
        when(channelPort.findById(2)).thenReturn(Optional.of(channel(2, "EMAIL")));
        when(emailPortProvider.getIfUnique()).thenReturn(null);

        int retried = service.retryFailedDeliveries();

        assertThat(retried).isEqualTo(1);
        assertThat(failed.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(3); // agotó el máximo: no volverá a reintentarse
        verify(deliveryPort).update(failed);
    }

    @Test
    void retry_noneEligible_doesNothing() {
        when(deliveryPort.findByStatusAndAttemptsLessThan(DeliveryStatus.FAILED, MAX_ATTEMPTS))
                .thenReturn(List.of());

        int retried = service.retryFailedDeliveries();

        assertThat(retried).isZero();
        verify(deliveryPort, never()).update(any());
        verify(channelPort, never()).findById(any());
    }
}
