package com.parosurvivors.serviya.services.application.services;

import com.parosurvivors.serviya.services.application.dto.CategoryRequest;
import com.parosurvivors.serviya.services.application.dto.CategoryResponse;
import com.parosurvivors.serviya.services.application.mappers.CategoryMapper;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceCategoryPort;
import com.parosurvivors.serviya.services.application.ports.output.CategoryPersistencePort;
import com.parosurvivors.serviya.services.domain.Category;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketplaceCategory implements MarketplaceCategoryPort {
    
    private final CategoryPersistencePort persistencePort;
    private final CategoryMapper mapper;
    
    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = mapper.toDomain(request);
        
        Category saved = persistencePort.save(category);
        return mapper.toResponse(saved);
    }

    @Override
    public Optional<CategoryResponse> getById(Long id) {
        return persistencePort.findById(id)
                .map(mapper::toResponse);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return persistencePort.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
    
    
}
