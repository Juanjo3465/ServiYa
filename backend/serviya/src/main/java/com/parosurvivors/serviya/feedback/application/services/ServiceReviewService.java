package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ServiceReviewServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceReviewPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceReviewTagPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceReview;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceReviewServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceReviewService implements ServiceReviewServicePort {

    private final ServiceReviewPersistencePort serviceReviewPersistencePort;
    private final ServiceReviewTagPersistencePort serviceReviewTagPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ServiceReview createReview(Long clientId, Long requestId, String comment, List<Long> tagIds) {
        throw new UnsupportedOperationException("TODO: createReview — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteReview(Long requestId, Long clientId) {
        throw new UnsupportedOperationException("TODO: deleteReview — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public List<ServiceReview> getByServiceId(Long serviceId) {
        return serviceReviewPersistencePort.findByServiceId(serviceId);
    }

    @Override
    public List<ServiceReview> getByServiceIdThree(Long serviceId) {
        return serviceReviewPersistencePort.findTop3ByServiceId(serviceId);
    }
}
