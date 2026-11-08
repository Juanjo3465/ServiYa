package com.parosurvivors.serviya.reports.api;

import com.parosurvivors.serviya.reports.api.dto.ReportActionSnapshot;
import com.parosurvivors.serviya.reports.api.dto.ReportSnapshot;

/**
 * Comandos publicos del modulo reports.
 */
public interface ReportCommandApi {

    ReportSnapshot createBaseReport(
            Long reporterId, Long reportedUserId, String type, String category, String reason);

    void resolveReport(Long reportId, Long adminId, String actionType);

    void closeReport(Long reportId, Long adminId);

    ReportActionSnapshot createAction(Long reportId, Long adminId, String actionType);
}
