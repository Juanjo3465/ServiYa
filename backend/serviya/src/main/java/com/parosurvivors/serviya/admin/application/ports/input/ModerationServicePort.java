package com.parosurvivors.serviya.admin.application.ports.input;

import com.parosurvivors.serviya.admin.application.dto.CreateReportRequest;

/**
 * Puerto de entrada de ModerationService — moderación de reportes y contenido (rol ADMIN).
 * El paso común privado finalizeReport del documento queda como detalle de implementación.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 9).
 */
public interface ModerationServicePort {

    void warnUser(int reportId, int adminId);

    void banUserFromReport(int reportId, int adminId);

    void revertFeedbackFromReport(int reportId, int adminId);

    void removeFeedbackDirectly(int adminId, CreateReportRequest reportData);

    void markRequestAsNotProvided(int reportId, int adminId);

    void closeReport(int reportId, int adminId);
}
