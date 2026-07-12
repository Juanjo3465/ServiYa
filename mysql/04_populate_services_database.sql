-- =====================================================
-- SERVICIOS del oferente 1 (requiere users + categories)
-- ids de servicio: 1=plomería 2=jardinería 3=limpieza
-- =====================================================
SET NAMES utf8mb4;

INSERT INTO services (
    offerer_id,
    title,
    description,
    photos,
    price_hourly,
    category_id,
    average_duration_minutes
)
VALUES (
    1,
    'Servicio de plomería',
    'Reparación e instalación de tuberías',
    '["foto1.jpg", "foto2.jpg"]',
    25.50,
    3,
    90
),
(
    1,
    'Servicio de jardinería',
    'Mantenimiento de jardines y áreas verdes',
    '["foto3.jpg", "foto4.jpg"]',
    30.00,
    5,
    120
),
(
    1,
    'Servicio de limpieza',
    'Limpieza profunda de hogares y oficinas',
    '["foto5.jpg", "foto6.jpg"]',
    20.00,
    6,
    60
),
(
    1,
    'Servicio de reparacion de celulares',
    'Limpieza profunda de hogares y oficinas',
    '["foto5.jpg", "foto6.jpg"]',
    70.00,
    9,
    60
);