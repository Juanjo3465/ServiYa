package com.parosurvivors.serviya.notifications.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notificación generada por el sistema (registro base, sin secretos). Mapea la tabla
 * {@code notifications}. Los datos sensibles del mensaje final ({@code protectedData})
 * NO se persisten aquí; los arma el adaptador de cada canal.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    private Long id;
    private Long userId;
    private String notificationType;
    private String title;
    private String message;
    private String entityType;
    private Long entityId;
    private LocalDateTime createdAt;

    // =====================================================
    // BUSINESS METHODS
    // =====================================================

    public boolean referencesEntity() {
        return entityType != null && entityId != null;
    }
}
