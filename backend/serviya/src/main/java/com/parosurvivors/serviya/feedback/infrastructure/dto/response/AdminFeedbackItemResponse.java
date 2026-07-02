package com.parosurvivors.serviya.feedback.infrastructure.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminFeedbackItemResponse(
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
