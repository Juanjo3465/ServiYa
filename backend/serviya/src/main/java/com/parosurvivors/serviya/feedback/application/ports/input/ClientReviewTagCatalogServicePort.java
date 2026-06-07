package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.domain.ClientReviewTagCatalog;

import java.util.List;

/**
 * Puerto de entrada de ClientReviewTagCatalogService.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 5).
 */
public interface ClientReviewTagCatalogServicePort {

    List<ClientReviewTagCatalog> getCatalog();
}
