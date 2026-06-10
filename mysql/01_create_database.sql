-- =========================================================
-- DATABASE: marketplace_services
-- MySQL 8+
-- =========================================================

CREATE DATABASE IF NOT EXISTS marketplace_services
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE marketplace_services;

-- =========================================================
-- USERS & AUTH
-- =========================================================

CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name ENUM('CLIENT', 'OFFERER', 'ADMIN') NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    role_id TINYINT UNSIGNED NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_user_role UNIQUE (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);

CREATE TABLE consents (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    accepted BOOLEAN NOT NULL,
    consented_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_consents_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_prt_user (user_id),
    INDEX idx_prt_expires (expires_at),

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================================================
-- ADDRESSES
-- =========================================================

CREATE TABLE addresses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,

    -- AES-256-GCM encrypted
    address_line VARBINARY(1024) NOT NULL,

    city VARCHAR(150) NOT NULL,

    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_addresses_user (user_id),
    INDEX idx_addresses_geo (latitude, longitude),

    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================================================
-- USER PROFILES
-- =========================================================

CREATE TABLE user_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT UNSIGNED NOT NULL UNIQUE,

    full_name VARCHAR(255) NOT NULL,

    document_type VARCHAR(50) NOT NULL,

    -- AES-256-GCM encrypted
    document_number VARBINARY(512) NOT NULL,
    phone_number VARBINARY(512) NOT NULL,

    primary_address_id BIGINT UNSIGNED NULL,

    profile_photo_url VARCHAR(1000) NULL,

    bio TEXT NULL,

    profile_type ENUM('NATURAL', 'COMPANY') NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_profiles_primary_address
        FOREIGN KEY (primary_address_id)
        REFERENCES addresses(id)
        ON DELETE SET NULL
);

-- =========================================================
-- OFFERER
-- =========================================================

CREATE TABLE offerer_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT UNSIGNED NOT NULL UNIQUE,

    whatsapp_number VARCHAR(30) NOT NULL,

    public_description TEXT NULL,

    specialty VARCHAR(255) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_offerer_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE offerer_availabilities (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    offerer_id BIGINT UNSIGNED NOT NULL,

    week_day TINYINT UNSIGNED NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    INDEX idx_offerer_availability_offerer (offerer_id),

    CONSTRAINT chk_offerer_week_day
        CHECK (week_day BETWEEN 0 AND 6),

    CONSTRAINT fk_offerer_availability_user
        FOREIGN KEY (offerer_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================================================
-- CATEGORIES & SERVICES
-- =========================================================

CREATE TABLE categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE services (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    offerer_id BIGINT UNSIGNED NOT NULL,

    title VARCHAR(255) NOT NULL,

    description TEXT NOT NULL,

    photos JSON NULL,

    price_hourly DECIMAL(12,2) NOT NULL,

    category_id BIGINT UNSIGNED NOT NULL,

    average_duration_minutes INT UNSIGNED NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    operation_radius_km DECIMAL(8,2) NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted_at DATETIME NULL,

    INDEX idx_services_category (category_id),
    INDEX idx_services_price (price_hourly),
    INDEX idx_services_offerer_category (offerer_id, category_id),
    INDEX idx_services_category_price (category_id, price_hourly),

    CONSTRAINT fk_services_offerer
        FOREIGN KEY (offerer_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_services_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
);

CREATE TABLE service_availabilities (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    service_id BIGINT UNSIGNED NOT NULL,

    week_day TINYINT UNSIGNED NOT NULL,

    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    INDEX idx_service_availability_service (service_id),

    CONSTRAINT chk_service_week_day
        CHECK (week_day BETWEEN 0 AND 6),

    CONSTRAINT fk_service_availability_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
        ON DELETE CASCADE
);

-- =========================================================
-- SERVICE REQUESTS
-- =========================================================

CREATE TABLE service_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    service_id BIGINT UNSIGNED NOT NULL,

    previous_request_id BIGINT UNSIGNED NULL UNIQUE,

    client_id BIGINT UNSIGNED NOT NULL,

    offerer_id BIGINT UNSIGNED NOT NULL,

    address_id BIGINT UNSIGNED NOT NULL,

    scheduled_date DATETIME NOT NULL,

    status ENUM(
        'PENDING',
        'ACCEPTED',
        'REJECTED',
        'CANCELLED',
        'COMPLETED',
        'RESCHEDULED',
        'NOT_PROVIDED'
    ) NOT NULL DEFAULT 'PENDING',

    updated_by BIGINT UNSIGNED NULL,

    requested_price DECIMAL(12,2) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    completed_at DATETIME NULL,

    updated_status_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_service_requests_status (status),
    INDEX idx_service_requests_client_status (client_id, status),
    INDEX idx_service_requests_service_status (service_id, status),
    INDEX idx_service_requests_scheduled_date (scheduled_date),
    INDEX idx_service_requests_updated_status_at (updated_status_at),

    CONSTRAINT fk_service_requests_service
        FOREIGN KEY (service_id)
        REFERENCES services(id),

    CONSTRAINT fk_service_requests_previous
        FOREIGN KEY (previous_request_id)
        REFERENCES service_requests(id),

    CONSTRAINT fk_service_requests_client
        FOREIGN KEY (client_id)
        REFERENCES users(id),

    CONSTRAINT fk_service_requests_offerer
        FOREIGN KEY (offerer_id)
        REFERENCES users(id),

    CONSTRAINT fk_service_requests_address
        FOREIGN KEY (address_id)
        REFERENCES addresses(id),

    CONSTRAINT fk_service_requests_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
);

CREATE TABLE reschedule_proposals (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    request_id BIGINT UNSIGNED NOT NULL,

    reason TEXT NOT NULL,

    proposed_date DATETIME NOT NULL,

    status ENUM(
        'PENDING',
        'ACCEPTED',
        'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    responded_at DATETIME NULL,

    INDEX idx_reschedule_request (request_id),

    CONSTRAINT fk_reschedule_request
        FOREIGN KEY (request_id)
        REFERENCES service_requests(id)
        ON DELETE CASCADE
);

-- =========================================================
-- SERVICE REVIEWS & RATINGS
-- =========================================================

CREATE TABLE service_ratings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    request_id BIGINT UNSIGNED NOT NULL UNIQUE,

    client_id BIGINT UNSIGNED NOT NULL,

    rating TINYINT UNSIGNED NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_rating
        CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT fk_service_ratings_request
        FOREIGN KEY (request_id)
        REFERENCES service_requests(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_ratings_client
        FOREIGN KEY (client_id)
        REFERENCES users(id)
);

CREATE TABLE service_reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    request_id BIGINT UNSIGNED NOT NULL UNIQUE,

    client_id BIGINT UNSIGNED NOT NULL,

    comment TEXT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_service_reviews_request
        FOREIGN KEY (request_id)
        REFERENCES service_requests(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_reviews_client
        FOREIGN KEY (client_id)
        REFERENCES users(id)
);

CREATE TABLE service_review_tags_catalog (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    tag_name VARCHAR(150) NOT NULL UNIQUE,

    sentiment ENUM('P', 'N') NOT NULL
);

CREATE TABLE service_review_tags (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    review_id BIGINT UNSIGNED NOT NULL,

    tag_id BIGINT UNSIGNED NOT NULL,

    CONSTRAINT uq_service_review_tag UNIQUE (review_id, tag_id),

    CONSTRAINT fk_service_review_tags_review
        FOREIGN KEY (review_id)
        REFERENCES service_reviews(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_review_tags_tag
        FOREIGN KEY (tag_id)
        REFERENCES service_review_tags_catalog(id)
        ON DELETE CASCADE
);

-- =========================================================
-- CLIENT REVIEWS & RATINGS
-- =========================================================

CREATE TABLE client_ratings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    request_id BIGINT UNSIGNED NOT NULL UNIQUE,

    offerer_id BIGINT UNSIGNED NOT NULL,

    client_id BIGINT UNSIGNED NOT NULL,

    rating TINYINT UNSIGNED NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_client_rating
        CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT fk_client_ratings_request
        FOREIGN KEY (request_id)
        REFERENCES service_requests(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_ratings_offerer
        FOREIGN KEY (offerer_id)
        REFERENCES users(id),

    CONSTRAINT fk_client_ratings_client
        FOREIGN KEY (client_id)
        REFERENCES users(id)
);

CREATE TABLE client_reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    request_id BIGINT UNSIGNED NOT NULL UNIQUE,

    offerer_id BIGINT UNSIGNED NOT NULL,

    comment TEXT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_client_reviews_request
        FOREIGN KEY (request_id)
        REFERENCES service_requests(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_reviews_offerer
        FOREIGN KEY (offerer_id)
        REFERENCES users(id)
);

CREATE TABLE client_review_tags_catalog (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    tag_name VARCHAR(150) NOT NULL UNIQUE,

    sentiment ENUM('P', 'N') NOT NULL
);

CREATE TABLE client_review_tags (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    review_id BIGINT UNSIGNED NOT NULL,

    tag_id BIGINT UNSIGNED NOT NULL,

    CONSTRAINT uq_client_review_tag UNIQUE (review_id, tag_id),

    CONSTRAINT fk_client_review_tags_review
        FOREIGN KEY (review_id)
        REFERENCES client_reviews(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_review_tags_tag
        FOREIGN KEY (tag_id)
        REFERENCES client_review_tags_catalog(id)
        ON DELETE CASCADE
);

-- =========================================================
-- METRICS
-- =========================================================

CREATE TABLE service_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    service_id BIGINT UNSIGNED NOT NULL UNIQUE,

    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0,

    total_ratings INT UNSIGNED NOT NULL DEFAULT 0,

    total_reviews INT UNSIGNED NOT NULL DEFAULT 0,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_service_metrics_avg_rating (average_rating),

    CONSTRAINT fk_service_metrics_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
        ON DELETE CASCADE
);

CREATE TABLE service_tag_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    tag_id BIGINT UNSIGNED NOT NULL,

    service_id BIGINT UNSIGNED NOT NULL,

    tag_count INT UNSIGNED NOT NULL DEFAULT 0,

    CONSTRAINT uq_service_tag_metric UNIQUE (tag_id, service_id),

    INDEX idx_service_tag_metrics_service_count (service_id, tag_count),

    CONSTRAINT fk_service_tag_metrics_tag
        FOREIGN KEY (tag_id)
        REFERENCES service_review_tags_catalog(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_tag_metrics_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
        ON DELETE CASCADE
);

CREATE TABLE offerer_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    offerer_id BIGINT UNSIGNED NOT NULL UNIQUE,

    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0,

    total_ratings INT UNSIGNED NOT NULL DEFAULT 0,

    total_reviews INT UNSIGNED NOT NULL DEFAULT 0,

    total_positive_tags INT UNSIGNED NOT NULL DEFAULT 0,

    total_negative_tags INT UNSIGNED NOT NULL DEFAULT 0,

    total_accepted_requests INT UNSIGNED NOT NULL DEFAULT 0,

    total_completed_services INT UNSIGNED NOT NULL DEFAULT 0,

    total_cancelled_services INT UNSIGNED NOT NULL DEFAULT 0,

    total_rescheduled_services INT UNSIGNED NOT NULL DEFAULT 0,

    total_not_provided_services INT UNSIGNED NOT NULL DEFAULT 0,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_offerer_metrics_user
        FOREIGN KEY (offerer_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE client_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    client_id BIGINT UNSIGNED NOT NULL UNIQUE,

    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0,

    total_ratings INT UNSIGNED NOT NULL DEFAULT 0,

    total_reviews INT UNSIGNED NOT NULL DEFAULT 0,

    total_positive_tags INT UNSIGNED NOT NULL DEFAULT 0,

    total_negative_tags INT UNSIGNED NOT NULL DEFAULT 0,

    total_accepted_requests INT UNSIGNED NOT NULL DEFAULT 0,

    total_completed_requests INT UNSIGNED NOT NULL DEFAULT 0,

    total_cancelled_requests INT UNSIGNED NOT NULL DEFAULT 0,

    total_scheduled_requests INT UNSIGNED NOT NULL DEFAULT 0,

    total_not_provided_requests INT UNSIGNED NOT NULL DEFAULT 0,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_client_metrics_user
        FOREIGN KEY (client_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE offerer_tag_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    offerer_id BIGINT UNSIGNED NOT NULL,

    tag_id BIGINT UNSIGNED NOT NULL,

    tag_count INT UNSIGNED NOT NULL DEFAULT 0,

    CONSTRAINT uq_offerer_tag_metric UNIQUE (offerer_id, tag_id),

    INDEX idx_offerer_tag_metrics_offerer_count (offerer_id, tag_count),

    CONSTRAINT fk_offerer_tag_metrics_user
        FOREIGN KEY (offerer_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_offerer_tag_metrics_tag
        FOREIGN KEY (tag_id)
        REFERENCES service_review_tags_catalog(id)
        ON DELETE CASCADE
);

CREATE TABLE client_tag_metrics (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    client_id BIGINT UNSIGNED NOT NULL,

    tag_id BIGINT UNSIGNED NOT NULL,

    tag_count INT UNSIGNED NOT NULL DEFAULT 0,

    CONSTRAINT uq_client_tag_metric UNIQUE (client_id, tag_id),

    INDEX idx_client_tag_metrics_client_count (client_id, tag_count),

    CONSTRAINT fk_client_tag_metrics_user
        FOREIGN KEY (client_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_tag_metrics_tag
        FOREIGN KEY (tag_id)
        REFERENCES client_review_tags_catalog(id)
        ON DELETE CASCADE
);

-- =========================================================
-- REPORTS
-- =========================================================

CREATE TABLE reports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    reporter_id BIGINT UNSIGNED NOT NULL,

    reported_user_id BIGINT UNSIGNED NOT NULL,

    report_type ENUM(
        'REQUEST',
        'SERVICE_REVIEW',
        'CLIENT_REVIEW'
    ) NOT NULL,

    category VARCHAR(150) NOT NULL,

    reason TEXT NOT NULL,

    status ENUM(
        'PENDING',
        'RESOLVED',
        'CLOSED'
    ) NOT NULL DEFAULT 'PENDING',

    priority ENUM(
        'LOW',
        'MEDIUM',
        'HIGH',
        'CRITICAL'
    ) NOT NULL DEFAULT 'MEDIUM',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_reports_reporter
        FOREIGN KEY (reporter_id)
        REFERENCES users(id),

    CONSTRAINT fk_reports_reported_user
        FOREIGN KEY (reported_user_id)
        REFERENCES users(id)
);

CREATE TABLE request_reports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    report_id BIGINT UNSIGNED NOT NULL UNIQUE,

    request_id BIGINT UNSIGNED NOT NULL,

    CONSTRAINT fk_request_reports_report
        FOREIGN KEY (report_id)
        REFERENCES reports(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_request_reports_request
        FOREIGN KEY (request_id)
        REFERENCES service_requests(id)
);

CREATE TABLE service_review_reports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    report_id BIGINT UNSIGNED NOT NULL UNIQUE,

    review_id BIGINT UNSIGNED NOT NULL,

    CONSTRAINT fk_service_review_reports_report
        FOREIGN KEY (report_id)
        REFERENCES reports(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_review_reports_review
        FOREIGN KEY (review_id)
        REFERENCES service_reviews(id)
);

CREATE TABLE client_review_reports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    report_id BIGINT UNSIGNED NOT NULL UNIQUE,

    review_id BIGINT UNSIGNED NOT NULL,

    CONSTRAINT fk_client_review_reports_report
        FOREIGN KEY (report_id)
        REFERENCES reports(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_review_reports_review
        FOREIGN KEY (review_id)
        REFERENCES client_reviews(id)
);

CREATE TABLE report_actions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    report_id BIGINT UNSIGNED NOT NULL,

    admin_id BIGINT UNSIGNED NOT NULL,

    action_description TEXT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_report_actions_report (report_id),

    CONSTRAINT fk_report_actions_report
        FOREIGN KEY (report_id)
        REFERENCES reports(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_report_actions_admin
        FOREIGN KEY (admin_id)
        REFERENCES users(id)
);

-- =========================================================
-- NOTIFICATIONS
-- =========================================================

CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT UNSIGNED NOT NULL,

    notification_type VARCHAR(100) NOT NULL,

    title VARCHAR(255) NOT NULL,

    message TEXT NOT NULL,

    entity_type VARCHAR(100) NULL,

    entity_id BIGINT UNSIGNED NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_notifications_created_at (created_at),

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE notification_channels (
    id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE notification_deliveries (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    notification_id BIGINT UNSIGNED NOT NULL,

    channel_id TINYINT UNSIGNED NOT NULL,

    delivery_status ENUM(
        'PENDING',
        'SENT',
        'FAILED',
        'READ'
    ) NOT NULL DEFAULT 'PENDING',

    read_at DATETIME NULL,

    sent_at DATETIME NULL,

    INDEX idx_notification_deliveries_notification (notification_id),

    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_deliveries_channel
        FOREIGN KEY (channel_id)
        REFERENCES notification_channels(id)
);

-- =========================================================
-- INITIAL DATA
-- =========================================================

INSERT INTO roles(name)
VALUES
('CLIENT'),
('OFFERER'),
('ADMIN');

INSERT INTO notification_channels(name)
VALUES
('INTERNAL'),
('EMAIL');