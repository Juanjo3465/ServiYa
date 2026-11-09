package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.metrics.api.MetricsQueryApi;
import com.parosurvivors.serviya.metrics.api.dto.OffererMetricsSnapshot;
import com.parosurvivors.serviya.profiles.application.dto.result.OffererPublicProfileResult;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererPublicProfileServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.OffererProfilePersistencePort;
import com.parosurvivors.serviya.profiles.application.ports.output.UserProfilePersistencePort;
import com.parosurvivors.serviya.profiles.domain.OffererProfile;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.services.api.ServiceQueryApi;
import com.parosurvivors.serviya.services.api.dto.CategorySnapshot;
import com.parosurvivors.serviya.services.api.dto.ServiceSnapshot;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compone la vitrina PUBLICA de un oferente (RF-027): identidad, especialidad, reputacion, metricas de
 * desempeño y sus servicios activos.
 *
 * <p>Es un bean separado de {@code OffererProfileService} para evitar un ciclo de dependencias:
 * MarketplaceService ya depende de OffererProfileServicePort (enriquece cada servicio con su oferente),
 * asi que si OffererProfileService dependiera a su vez del marketplace el contexto de Spring no
 * arrancaria. Aqui la dependencia fluye en un solo sentido.</p>
 */
@Component
@RequiredArgsConstructor
public class OffererPublicProfileService implements OffererPublicProfileServicePort {

    private final OffererProfilePersistencePort offererProfilePersistencePort;
    private final UserProfilePersistencePort userProfilePersistencePort;
    private final MetricsQueryApi metricsQueryApi;
    private final ServiceQueryApi serviceQueryApi;

    /**
     * Endpoint publico (visitantes sin sesion), por eso NO se expone ningun PII sensible: el documento y
     * el telefono personal quedan fuera; solo va el whatsapp que el oferente publica como contacto.
     *
     * <p>Solo se listan los servicios ACTIVOS: los desactivados (p. ej. tras eliminar la cuenta, RF-008)
     * no deben aparecer en la vitrina publica.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public OffererPublicProfileResult getPublicProfileDetail(Long userId) {
        OffererProfile offererProfile = offererProfilePersistencePort.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offerer profile not found for userId: " + userId));
        UserProfile userProfile = userProfilePersistencePort.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User profile not found for userId: " + userId));

        // getOffererMainMetrics devuelve ceros si aun no tiene actividad: el perfil siempre se puede pintar.
        OffererMetricsSnapshot metrics = metricsQueryApi.getOffererMainMetrics(userId);

        List<OffererPublicProfileResult.PublishedService> services = activeServicesOf(userId);

        return new OffererPublicProfileResult(
                userId,
                userProfile.getFullName(),
                userProfile.getProfilePhotoUrl(),
                offererProfile.getSpecialty(),
                offererProfile.getPublicDescription(),
                offererProfile.getWhatsappNumber(),
                metrics.averageRating(),
                metrics.totalRatings(),
                metrics.totalComments(),
                metrics.totalPositiveTags(),
                metrics.totalNegativeTags(),
                metrics.totalCompletedServices(),
                metrics.totalCancelledServices(),
                metrics.totalNotProvidedServices(),
                services);
    }

    /**
     * Servicios ACTIVOS del oferente, o lista vacia si aun no publica ninguno.
     *
     * <p>{@code getByOffererId} lanza ResourceNotFoundException cuando el oferente no tiene servicios.
     * Eso tiene sentido en la vista de "mis servicios", pero NO aqui: un oferente recien registrado —o
     * uno cuyos servicios se desactivaron— sigue teniendo un perfil publico valido que debe poder verse,
     * simplemente con la vitrina vacia. Por eso se traduce a una lista vacia en lugar de un 404.</p>
     */
    private List<OffererPublicProfileResult.PublishedService> activeServicesOf(Long userId) {
        List<ServiceSnapshot> services = serviceQueryApi.findByOffererId(userId);
        if (services.isEmpty()) {
            return List.of();
        }
        Map<Long, String> categoryNames = serviceQueryApi.getCategories().stream()
                .collect(Collectors.toMap(CategorySnapshot::id, CategorySnapshot::name));
        return services.stream()
                .filter(ServiceSnapshot::active)
                .map(service -> new OffererPublicProfileResult.PublishedService(
                        service.id(),
                        service.title(),
                        service.description(),
                        service.priceHourly(),
                        categoryNames.get(service.categoryId()),
                        service.averageDurationMinutes()))
                .toList();
    }
}
