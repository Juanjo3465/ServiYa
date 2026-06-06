package com.parosurvivors.serviya.services.application.ports.input;

import com.parosurvivors.serviya.services.application.dto.CategoryRequest;
import com.parosurvivors.serviya.services.application.dto.CategoryResponse;

import java.util.List;
import java.util.Optional;

public interface MarketplaceCategoryPort {

    CategoryResponse create(CategoryRequest request);
    Optional<CategoryResponse> getById(Long id);
    List<CategoryResponse> getAll();
    
}
