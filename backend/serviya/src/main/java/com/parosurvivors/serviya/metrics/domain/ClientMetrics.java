package com.parosurvivors.serviya.metrics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Métricas agregadas del usuario en su rol de cliente. Mapea la tabla {@code client_metrics}.
 * Se actualiza vía eventos de aplicación (ver CLAUDE.md, "Orchestration vs. events").
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientMetrics {
    private Long id;
    private Long clientId;
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;
    @Builder.Default
    private Integer totalRatings = 0;
    @Builder.Default
    private Integer totalReviews = 0;
    @Builder.Default
    private Integer totalPositiveTags = 0;
    @Builder.Default
    private Integer totalNegativeTags = 0;
    @Builder.Default
    private Integer totalAcceptedRequests = 0;
    @Builder.Default
    private Integer totalCompletedRequests = 0;
    @Builder.Default
    private Integer totalCancelledRequests = 0;
    @Builder.Default
    private Integer totalScheduledRequests = 0;
    @Builder.Default
    private Integer totalNotProvidedRequests = 0;
    private LocalDateTime updatedAt;

    // =====================================================
    // BUSINESS METHODS
    // =====================================================

    public void registerRating(int rating) {
        BigDecimal accumulated = averageRating.multiply(BigDecimal.valueOf(totalRatings))
                .add(BigDecimal.valueOf(rating));
        totalRatings++;
        averageRating = accumulated.divide(BigDecimal.valueOf(totalRatings), 2, RoundingMode.HALF_UP);
        touch();
    }

    public void removeRating(int rating) {
        if (totalRatings <= 1) {
            totalRatings = 0;
            averageRating = BigDecimal.ZERO;
        } else {
            BigDecimal accumulated = averageRating.multiply(BigDecimal.valueOf(totalRatings))
                    .subtract(BigDecimal.valueOf(rating));
            totalRatings--;
            averageRating = accumulated.divide(BigDecimal.valueOf(totalRatings), 2, RoundingMode.HALF_UP);
        }
        touch();
    }

    public void incrementReviews() {
        totalReviews++;
        touch();
    }

    public void decrementReviews() {
        totalReviews = Math.max(0, totalReviews - 1);
        touch();
    }

    public void addPositiveTags(int count) {
        totalPositiveTags += count;
        touch();
    }

    public void addNegativeTags(int count) {
        totalNegativeTags += count;
        touch();
    }

    public void incrementAccepted() {
        totalAcceptedRequests++;
        touch();
    }

    public void incrementCompleted() {
        totalCompletedRequests++;
        touch();
    }

    public void incrementCancelled() {
        totalCancelledRequests++;
        touch();
    }

    public void incrementScheduled() {
        totalScheduledRequests++;
        touch();
    }

    public void incrementNotProvided() {
        totalNotProvidedRequests++;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
