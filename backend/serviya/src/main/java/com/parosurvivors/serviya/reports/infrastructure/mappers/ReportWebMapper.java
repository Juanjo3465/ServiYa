package com.parosurvivors.serviya.reports.infrastructure.mappers;

import com.parosurvivors.serviya.reports.application.dto.command.CreateClientReviewReportCommand;
import com.parosurvivors.serviya.reports.application.dto.command.CreateRequestReportCommand;
import com.parosurvivors.serviya.reports.application.dto.command.CreateServiceReviewReportCommand;
import com.parosurvivors.serviya.reports.application.dto.result.ReportDetailResult;
import com.parosurvivors.serviya.reports.domain.ClientReviewReport;
import com.parosurvivors.serviya.reports.domain.Report;
import com.parosurvivors.serviya.reports.domain.ReportAction;
import com.parosurvivors.serviya.reports.domain.RequestReport;
import com.parosurvivors.serviya.reports.domain.ServiceReviewReport;
import com.parosurvivors.serviya.reports.infrastructure.dto.form.CreateClientReviewReportForm;
import com.parosurvivors.serviya.reports.infrastructure.dto.form.CreateRequestReportForm;
import com.parosurvivors.serviya.reports.infrastructure.dto.form.CreateServiceReviewReportForm;
import com.parosurvivors.serviya.reports.infrastructure.dto.response.ClientReviewReportResponse;
import com.parosurvivors.serviya.reports.infrastructure.dto.response.ReportActionResponse;
import com.parosurvivors.serviya.reports.infrastructure.dto.response.ReportDetailResponse;
import com.parosurvivors.serviya.reports.infrastructure.dto.response.ReportResponse;
import com.parosurvivors.serviya.reports.infrastructure.dto.response.RequestReportResponse;
import com.parosurvivors.serviya.reports.infrastructure.dto.response.ServiceReviewReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper web (MapStruct) de reportes: Form->Command y dominio/Result->Response.
 * TODO: completar mapeos donde difieran los nombres de campo.
 */
@Mapper(componentModel = "spring")
public interface ReportWebMapper {

    @Mapping(target = "reporterId", source = "reporterId")
    CreateRequestReportCommand toCommand(CreateRequestReportForm form, Long reporterId);

    @Mapping(target = "reporterId", source = "reporterId")
    CreateServiceReviewReportCommand toCommand(CreateServiceReviewReportForm form, Long reporterId);

    @Mapping(target = "reporterId", source = "reporterId")
    CreateClientReviewReportCommand toCommand(CreateClientReviewReportForm form, Long reporterId);

    ReportResponse toResponse(Report report);

    ReportDetailResponse toResponse(ReportDetailResult result);

    ReportActionResponse toResponse(ReportAction action);

    RequestReportResponse toResponse(RequestReport report);

    ServiceReviewReportResponse toResponse(ServiceReviewReport report);

    ClientReviewReportResponse toResponse(ClientReviewReport report);

    List<ReportResponse> toReportResponses(List<Report> reports);

    List<ReportActionResponse> toActionResponses(List<ReportAction> actions);
}
