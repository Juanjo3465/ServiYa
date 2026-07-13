package com.parosurvivors.serviya.profiles.application.ports.input;

import com.parosurvivors.serviya.profiles.application.dto.command.UpdateOffererProfileCommand;
import com.parosurvivors.serviya.profiles.application.dto.result.OffererPublicProfileResult;
import com.parosurvivors.serviya.profiles.domain.OffererProfile;
import com.parosurvivors.serviya.profiles.domain.OffererProfileSummary;

/**
 * Puerto de entrada de OffererProfileService. Recibe Commands y devuelve dominio/read model;
 * nunca tipos web. Ver documents/project-structure/estructura-servicios.docx (módulo 2).
 */
public interface OffererProfileServicePort {

    OffererProfile getPublicProfile(Long userId);

    /**
     * RF-027: perfil publico COMPLETO (identidad, especialidad, reputacion, metricas de desempeño y
     * servicios activos). Accesible sin autenticacion; no expone PII sensible.
     */
    OffererPublicProfileResult getPublicProfileDetail(Long userId);

    OffererProfileSummary getProfileSummary(Long userId);

    OffererProfile patchOffererProfile(UpdateOffererProfileCommand command);
}
