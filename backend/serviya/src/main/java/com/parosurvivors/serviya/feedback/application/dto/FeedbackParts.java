package com.parosurvivors.serviya.feedback.application.dto;

/**
 * Indica el lado del flujo de feedback que ejecuta {@code FeedbackFlow}.
 * Las fachadas Service/ClientFeedbackService construyen la instancia y delegan submit/remove.
 */
public record FeedbackParts(FeedbackSide side) {

    public static FeedbackParts service() {
        return new FeedbackParts(FeedbackSide.SERVICE);
    }

    public static FeedbackParts client() {
        return new FeedbackParts(FeedbackSide.CLIENT);
    }
}
