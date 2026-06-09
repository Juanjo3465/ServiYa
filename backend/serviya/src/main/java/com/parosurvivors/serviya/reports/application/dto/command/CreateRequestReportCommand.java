package com.parosurvivors.serviya.reports.application.dto.command;

/**
 * Entrada de aplicacion (Command) para reportar una solicitud. El reporterId proviene del JWT.
 */
public record CreateRequestReportCommand(
        Long reporterId,
        Long reportedUserId,
        String category,
        String reason,
        Long requestId) {
}
