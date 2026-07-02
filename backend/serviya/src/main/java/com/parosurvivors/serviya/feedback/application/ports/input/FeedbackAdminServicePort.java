package com.parosurvivors.serviya.feedback.application.ports.input;

import com.parosurvivors.serviya.feedback.application.dto.query.SearchAdminFeedbackQuery;
import com.parosurvivors.serviya.feedback.application.dto.result.AdminFeedbackItemResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackAdminServicePort {

    Page<AdminFeedbackItemResult> searchFeedback(SearchAdminFeedbackQuery query, Pageable pageable);
}
