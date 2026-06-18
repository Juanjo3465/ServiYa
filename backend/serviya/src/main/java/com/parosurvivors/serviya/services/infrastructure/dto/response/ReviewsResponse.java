package com.parosurvivors.serviya.services.infrastructure.dto.response;

import java.util.List;

public record ReviewsResponse(
    List<ReviewResponse> reviews
) {}
