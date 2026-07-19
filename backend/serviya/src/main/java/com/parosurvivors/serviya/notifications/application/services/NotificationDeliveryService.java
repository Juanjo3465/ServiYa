package com.parosurvivors.serviya.notifications.application.services;

import com.parosurvivors.serviya.notifications.application.dto.result.NotificationDeliveryResult;
import com.parosurvivors.serviya.notifications.application.events.EmailDeliveryRequestedEvent;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import com.parosurvivors.serviya.notifications.application.ports.output.EmailPort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationChannelPersistencePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationDeliveryPersistencePort;
import com.parosurvivors.serviya.notifications.application.ports.output.NotificationPersistencePort;
import com.parosurvivors.serviya.notifications.domain.ChannelName;
import com.parosurvivors.serviya.notifications.domain.DeliveryStatus;
import com.parosurvivors.serviya.notifications.domain.Notification;
import com.parosurvivors.serviya.notifications.domain.NotificationChannel;
import com.parosurvivors.serviya.notifications.domain.NotificationDelivery;
import com.parosurvivors.serviya.shared.events.application.ports.output.DomainEventPublisherPort;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryService implements NotificationDeliveryServicePort {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    /**
     * Tipos de notificación cuyo mensaje no tiene sentido sin {@code protectedData} y que por tanto NO
     * se reintentan (ver {@link #isWorthlessWithoutProtectedData}). Hoy solo el enlace de recuperación
     * de contraseña, cuyo único contenido útil es la URL con el token.
     */
    private static final Set<String> TYPES_REQUIRING_PROTECTED_DATA = Set.of("password_reset");

    private final NotificationDeliveryPersistencePort notificationDeliveryPersistencePort;
    private final NotificationChannelPersistencePort notificationChannelPersistencePort;
    private final NotificationPersistencePort notificationPersistencePort;
    private final ObjectProvider<EmailPort> emailPortProvider;
    private final DomainEventPublisherPort eventPublisher;

    /** Máximo de intentos de envío por entrega antes de rendirse. Configurable por entorno. */
    @Value("${serviya.notifications.max-delivery-attempts:3}")
    private int maxDeliveryAttempts;

    @Override
    public NotificationDelivery deliver(Long notificationId, Long channelId, Map<String, String> protectedData) {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(notificationId)
                .channelId(channelId.intValue())
                .deliveryStatus(DeliveryStatus.PENDING)
                .attempts(0)
                .build();

        Optional<NotificationChannel> channel = notificationChannelPersistencePort.findById(channelId.intValue());

        // EMAIL: llama a un proveedor externo → se difiere. Se persiste PENDING dentro de la transacción de
        // negocio y se publica un evento; el envío real ocurre AFTER_COMMIT en una transacción nueva, para
        // no bloquear ni revertir la operación de negocio. El resto de canales (INTERNAL) no hacen I/O y se
        // resuelven en línea.
        if (channel.map(c -> ChannelName.EMAIL.name().equals(c.getName())).orElse(false)) {
            NotificationDelivery saved = notificationDeliveryPersistencePort.save(delivery);
            eventPublisher.publish(new EmailDeliveryRequestedEvent(saved.getId(), protectedData));
            return saved;
        }

        delivery.registerAttempt();
        dispatch(delivery, channel, protectedData);
        return notificationDeliveryPersistencePort.save(delivery);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPendingEmail(Long deliveryId, Map<String, String> protectedData) {
        // Se ejecuta AFTER_COMMIT del publicador, en su PROPIA transacción: si el envío falla, solo esta
        // entrega queda FAILED (la recogerá el reintento programado); nada del negocio se revierte.
        NotificationDelivery delivery = notificationDeliveryPersistencePort.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Email delivery {} not found for AFTER_COMMIT send", deliveryId);
            return;
        }
        attemptDelivery(delivery, protectedData);
        notificationDeliveryPersistencePort.update(delivery);
    }

    @Override
    @Transactional
    public int retryFailedDeliveries() {
        List<NotificationDelivery> retryable = notificationDeliveryPersistencePort
                .findByStatusAndAttemptsLessThan(DeliveryStatus.FAILED, maxDeliveryAttempts);
        int retried = 0;
        for (NotificationDelivery delivery : retryable) {
            if (isWorthlessWithoutProtectedData(delivery)) {
                continue;
            }
            try {
                // protectedData NO se persiste: el reintento va sin datos protegidos, solo con el mensaje base.
                attemptDelivery(delivery, Map.of());
                notificationDeliveryPersistencePort.update(delivery);
                retried++;
            } catch (RuntimeException ex) {
                log.warn("Retry of delivery {} threw an exception; leaving it for the next run", delivery.getId(), ex);
            }
        }
        if (retried > 0) {
            log.info("Retried {} failed notification deliveries (max attempts {})", retried, maxDeliveryAttempts);
        }
        return retried;
    }

    /**
     * ¿Reintentar esta entrega produciría un mensaje inservible? {@code protectedData} viaja SOLO en el
     * evento del primer envío (no se persiste, a propósito), así que un reintento va sin él. Para la
     * mayoría de notificaciones eso solo significa perder el enlace de detalle. Pero hay tipos cuyo
     * contenido ES el dato protegido: reenviar "Restablece tu contraseña" sin el enlace le daría al
     * usuario un correo desconcertante y sin salida. Para esos, no reintentar es la conducta correcta —
     * el usuario simplemente vuelve a pedir el reset y se emite un token nuevo.
     */
    private boolean isWorthlessWithoutProtectedData(NotificationDelivery delivery) {
        return notificationPersistencePort.findById(delivery.getNotificationId())
                .map(notification -> TYPES_REQUIRING_PROTECTED_DATA.contains(notification.getNotificationType()))
                .orElse(false);
    }

    /**
     * Ejecuta UN intento de envío sobre la entrega (contabilizándolo) resolviendo el canal desde la BD y
     * marcando SENT/FAILED. Reutilizado por el reintento programado y por el envío EMAIL AFTER_COMMIT.
     */
    private void attemptDelivery(NotificationDelivery delivery, Map<String, String> protectedData) {
        delivery.registerAttempt();
        dispatch(delivery, notificationChannelPersistencePort.findById(delivery.getChannelId()), protectedData);
    }

    /** Envía por el canal ya resuelto y marca SENT/FAILED. No contabiliza el intento (lo hace el llamador). */
    private void dispatch(NotificationDelivery delivery, Optional<NotificationChannel> channel,
                          Map<String, String> protectedData) {
        channel.ifPresentOrElse(c -> {
            switch (c.getName()) {
                case "INTERNAL" -> delivery.markAsSent();
                case "EMAIL" -> {
                    EmailPort emailPort = emailPortProvider.getIfUnique();
                    if (emailPort != null) {
                        Notification notification = notificationPersistencePort
                                .findById(delivery.getNotificationId()).orElse(null);
                        if (notification != null && emailPort.send(
                                notification.getUserId(),
                                notification.getNotificationType(),
                                notification.getTitle(),
                                notification.getMessage(),
                                protectedData)) {
                            delivery.markAsSent();
                        } else {
                            delivery.markAsFailed();
                        }
                    } else {
                        log.warn("No EmailPort bean available — marking EMAIL delivery {} as FAILED",
                                delivery.getNotificationId());
                        delivery.markAsFailed();
                    }
                }
                default -> {
                    log.warn("Unknown channel '{}' — marking delivery {} as FAILED",
                            c.getName(), delivery.getNotificationId());
                    delivery.markAsFailed();
                }
            }
        }, () -> {
            log.warn("Channel ID {} not found — marking delivery {} as FAILED",
                    delivery.getChannelId(), delivery.getNotificationId());
            delivery.markAsFailed();
        });
    }

    @Override
    public Page<NotificationDeliveryResult> getDeliveries(Long userId, Boolean read, Long channelId, String status, Pageable pageable) {
        DeliveryStatus deliveryStatus = status != null ? DeliveryStatus.valueOf(status) : null;

        Page<NotificationDelivery> deliveries = notificationDeliveryPersistencePort
                .findDeliveriesByUserId(userId, read, channelId, deliveryStatus, pageable);

        List<Long> notifIds = deliveries.stream()
                .map(NotificationDelivery::getNotificationId)
                .distinct()
                .toList();

        Map<Long, Notification> notifMap = notificationPersistencePort.findAllById(notifIds).stream()
                .collect(Collectors.toMap(Notification::getId, Function.identity()));

        return deliveries.map(delivery -> {
            Notification notification = notifMap.get(delivery.getNotificationId());
            return new NotificationDeliveryResult(
                    delivery.getId(),
                    delivery.getNotificationId(),
                    delivery.getChannelId(),
                    delivery.getDeliveryStatus().name(),
                    delivery.getReadAt(),
                    delivery.getSentAt(),
                    notification != null ? notification.getNotificationType() : null,
                    notification != null ? notification.getTitle() : null,
                    notification != null ? notification.getMessage() : null,
                    notification != null ? notification.getEntityType() : null,
                    notification != null ? notification.getEntityId() : null,
                    notification != null ? notification.getCreatedAt() : null
            );
        });
    }

    @Override
    public String getDeliveryStatus(Long notificationId, Long channelId) {
        return notificationDeliveryPersistencePort
                .findByNotificationIdAndChannelId(notificationId, channelId.intValue())
                .map(d -> d.getDeliveryStatus().name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for notification " + notificationId + " and channel " + channelId));
    }

    @Override
    public boolean isRead(Long notificationId, Long channelId) {
        return notificationDeliveryPersistencePort
                .findByNotificationIdAndChannelId(notificationId, channelId.intValue())
                .map(NotificationDelivery::isRead)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery not found for notification " + notificationId + " and channel " + channelId));
    }

    @Override
    public void markAsRead(Long deliveryId) {
        NotificationDelivery delivery = notificationDeliveryPersistencePort.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + deliveryId));
        delivery.markAsRead();
        notificationDeliveryPersistencePort.update(delivery);
    }
}
