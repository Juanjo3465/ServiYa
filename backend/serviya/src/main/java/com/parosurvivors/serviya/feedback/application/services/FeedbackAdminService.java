package com.parosurvivors.serviya.feedback.application.services;

import com.parosurvivors.serviya.feedback.application.dto.query.SearchAdminFeedbackQuery;
import com.parosurvivors.serviya.feedback.application.dto.result.AdminFeedbackItemResult;
import com.parosurvivors.serviya.feedback.application.dto.result.ClientFeedbackResult;
import com.parosurvivors.serviya.feedback.application.dto.result.ServiceFeedbackResult;
import com.parosurvivors.serviya.feedback.application.ports.input.ClientFeedbackServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.FeedbackAdminServicePort;
import com.parosurvivors.serviya.feedback.application.ports.input.ServiceFeedbackServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedbackAdminService implements FeedbackAdminServicePort {

    private final ServiceFeedbackServicePort serviceFeedbackService;
    private final ClientFeedbackServicePort clientFeedbackService;

    @Override
    public Page<AdminFeedbackItemResult> searchFeedback(SearchAdminFeedbackQuery query, Pageable pageable) {
        String type = query.type() != null ? query.type().toUpperCase() : "ALL";
        List<AdminFeedbackItemResult> items = new ArrayList<>();

        if ("ALL".equals(type) || "SERVICE".equals(type) || "SERVICE_FEEDBACK".equals(type)) {
            if (query.serviceId() != null) {
                serviceFeedbackService.getServiceFeedbackList(query.serviceId(), Pageable.unpaged())
                        .forEach(result -> items.add(fromService(result)));
            } else if (query.clientId() != null) {
                serviceFeedbackService.getServiceFeedbackByClient(query.clientId(), Pageable.unpaged())
                        .forEach(result -> items.add(fromService(result)));
            }
        }

        if ("ALL".equals(type) || "CLIENT".equals(type) || "CLIENT_FEEDBACK".equals(type)) {
            if (query.clientId() != null) {
                clientFeedbackService.getClientFeedbackList(query.clientId(), Pageable.unpaged())
                        .forEach(result -> items.add(fromClient(result)));
            } else if (query.offererId() != null) {
                clientFeedbackService.getClientFeedbackByOfferer(query.offererId(), Pageable.unpaged())
                        .forEach(result -> items.add(fromClient(result)));
            }
        }

        items.sort(Comparator.comparing(AdminFeedbackItemResult::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<AdminFeedbackItemResult> pageContent = start >= items.size()
                ? List.of()
                : items.subList(start, end);
        return new PageImpl<>(pageContent, pageable, items.size());
    }

    private AdminFeedbackItemResult fromService(ServiceFeedbackResult result) {
        return new AdminFeedbackItemResult(
                "SERVICE",
                result.requestId(),
                result.serviceId(),
                result.clientId(),
                null,
                result.rating(),
                result.comment(),
                result.tags(),
                result.createdAt());
    }

    private AdminFeedbackItemResult fromClient(ClientFeedbackResult result) {
        return new AdminFeedbackItemResult(
                "CLIENT",
                result.requestId(),
                null,
                result.clientId(),
                result.offererId(),
                result.rating(),
                result.comment(),
                result.tags(),
                result.createdAt());
    }
}
