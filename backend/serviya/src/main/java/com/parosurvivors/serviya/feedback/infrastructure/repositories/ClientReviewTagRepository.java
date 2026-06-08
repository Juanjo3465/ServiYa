package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ClientReviewTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientReviewTagRepository extends JpaRepository<ClientReviewTagEntity, Long> {
    List<ClientReviewTagEntity> findByReviewId(Long reviewId);
}
