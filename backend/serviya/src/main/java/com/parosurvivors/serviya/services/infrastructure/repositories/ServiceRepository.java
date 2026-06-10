package com.parosurvivors.serviya.services.infrastructure.repositories;

import com.parosurvivors.serviya.services.infrastructure.entities.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByOffererId(Long offererId);

    @Query(value = """
        SELECT s.*
        FROM services s
        LEFT JOIN user_profiles up ON up.user_id = s.offerer_id
        WHERE (
            :name IS NULL
            OR LOWER(s.title) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(s.description) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(up.full_name) LIKE LOWER(CONCAT('%', :name, '%'))
        )
        AND (:categoryId IS NULL OR s.category_id = :categoryId)
        AND (:offererId IS NULL OR s.offerer_id = :offererId)
        AND (:minPrice IS NULL OR s.price_hourly >= :minPrice)
        AND (:maxPrice IS NULL OR s.price_hourly <= :maxPrice)
        AND (:available IS NULL OR s.is_active = :available)
        """, nativeQuery = true)
    List<ServiceEntity> search(
        @Param("name") String name,
        @Param("categoryId") Long categoryId,
        @Param("offererId") Long offererId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("available") Boolean available
    );
}
