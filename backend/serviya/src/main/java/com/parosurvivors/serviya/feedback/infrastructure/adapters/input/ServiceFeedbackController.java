package com.parosurvivors.serviya.feedback.infrastructure.adapters.input;

import com.parosurvivors.serviya.feedback.application.dto.ReviewRequest;
import com.parosurvivors.serviya.feedback.application.dto.ServiceFeedbackResponse;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceReviewTagCatalogServicePort;
import com.parosurvivors.serviya.feedback.domain.ServiceReviewTagCatalog;
import com.parosurvivors.serviya.feedback.infrastructure.adapters.input.api.ServiceFeedbackApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada (REST) del feedback de servicio. Placeholder funcional;
 * documentacion en {@link ServiceFeedbackApi}.
 */
@RestController
@RequiredArgsConstructor
public class ServiceFeedbackController implements ServiceFeedbackApi {

    private final ServiceFeedbackServicePort serviceFeedbackService;
    private final ServiceReviewTagCatalogServicePort serviceReviewTagCatalogService;

    @Override
    @PostMapping("/api/v1/service-requests/{id}/feedback")
    public ResponseEntity<Void> submitServiceFeedback(@PathVariable Long id,
                                                      @RequestParam(required = false) Integer rating,
                                                      @RequestBody(required = false) ReviewRequest review) {
        serviceFeedbackService.submitServiceFeedback(currentUserId(), id, rating, review);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/api/v1/service-requests/{id}/feedback")
    public ResponseEntity<ServiceFeedbackResponse> getServiceFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(serviceFeedbackService.getServiceFeedback(id));
    }

    @Override
    @GetMapping("/api/v1/services/{id}/feedback")
    public ResponseEntity<Page<ServiceFeedbackResponse>> getServiceFeedbackList(@PathVariable Long id,
                                                                               Pageable pageable) {
        return ResponseEntity.ok(serviceFeedbackService.getServiceFeedbackList(id, pageable));
    }

    @Override
    @GetMapping("/api/v1/clients/{id}/service-feedback")
    public ResponseEntity<Page<ServiceFeedbackResponse>> getServiceFeedbackByClient(@PathVariable Long id,
                                                                                   Pageable pageable) {
        return ResponseEntity.ok(serviceFeedbackService.getServiceFeedbackByClient(id, pageable));
    }

    @Override
    @GetMapping("/api/v1/service-review-tags")
    public ResponseEntity<List<ServiceReviewTagCatalog>> getCatalog() {
        return ResponseEntity.ok(serviceReviewTagCatalogService.getCatalog());
    }

    /** TODO: reemplazar por el id extraido del JWT autenticado. */
    private Long currentUserId() {
        return 0L;
    }
}
