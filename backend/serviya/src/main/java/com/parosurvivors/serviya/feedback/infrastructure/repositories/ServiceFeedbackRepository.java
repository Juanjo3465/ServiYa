package com.parosurvivors.serviya.feedback.infrastructure.repositories;

import com.parosurvivors.serviya.feedback.infrastructure.entities.ServiceFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceFeedbackRepository extends JpaRepository<ServiceFeedbackEntity, Long> {
    Optional<ServiceFeedbackEntity> findByRequestId(Long requestId);

    boolean existsByRequestId(Long requestId);
    List<ServiceFeedbackEntity> findByClientId(Long clientId);

    /**
     * Reseñas (feedback con comentario) más recientes de un servicio. Filtra directo por la columna
     * denormalizada service_id (sin JOIN/subconsulta a service_requests). Solo devuelve feedback con
     * comentario (las reseñas que se muestran en el detalle). El límite se controla con
     * {@link Pageable} (PageRequest.of(0, n)).
     */
    List<ServiceFeedbackEntity> findByServiceIdAndCommentIsNotNullOrderByCreatedAtDesc(Long serviceId, Pageable pageable);

    /** Todo el feedback (rating y/o reseña) de un servicio, para el listado completo (RF-040, RF-046). */
    Page<ServiceFeedbackEntity> findByServiceId(Long serviceId, Pageable pageable);

    /** Todo el feedback de servicio dejado por un cliente en cualquier servicio. */
    Page<ServiceFeedbackEntity> findByClientId(Long clientId, Pageable pageable);
    
    /** Calificación promedio de todos los feedback de servicio. */
    @Query("SELECT AVG(CAST(f.rating AS DOUBLE)) FROM ServiceFeedbackEntity f")
    Double findAverageRating();
}