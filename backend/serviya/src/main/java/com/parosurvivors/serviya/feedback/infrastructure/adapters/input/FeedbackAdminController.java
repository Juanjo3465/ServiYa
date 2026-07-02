package com.parosurvivors.serviya.feedback.infrastructure.adapters.input;

import com.parosurvivors.serviya.feedback.application.dto.query.SearchAdminFeedbackQuery;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackAdminServicePort;
import com.parosurvivors.serviya.feedback.infrastructure.dto.response.AdminFeedbackItemResponse;
import com.parosurvivors.serviya.feedback.infrastructure.mappers.ServiceFeedbackWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/feedback")
@RequiredArgsConstructor
public class FeedbackAdminController {

    private final FeedbackAdminServicePort feedbackAdminService;
    private final ServiceFeedbackWebMapper mapper;

    @GetMapping
    public ResponseEntity<Page<AdminFeedbackItemResponse>> searchFeedback(
            SearchAdminFeedbackQuery query,
            Pageable pageable) {
        return ResponseEntity.ok(feedbackAdminService.searchFeedback(query, pageable)
                .map(mapper::toAdminResponse));
    }
}
