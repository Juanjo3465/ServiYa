package com.parosurvivors.serviya.reports.application.ports.input;

import com.parosurvivors.serviya.reports.domain.ReportAction;

import java.util.List;

/**
 * Puerto de entrada de ReportActionService — trazabilidad de acciones de admin sobre reportes.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 7).
 */
public interface ReportActionServicePort {

    ReportAction createAction(int reportId, int adminId, String actionTaken);

    List<ReportAction> getActionsByReport(int reportId);
}
