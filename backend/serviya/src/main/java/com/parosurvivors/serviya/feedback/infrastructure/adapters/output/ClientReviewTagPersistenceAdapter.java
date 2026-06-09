package com.parosurvivors.serviya.feedback.infrastructure.adapters.output;

import com.parosurvivors.serviya.feedback.application.ports.output.ClientReviewTagPersistencePort;
import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientReviewTagEntity;
import com.parosurvivors.serviya.feedback.infrastructure.repositories.ClientReviewTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientReviewTagPersistenceAdapter implements ClientReviewTagPersistencePort {

    private final ClientReviewTagRepository repository;

    @Override
    public void addTags(Long reviewId, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        List<ClientReviewTagEntity> links = tagIds.stream()
                .map(tagId -> {
                    ClientReviewTagEntity entity = new ClientReviewTagEntity();
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
                .map(ClientReviewTagEntity::getTagId)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByReviewId(Long reviewId) {
        repository.deleteAll(repository.findByReviewId(reviewId));
    }
}
