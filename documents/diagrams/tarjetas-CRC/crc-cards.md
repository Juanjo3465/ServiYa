# CRC Cards — Services Marketplace

**CRC (Class – Responsibility – Collaborator)** cards for the service layer, organized by the system's 8 modules.

Each card describes, in conceptual language (not technical signatures):
- **Class** — the name of the service.
- **Responsibilities** — what it knows how to do / what it is in charge of.
- **Collaborators** — other classes (services, ports, or domain entities) it depends on to fulfill its task.

> Collaborations marked as *(event)* are not direct calls: the collaborator reacts to a published event (decoupling via Spring Application Events).

---

## Module 1 — Auth & Users

### UserAuthenticationService  ·  «orchestrator»
| Responsibilities | Collaborators |
|---|---|
| Authenticate users (login) and issue the JWT. | UserService |
| Orchestrate the full registration within a transaction (user, consent, profile, role). | ConsentService |
| Validate that the role to be assigned is CLIENT or OFFERER (never ADMIN). | UserRoleService |
| Coordinate password recovery (request and confirmation). | PasswordResetTokenService, NotificationService |

### UserService
| Responsibilities | Collaborators |
|---|---|
| Create the base user record (with password hash). | — (user repository) |
| Manage the existing user: change password and email. | — |
| Ban and unban users. | — |

### UserDeletionService  ·  «orchestrator»
| Responsibilities | Collaborators |
|---|---|
| Orchestrate the deletion (soft delete) of a user within a transaction. | UserService |
| If an offerer: deactivate their services and cancel the requests received. | ServiceManagementService |
| If a client: cancel the requests created. Notify the counterpart. | ServiceRequestCommandService, NotificationService |

### AdminService
| Responsibilities | Collaborators |
|---|---|
| Create administrators (outside of public registration). | UserService |
| Search/list users and assemble the detail with counts for the admin panel. | UserDeletionService |
| Ban, unban, and delete users, notifying the affected party. | NotificationService |

### RoleService
| Responsibilities | Collaborators |
|---|---|
| Provide the catalog of system roles. | — |

### UserRoleService
| Responsibilities | Collaborators |
|---|---|
| Query a user's roles and check whether they have one. | — |
| Assign and remove roles from a user. | — |

### ConsentService
| Responsibilities | Collaborators |
|---|---|
| Record the consent to terms during registration. | — |
| Check whether a user has consented. | — |

### PasswordResetTokenService
| Responsibilities | Collaborators |
|---|---|
| Generate and persist the recovery token (hashed). | — |
| Validate the token (matching, expiration, usage). | — |
| Mark the token as used. | — |

---

## Module 2 — Profiles

### UserProfileService
| Responsibilities | Collaborators |
|---|---|
| Provide the user's personal info (with decrypted PII). | AddressService |
| Update the profile via form (partial PATCH), encrypting PII. | — |
| Change the primary address, validating ownership. | — |

### AddressService
| Responsibilities | Collaborators |
|---|---|
| Manage a user's addresses (create, edit, delete, list). | GeocodingPort |
| Encrypt the address line (PII) and obtain coordinates. | — |

### OffererProfileService
| Responsibilities | Collaborators |
|---|---|
| Provide the public profile and the offerer summary (includes WhatsApp and metrics). | OffererMetricsService |
| Update the offerer profile via form (partial PATCH). | — |

### OffererAvailabilityService
| Responsibilities | Collaborators |
|---|---|
| Provide the offerer's availability schedule. | — |
| Save the full calendar (bulk replacement, transactional). | — |
| Manage individual time slots (create, edit, delete, activate). | — |

---

## Module 3 — Services

### CategoryService
| Responsibilities | Collaborators |
|---|---|
| Provide the catalog of service categories. | — |

### ServiceQueryService
| Responsibilities | Collaborators |
|---|---|
| Provide summaries and the detail page of a service. | ServiceReviewService |
| Compose the detail with rating, reviews, and offerer summary. | OffererProfileService |
| List and count an offerer's services. | — |

### ServiceManagementService
| Responsibilities | Collaborators |
|---|---|
| Create the service with its metric and time slots (transactional). | ServiceMetricsService, ServiceAvailabilityService |
| Edit service attributes (PATCH) and manage its lifecycle. | — |
| Activate/deactivate services; deactivate all of an offerer's services. | — |

### ServiceSearchService
| Responsibilities | Collaborators |
|---|---|
| Search services with compound filters (text, category, price, proximity, score, availability). | — |

### ServiceAvailabilityService
| Responsibilities | Collaborators |
|---|---|
| Provide a service's availability schedule. | — |
| Save the service's full calendar (bulk replacement, transactional). | — |
| Manage the service's individual time slots. | — |

---

## Module 4 — Service Requests

### ServiceRequestQueryService
| Responsibilities | Collaborators |
|---|---|
| List a client's/offerer's requests filtered by status. | ServiceFeedbackService |
| Provide the detail of a request (party view and admin view). | — |
| Provide the rescheduling history and the counts. | — |

### ServiceRequestCommandService
| Responsibilities | Collaborators |
|---|---|
| Create requests, validating active service, availability, and radius. | ServiceQueryService, GeocodingPort |
| Manage state transitions (state machine in the domain). | ServiceRequest (domain) |
| Reschedule by creating a linked request (previousRequestId). | NotificationService (event), MetricsServices (event) |

### RescheduleProposalService
| Responsibilities | Collaborators |
|---|---|
| Create, cancel, and query rescheduling proposals (filterable by status). | ServiceRequestCommandService |
| Accept a proposal: change its status and trigger the rescheduling. | NotificationService |
| Reject a proposal, notifying the offerer. | — |

### ScheduledTasksService  ·  «scheduled»
| Responsibilities | Collaborators |
|---|---|
| Automatically reject pending requests and expired proposals. | ServiceRequestCommandService |
| Mark accepted-but-unconfirmed requests as no-shows after 1 day. | RescheduleProposalService |
| Finalize as completed those not confirmed by the client after 1 day. | NotificationService |

---

## Module 5 — Reviews & Ratings

### ServiceFeedbackService  ·  «orchestrator»
| Responsibilities | Collaborators |
|---|---|
| Validate status and uniqueness before creating service feedback. | ServiceRatingService |
| Create rating and/or review depending on what was submitted (one, the other, or both). | ServiceReviewService |
| Provide a request's feedback and the feedback listings. | — |
| Revert a request's feedback (deletes rating and/or review if they exist) and confirm to the requester. | — |

### ServiceRatingService
| Responsibilities | Collaborators |
|---|---|
| Create a service's rating and publish the rating event. | ServiceMetricsService (event), OffererMetricsService (event) |
| Delete the rating, publishing the deletion event. | — |

### ServiceReviewService
| Responsibilities | Collaborators |
|---|---|
| Create the service review with its tags (transactional) and publish the event. | ServiceTagMetricsService (event) |
| Delete the review and its tags, publishing the deletion event. | OffererMetricsService (event) |

### ServiceReviewTagCatalogService
| Responsibilities | Collaborators |
|---|---|
| Provide the catalog of tags available for service reviews. | — |

### ClientFeedbackService  ·  «orchestrator»
| Responsibilities | Collaborators |
|---|---|
| Validate status and uniqueness before creating client feedback. | ClientRatingService |
| Create the client's rating and/or review depending on what was submitted. | ClientReviewService |
| Provide a request's feedback and the feedback listings. | — |
| Revert a request's feedback (deletes rating and/or review if they exist) and confirm to the requester. | — |

### ClientRatingService
| Responsibilities | Collaborators |
|---|---|
| Create a client's rating and publish the event. | ClientMetricsService (event) |
| Delete the rating, publishing the deletion event. | — |

### ClientReviewService
| Responsibilities | Collaborators |
|---|---|
| Create the client review with its tags (transactional) and publish the event. | ClientTagMetricsService (event) |
| Delete the review and its tags, publishing the deletion event. | ClientMetricsService (event) |

### ClientReviewTagCatalogService
| Responsibilities | Collaborators |
|---|---|
| Provide the catalog of tags available for client reviews. | — |

---

## Module 6 — Metrics

### ServiceMetricsService
| Responsibilities | Collaborators |
|---|---|
| Provide a service's precomputed metrics. | — |
| Recompute average and rating/review counts when listening to events. | ServiceRatingService (event), ServiceReviewService (event) |

### ServiceTagMetricsService
| Responsibilities | Collaborators |
|---|---|
| Provide a service's tag counts. | — |
| Apply UPSERT/decrement of tags when listening to review events. | ServiceReviewService (event) |

### OffererMetricsService
| Responsibilities | Collaborators |
|---|---|
| Provide the offerer's full metrics and summary. | — |
| Update rating, reviews, positive/negative tags, and status counters via events. | ServiceRatingService (event), ServiceReviewService (event), ServiceRequestCommandService (event) |

### ClientMetricsService
| Responsibilities | Collaborators |
|---|---|
| Provide the client's full metrics and summary. | — |
| Update rating, reviews, positive/negative tags, and status counters via events. | ClientRatingService (event), ClientReviewService (event), ServiceRequestCommandService (event) |

### OffererTagMetricsService
| Responsibilities | Collaborators |
|---|---|
| Provide an offerer's tag counts. | — |
| Apply UPSERT/decrement of tags when listening to service review events. | ServiceReviewService (event) |

### ClientTagMetricsService
| Responsibilities | Collaborators |
|---|---|
| Provide a client's tag counts. | — |
| Apply UPSERT/decrement of tags when listening to client review events. | ClientReviewService (event) |

---

## Module 7 — Reports

### ReportService
| Responsibilities | Collaborators |
|---|---|
| Create the common base report (shared logic via composition). | — |
| List reports in a unified way (by type, category, status) and provide counts. | — |

### RequestReportService
| Responsibilities | Collaborators |
|---|---|
| Create a report about a request (reusing the base report). | ReportService |
| Provide the report detail with the request's data. | — |

### ServiceReviewReportService
| Responsibilities | Collaborators |
|---|---|
| Create a report about a service review (reusing the base report). | ReportService |
| Provide the report detail with the review's data. | — |

### ClientReviewReportService
| Responsibilities | Collaborators |
|---|---|
| Create a report about a client review (reusing the base report). | ReportService |
| Provide the report detail with the review's data. | — |

### ReportActionService
| Responsibilities | Collaborators |
|---|---|
| Record the actions taken by an admin on a report. | — |
| Provide a report's action history. | — |

### ModerationService  ·  «orchestrator»
| Responsibilities | Collaborators |
|---|---|
| Apply moderation actions (warn, ban, revert feedback, close). | UserService |
| To revert feedback: identify the type based on the report and DELEGATE to the corresponding FeedbackService. | ServiceFeedbackService, ClientFeedbackService |
| Close the report and record the action centrally (finalizeReport). | ReportActionService |
| Notify the reporter of the outcome and the reported party of what affects them (including the reversal confirmed by the boolean). | NotificationService |

---

## Module 8 — Notifications

### NotificationService  ·  «orchestrator»
| Responsibilities | Collaborators |
|---|---|
| Create the base notification, persisting only the generic message. | NotificationDeliveryService |
| Dispatch the notification through the requested channels (transactional). | — |
| Carry the protected data without persisting it. | — |

### NotificationDeliveryService
| Responsibilities | Collaborators |
|---|---|
| Create the per-channel delivery and dispatch it through the outbound port. | EmailPort, PushNotificationPort |
| Manage the send status and the read state (readAt). | NotificationChannelService |
| Provide the user's notifications (ordered, filterable). | — |

### NotificationChannelService
| Responsibilities | Collaborators |
|---|---|
| Provide the available notification channels. | — |
