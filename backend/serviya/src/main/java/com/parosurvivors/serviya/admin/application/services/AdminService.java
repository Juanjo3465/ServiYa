package com.parosurvivors.serviya.admin.application.services;

import com.parosurvivors.serviya.admin.application.dto.command.CreateUserByAdminCommand;
import com.parosurvivors.serviya.admin.application.dto.query.AdminFeedbackSearchQuery;
import com.parosurvivors.serviya.admin.application.dto.result.AdminFeedbackSearchResult;
import com.parosurvivors.serviya.admin.application.dto.result.UserAdminDetailResult;
import com.parosurvivors.serviya.admin.application.ports.input.AdminServicePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ClientFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.application.ports.output.ServiceFeedbackPersistencePort;
import com.parosurvivors.serviya.feedback.domain.ClientFeedback;
import com.parosurvivors.serviya.feedback.domain.ServiceFeedback;
import com.parosurvivors.serviya.metrics.application.ports.input.ClientMetricsServicePort;
import com.parosurvivors.serviya.metrics.application.ports.input.OffererMetricsServicePort;
import com.parosurvivors.serviya.metrics.domain.ClientMetrics;
import com.parosurvivors.serviya.metrics.domain.OffererMetrics;
import com.parosurvivors.serviya.notifications.application.ports.input.NotificationServicePort;
import com.parosurvivors.serviya.profiles.application.ports.input.UserProfileServicePort;
import com.parosurvivors.serviya.profiles.domain.UserProfile;
import com.parosurvivors.serviya.reports.application.ports.input.ReportServicePort;
import com.parosurvivors.serviya.requests.application.dto.result.AdminRequestDetailResult;
import com.parosurvivors.serviya.requests.application.ports.input.ServiceRequestQueryServicePort;
import com.parosurvivors.serviya.services.application.ports.input.MarketplaceServicePort;
import com.parosurvivors.serviya.shared.exceptions.InvalidStateException;
import com.parosurvivors.serviya.users.application.dto.command.CreateUserAccountCommand;
import com.parosurvivors.serviya.users.application.dto.item.UserSummaryItem;
import com.parosurvivors.serviya.users.application.dto.query.SearchUsersQuery;
import com.parosurvivors.serviya.users.application.ports.input.UserCreationServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserDeletionServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserQueryServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserRoleServicePort;
import com.parosurvivors.serviya.users.application.ports.input.UserServicePort;
import com.parosurvivors.serviya.users.domain.RoleName;
import com.parosurvivors.serviya.users.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Administracion de usuarios del panel admin (contexto backoffice). Orquesta hacia los puertos de entrada
 * de los demas modulos (users, profiles, metrics, reports, requests), nunca sus repositorios. El gate de rol
 * ADMIN vive en el endpoint (Spring Security); aqui no se repite. Ver estructura-servicios.docx (modulo 9).
 */
@Component
@RequiredArgsConstructor
public class AdminService implements AdminServicePort {

    private final UserServicePort userServicePort;
    private final UserCreationServicePort userCreationServicePort;
    private final UserRoleServicePort userRoleServicePort;
    private final UserDeletionServicePort userDeletionServicePort;
    private final UserQueryServicePort userQueryServicePort;
    private final UserProfileServicePort userProfileServicePort;
    private final OffererMetricsServicePort offererMetricsServicePort;
    private final ClientMetricsServicePort clientMetricsServicePort;
    private final ReportServicePort reportServicePort;
    private final ServiceRequestQueryServicePort serviceRequestQueryServicePort;
    private final NotificationServicePort notificationServicePort;
    private final ServiceFeedbackPersistencePort serviceFeedbackPersistencePort;
    private final ClientFeedbackPersistencePort clientFeedbackPersistencePort;
    private final MarketplaceServicePort marketplaceServicePort;

    @Override
    public UserSummaryItem createUserByAdmin(CreateUserByAdminCommand command) {
        // Llamador expuesto del mecanismo de creacion: admite CUALQUIER rol, incluido ADMIN. Sin restriccion
        // de politica de rol aqui. El consentimiento se da por aceptado al crear desde el panel admin; la
        // unicidad de email la valida createUserAccount. Devuelve el read-model de listado (sin foto aun).
        CreateUserAccountCommand accountCommand = new CreateUserAccountCommand(
                command.email(), command.password(), command.fullName(), command.role(),
                command.documentType(), command.documentNumber(), command.phone(), true);
        User created = userCreationServicePort.createUserAccount(accountCommand);
        return new UserSummaryItem(created.getId(), created.getEmail(), command.fullName(), null,
                created.getBanned(), created.getDeletedAt(), created.getCreatedAt());
    }

    @Override
    @Transactional
    public void grantRoleByAdmin(Long adminId, Long userId, String roleName) {
        User user = userQueryServicePort.getUserById(userId);
        if (user.isDeleted()) {
            throw new InvalidStateException("No se puede asignar un rol a un usuario eliminado");
        }
        // Cualquier rol , por nombre. La existencia del rol y el duplicado los valida assignRole.
        userRoleServicePort.assignRole(userId, parseRole(roleName));
        // TODO(notif): opcionalmente notificar al usuario la concesion del rol.
    }

    private RoleName parseRole(String roleName) {
        try {
            return RoleName.valueOf(roleName == null ? "" : roleName.strip().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateException("Invalid role: " + roleName);
        }
    }

    @Override
    public Page<UserSummaryItem> searchUsers(SearchUsersQuery query, Pageable pageable) {
        return userQueryServicePort.searchUsers(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminDetailResult getUserAdminDetail(Long userId) {
        User user = userQueryServicePort.getUserById(userId);
        List<String> roles = userRoleServicePort.getUserRoles(userId).stream()
                .map(role -> role.getName().name())
                .toList();
        UserProfile profile = userProfileServicePort.getProfileInfo(userId);
        // Metricas de comportamiento leidas del modulo de metricas (fuente unica). getMainMetrics devuelve
        // ceros si el usuario no tiene ese rol, asi que ambos bloques vienen siempre presentes.
        OffererMetrics offererMetrics = offererMetricsServicePort.getMainMetrics(userId);
        ClientMetrics clientMetrics = clientMetricsServicePort.getMainMetrics(userId);
        // Reportes: moderacion, no metricas; conteo en el modulo reports.
        int reportsReceived = reportServicePort.countReportsByReportedUser(userId);
        int reportsSent = reportServicePort.countReportsByReporter(userId);
        return new UserAdminDetailResult(
                user.getId(), user.getEmail(), user.getBanned(), user.getDeletedAt(), user.getCreatedAt(), roles,
                profile.getFullName(), profile.getProfilePhotoUrl(), profile.getDocumentType(),
                profile.getDocumentNumber(), profile.getPhoneNumber(), profile.getBio(),
                profile.getProfileType() == null ? null : profile.getProfileType().name(),
                offererMetrics, clientMetrics, reportsReceived, reportsSent);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminRequestDetailResult getRequestDetailForAdmin(Long requestId) {
        // Vista administrativa (ambas partes). El gate de rol ADMIN vive en el endpoint (Spring Security).
        return serviceRequestQueryServicePort.getRequestDetailForAdmin(requestId);
    }

    @Override
    public void banUser(Long adminId, Long userId) {
        userServicePort.banUser(userId);
        // TODO(notif): notificar al usuario baneado por doble canal con motivos (RF-063/069).
    }

    @Override
    public void unbanUser(Long adminId, Long userId) {
        userServicePort.unbanUser(userId);
        // TODO(notif): notificar al usuario desbaneado por doble canal (RF-070/075).
    }

    @Override
    public void deleteUser(Long adminId, Long userId) {
        userDeletionServicePort.deleteUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedbackSearchResult> searchFeedback(AdminFeedbackSearchQuery query, Pageable pageable) {
        Stream<AdminFeedbackSearchResult> serviceStream = searchServiceFeedback(query);
        Stream<AdminFeedbackSearchResult> clientStream = searchClientFeedback(query);

        List<AdminFeedbackSearchResult> combined = Stream.concat(serviceStream, clientStream)
                .sorted(Comparator.comparing(AdminFeedbackSearchResult::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combined.size());
        List<AdminFeedbackSearchResult> pageContent = start >= combined.size() ? List.of() : combined.subList(start, end);
        return new PageImpl<>(pageContent, pageable, combined.size());
    }

    private Stream<AdminFeedbackSearchResult> searchServiceFeedback(AdminFeedbackSearchQuery query) {
        if (query.serviceId() == null && query.clientId() == null) {
            return Stream.empty();
        }
        List<ServiceFeedback> feedbacks;
        if (query.serviceId() != null) {
            feedbacks = serviceFeedbackPersistencePort.findByServiceId(query.serviceId(),
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        } else {
            feedbacks = serviceFeedbackPersistencePort.findByClientId(query.clientId(),
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        }
        return feedbacks.stream()
                .filter(f -> query.clientId() == null || query.clientId().equals(f.getClientId()))
                .filter(f -> query.serviceId() == null || query.serviceId().equals(f.getServiceId()))
                .filter(f -> query.keyword() == null || query.keyword().isBlank()
                        || (f.getComment() != null && f.getComment().toLowerCase().contains(query.keyword().toLowerCase())))
                .filter(f -> query.ratingMin() == null || (f.getRating() != null && f.getRating() >= query.ratingMin()))
                .filter(f -> query.ratingMax() == null || (f.getRating() != null && f.getRating() <= query.ratingMax()))
                .map(f -> new AdminFeedbackSearchResult(
                        "SERVICE", f.getId(), f.getRequestId(),
                        f.getClientId(), f.getServiceId(),
                        f.getRating(), f.getComment(), f.getCreatedAt()));
    }

    private Stream<AdminFeedbackSearchResult> searchClientFeedback(AdminFeedbackSearchQuery query) {
        if (query.clientId() == null && query.offererId() == null) {
            return Stream.empty();
        }
        List<ClientFeedback> feedbacks;
        if (query.clientId() != null && query.offererId() == null) {
            feedbacks = clientFeedbackPersistencePort.findByClientId(query.clientId(),
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        } else if (query.offererId() != null) {
            feedbacks = clientFeedbackPersistencePort.findByOffererId(query.offererId(),
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        } else {
            feedbacks = clientFeedbackPersistencePort.findByClientId(query.clientId(),
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        }
        return feedbacks.stream()
                .filter(f -> query.clientId() == null || query.clientId().equals(f.getClientId()))
                .filter(f -> query.offererId() == null || query.offererId().equals(f.getOffererId()))
                .filter(f -> query.keyword() == null || query.keyword().isBlank()
                        || (f.getComment() != null && f.getComment().toLowerCase().contains(query.keyword().toLowerCase())))
                .filter(f -> query.ratingMin() == null || (f.getRating() != null && f.getRating() >= query.ratingMin()))
                .filter(f -> query.ratingMax() == null || (f.getRating() != null && f.getRating() <= query.ratingMax()))
                .map(f -> new AdminFeedbackSearchResult(
                        "CLIENT", f.getId(), f.getRequestId(),
                        f.getOffererId(), f.getClientId(),
                        f.getRating(), f.getComment(), f.getCreatedAt()));
    }

    @Override
    public void deleteService(Long serviceId) {
        marketplaceServicePort.delete(serviceId);
    }
}
