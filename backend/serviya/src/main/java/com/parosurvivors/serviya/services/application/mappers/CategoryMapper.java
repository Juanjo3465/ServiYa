package com.parosurvivors.serviya.services.application.mappers;

import com.parosurvivors.serviya.services.application.dto.CategoryRequest;
import com.parosurvivors.serviya.services.application.dto.CategoryResponse;
import com.parosurvivors.serviya.services.domain.Category;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toDomain(CategoryRequest request);

    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    void updateFromRequest(CategoryRequest request, @MappingTarget Category category);
}
