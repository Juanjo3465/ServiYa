package com.parosurvivors.serviya.notifications.domain;

/**
 * Nombres canónicos de los canales de entrega. Cada valor debe coincidir EXACTAMENTE con la columna
 * {@code name} de la tabla {@code notification_channels} (sembrada como INTERNAL, EMAIL): así los
 * llamadores expresan la intención por nombre y {@code NotificationService} resuelve el id real en un
 * único lugar, sin depender del orden de inserción del seed. Ver [[notifications-wiring]].
 */
public enum ChannelName {
    INTERNAL,
    EMAIL
}
