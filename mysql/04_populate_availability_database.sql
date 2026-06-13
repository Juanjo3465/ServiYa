-- =====================================================
-- OFFERER PROFILE FOR USER 1
-- =====================================================

INSERT INTO offerer_profiles (
    user_id,
    whatsapp_number,
    public_description,
    specialty
)
VALUES (
    1,
    '+573001112233',
    'Experienced service provider available throughout the week.',
    'General Services'
);

-- =====================================================
-- AVAILABILITY SLOTS
-- week_day:
-- 0 = Sunday
-- 1 = Monday
-- 2 = Tuesday
-- 3 = Wednesday
-- 4 = Thursday
-- 5 = Friday
-- 6 = Saturday
-- =====================================================

INSERT INTO offerer_availabilities (
    offerer_id,
    week_day,
    start_time,
    end_time,
    is_active
)
VALUES
    (1, 1, '08:00:00', '12:00:00', TRUE),
    (1, 1, '14:00:00', '18:00:00', TRUE),

    (1, 2, '08:00:00', '12:00:00', TRUE),
    (1, 2, '14:00:00', '18:00:00', TRUE),

    (1, 3, '08:00:00', '12:00:00', TRUE),
    (1, 3, '14:00:00', '18:00:00', TRUE),

    (1, 4, '08:00:00', '12:00:00', TRUE),
    (1, 4, '14:00:00', '18:00:00', TRUE),

    (1, 5, '08:00:00', '12:00:00', TRUE),
    (1, 5, '14:00:00', '18:00:00', TRUE),

    (1, 6, '09:00:00', '13:00:00', TRUE),

    (1, 0, '10:00:00', '14:00:00', FALSE);