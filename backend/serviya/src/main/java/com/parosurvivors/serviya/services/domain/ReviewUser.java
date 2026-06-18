package com.parosurvivors.serviya.services.domain;

import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.feedback.domain.ServiceReview;

public record ReviewUser(
    ServiceReview review,
    UserProfile user
) {}
