package com.parosurvivors.serviya.feedback.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Item unificado para listados admin de feedback (RF-048).
 */
public record AdminFeedbackItemResult(
        String feedbackType,
        Long requestId,
        Long serviceId,
        Long clientId,
        Long offererId,
        Integer rating,
        String comment,
        List<String> tags,
        LocalDateTime createdAt) {
}
