-- =====================================================
-- FEEDBACK DE SERVICIO (requiere service_requests)
-- =====================================================
-- Tras la unificación de feedback, `service_reviews` pasó a ser `service_feedback`
-- (rating + comentario en una sola fila; al menos uno presente). Un feedback por
-- solicitud (request_id es UNIQUE) y SOLO sobre solicitudes COMPLETED (7 y 8 en 08).
-- service_id y client_id coinciden con los de la solicitud referenciada (service_id
-- está denormalizado en service_feedback).

insert into service_feedback (request_id, service_id, client_id, rating, comment) values
    (7, 3, 2,  5, 'Excelente servicio, muy recomendado.'),
    (8, 1, 11, 4, 'Buen trabajo, quedé satisfecho.');
