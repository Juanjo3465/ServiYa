package com.parosurvivors.serviya.admin.application.ports.input;

import com.parosurvivors.serviya.admin.application.dto.command.RemoveFeedbackCommand;

/**
 * Puerto de entrada de ModerationService — moderación de reportes y contenido (rol ADMIN).
 * Las acciones sobre reportes usan escalares (reportId + adminId del JWT); removeFeedbackDirectly recibe Command.
 * El paso común privado finalizeReport del documento queda como detalle de implementación.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 9).
 */
public interface ModerationServicePort {

    void warnUser(Long reportId, Long adminId);

    void banUserFromReport(Long reportId, Long adminId);

    void revertFeedbackFromReport(Long reportId, Long adminId);

    void removeFeedbackDirectly(RemoveFeedbackCommand command);

    void markRequestAsNotProvided(Long reportId, Long adminId);

    void closeReport(Long reportId, Long adminId);
}
