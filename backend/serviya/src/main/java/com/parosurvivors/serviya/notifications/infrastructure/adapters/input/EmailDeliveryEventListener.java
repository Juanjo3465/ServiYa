package com.parosurvivors.serviya.notifications.infrastructure.adapters.input;

import com.parosurvivors.serviya.notifications.application.events.EmailDeliveryRequestedEvent;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationDeliveryServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Adaptador de entrada (driving) que dispara el envío real de una entrega EMAIL una vez que la
 * transacción de negocio confirmó (AFTER_COMMIT). Fino: solo delega en el puerto, que ejecuta el
 * envío en una transacción NUEVA (REQUIRES_NEW). Análogo a los listeners de métricas.
 */
@Component
@RequiredArgsConstructor
public class EmailDeliveryEventListener {

    private final NotificationDeliveryServicePort notificationDeliveryServicePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailDeliveryRequested(EmailDeliveryRequestedEvent event) {
        notificationDeliveryServicePort.sendPendingEmail(event.deliveryId(), event.protectedData());
    }
}
