package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackTagCatalogServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackTagCatalogPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedbackTagCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceFeedbackTagCatalogService implements ServiceFeedbackTagCatalogServicePort {

    private final ServiceFeedbackTagCatalogPersistencePort serviceFeedbackTagCatalogPersistencePort;

    @Override
    public List<ServiceFeedbackTagCatalog> getCatalog() {
        return serviceFeedbackTagCatalogPersistencePort.findAll();
    }
}
