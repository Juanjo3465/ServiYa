package com.parosurvivors.serviya.reports.application.dto.command;

/**
 * Entrada de aplicacion (Command) para reportar una resena de cliente. El reporterId proviene del JWT.
 */
public record CreateClientReviewReportCommand(
        Long reporterId,
        Long reportedUserId,
        String category,
        String reason,
        Long clientReviewId) {
}
