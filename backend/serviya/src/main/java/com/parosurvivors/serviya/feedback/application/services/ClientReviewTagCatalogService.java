package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ClientReviewTagCatalogServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientReviewTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientReviewTagCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de ClientReviewTagCatalogServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class ClientReviewTagCatalogService implements ClientReviewTagCatalogServicePort {

    private final ClientReviewTagCatalogPersistencePort clientReviewTagCatalogPersistencePort;

    @Override
    public List<ClientReviewTagCatalog> getCatalog() {
        throw new UnsupportedOperationException("TODO: getCatalog — placeholder, ver estructura-servicios.docx");
    }
}
