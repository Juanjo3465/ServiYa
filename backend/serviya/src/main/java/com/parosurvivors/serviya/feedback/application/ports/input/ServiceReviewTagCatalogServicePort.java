package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;

import java.util.List;

/**
 * Puerto de entrada de ServiceReviewTagCatalogService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ServiceReviewTagCatalogServicePort {

    List<ServiceReviewTagCatalog> getCatalog();
}
