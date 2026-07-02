package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackTagCatalogServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientFeedbackTagCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientFeedbackTagCatalogService implements ClientFeedbackTagCatalogServicePort {

    private final ClientFeedbackTagCatalogPersistencePort clientFeedbackTagCatalogPersistencePort;

    @Override
    public List<ClientFeedbackTagCatalog> getCatalog() {
        return clientFeedbackTagCatalogPersistencePort.findAll();
    }
}
