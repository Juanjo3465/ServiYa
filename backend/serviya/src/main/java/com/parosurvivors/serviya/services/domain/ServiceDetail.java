package com.parosurvivors.serviya.services.domain;

import com.parosurvivors.serviya.metrics.api.dto.OffererMetricsSnapshot;
import com.parosurvivors.serviya.metrics.api.dto.ServiceMetricsSnapshot;
import com.parosurvivors.serviya.profiles.api.dto.OffererProfileSnapshot;
import com.parosurvivors.serviya.profiles.api.dto.OffererProfileSummarySnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Vista compuesta del servicio para el detalle: datos propios + snapshots publicos de profiles,
 * metrics y feedback. Los DTOs de otros modulos entran como snapshots de su API, no como dominio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDetail {
    private Service service;
    private Category category;
    private OffererProfileSnapshot offererProfile;
    private OffererProfileSummarySnapshot offererSummary;
    private List<FeedbackUser> feedbackUsers;
    private List<ServiceAvailability> availability;
    private ServiceMetricsSnapshot serviceMetrics;
    private OffererMetricsSnapshot offererMetrics;
}
