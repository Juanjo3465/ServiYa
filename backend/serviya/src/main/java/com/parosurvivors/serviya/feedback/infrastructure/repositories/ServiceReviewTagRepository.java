package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceReviewTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceReviewTagRepository extends JpaRepository<ServiceReviewTagEntity, Long> {
    List<ServiceReviewTagEntity> findByReviewId(Long reviewId);
}
