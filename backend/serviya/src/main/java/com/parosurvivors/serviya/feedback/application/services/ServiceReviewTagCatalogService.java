package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ServiceReviewTagCatalogServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceReviewTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ServiceReviewTagCatalogServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ServiceReviewTagCatalogService implements ServiceReviewTagCatalogServicePort {

    private final ServiceReviewTagCatalogPersistencePort serviceReviewTagCatalogPersistencePort;

    @Override
    public List<ServiceReviewTagCatalog> getCatalog() {
        throw new UnsupportedOperationException("TODO: getCatalog — placeholder, ver estructura-servicios.docx");
    }
}
