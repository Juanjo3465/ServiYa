package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ServiceReviewTagPersistencePort;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceReviewTagEntity;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ServiceReviewTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceReviewTagPersistenceAdapter implements ServiceReviewTagPersistencePort {

    private final ServiceReviewTagRepository repository;

    @Override
    public void addTags(Long reviewId, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        List<ServiceReviewTagEntity> links = tagIds.stream()
                .map(tagId -> {
                    ServiceReviewTagEntity entity = new ServiceReviewTagEntity();
                    entity.setReviewId(reviewId);
                    entity.setTagId(tagId);
                    return entity;
                })
                .collect(Collectors.toList());
        repository.saveAll(links);
    }

    @Override
    public List<Long> findTagIdsByReviewId(Long reviewId) {
        return repository.findByReviewId(reviewId).stream()
                .map(ServiceReviewTagEntity::getTagId)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByReviewId(Long reviewId) {
        repository.deleteAll(repository.findByReviewId(reviewId));
    }
}
