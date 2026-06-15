package com.parosurvivors.serviya.services.infrastructure.repositories;

import com.parosurvivors.serviya.metrics.infrastructure.entities.ServiceMetricsEntity;
import com.parosurvivors.serviya.profiles.infrastructure.entities.UserProfileEntity;
import com.parosurvivors.serviya.services.application.dto.query.SearchServiceQuery;
import com.parosurvivors.serviya.services.infrastructure.entities.ServiceEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Especificacion JPA para busqueda dinamica de servicios.
 * Todos los filtros son opcionales (null = ignorado).
 * Siempre excluye registros con soft-delete (deletedAt IS NOT NULL).
 *
 * JOINs utilizados:
 *   - service_metrics  (LEFT JOIN por serviceId)  → filtros minRating / maxRating
 *   - user_profiles    (LEFT JOIN por offererId)  → filtro offererType
 *
 * Pendiente: filtros de geolocalización (latitude/longitude/maxDistanceKm) requieren
 * que ServiceEntity tenga coordenadas o un JOIN con addresses.
 */
public class ServiceSpecificationRepository {

    private ServiceSpecificationRepository() {}

    public static Specification<ServiceEntity> fromQuery(SearchServiceQuery q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Evitar filas duplicadas cuando hay JOINs con colecciones
            if (query != null) {
                query.distinct(true);
            }

            // ── Siempre excluir soft-deleted ─────────────────────────────────
            predicates.add(cb.isNull(root.get("deletedAt")));

            // ── Filtros sobre services ────────────────────────────────────────
            if (q.name() != null && !q.name().isBlank()) {
                Join<ServiceEntity, UserProfileEntity> profile = root.join("offererProfile", JoinType.LEFT);
                String pattern = "%" + q.name().toLowerCase() + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(profile.get("fullName")), pattern)
                ));
            }

            if (q.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), q.categoryId()));
            }

            if (q.offererId() != null) {
                predicates.add(cb.equal(root.get("offererId"), q.offererId()));
            }

            if (q.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("priceHourly"), q.minPrice()));
            }

            if (q.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceHourly"), q.maxPrice()));
            }

            if (q.available() != null) {
                predicates.add(cb.equal(root.get("active"), q.available()));
            }

            // ── JOIN con service_metrics → rating ─────────────────────────────
            // LEFT JOIN: servicios sin métricas aún siguen apareciendo en resultados
            if (q.minRating() != null || q.maxRating() != null) {
                Join<ServiceEntity, ServiceMetricsEntity> metrics =
                        root.join("serviceMetrics", JoinType.LEFT);

                if (q.minRating() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            metrics.get("averageRating"),
                            BigDecimal.valueOf(q.minRating())
                    ));
                }
                if (q.maxRating() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            metrics.get("averageRating"),
                            BigDecimal.valueOf(q.maxRating())
                    ));
                }
            }

            // ── Geolocalización: pendiente ────────────────────────────────────
            // latitude, longitude, maxDistanceKm no se aplican aún porque ServiceEntity
            // no tiene coordenadas. Se implementará cuando se agregue ese campo o el
            // JOIN con addresses.

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
