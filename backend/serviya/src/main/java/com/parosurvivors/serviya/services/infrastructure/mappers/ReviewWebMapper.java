package com.parosurvivors.serviya.services.infrastructure.mappers;

import com.parosurvivors.serviya.feedback.domain.ServiceReview;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.services.infrastructure.dto.response.ReviewResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ReviewWebMapper {
    
    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "createdAt", source = "review.createdAt")
    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "userPhotoUrl", source = "user.profilePhotoUrl")
    ReviewResponse toResponse(ServiceReview review, UserProfile user);

}
