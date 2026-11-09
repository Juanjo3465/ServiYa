package com.parosurvivors.serviya.services.domain;

import com.parosurvivors.serviya.feedback.api.dto.ServiceFeedbackSnapshot;
import com.parosurvivors.serviya.profiles.api.dto.ProfileSnapshot;

/**
 * Feedback reciente de un servicio junto con el perfil publico de su autor.
 */
public record FeedbackUser(
        ServiceFeedbackSnapshot feedback,
        ProfileSnapshot user) {
}
