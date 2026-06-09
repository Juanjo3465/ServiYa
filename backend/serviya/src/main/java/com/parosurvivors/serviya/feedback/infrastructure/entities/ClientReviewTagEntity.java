package com.parosurvivors.serviya.feedback.infrastructure.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Tabla puente {@code client_review_tags} (etiquetas asignadas a una reseña de cliente).
 */
@Entity
@Table(
        name = "client_review_tags",
        uniqueConstraints = @UniqueConstraint(name = "uq_client_review_tag", columnNames = {"review_id", "tag_id"})
)
@Getter
@Setter
public class ClientReviewTagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;
}
