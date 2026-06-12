-- -----------------------------------------------------
-- POPULATION SCRIPT: service_requests (Future Dates & ID 1)
-- -----------------------------------------------------

SET FOREIGN_KEY_CHECKS = 0;

-- Clear existing data if you want a fresh start (Optional)
-- TRUNCATE TABLE service_requests;

INSERT INTO service_requests (
    service_id,
    previous_request_id,
    client_id,
    offerer_id,
    address_id,
    scheduled_date,
    status,
    updated_by,
    requested_price,
    created_at,
    completed_at,
    updated_status_at
) VALUES
-- 1. Client 1 creates a future pending request
(1, NULL, 1, 20, 100, '2026-07-15 10:00:00', 'PENDING', NULL, 150.00, '2026-06-12 09:00:00', NULL, '2026-06-12 09:00:00'),

-- 2. Client 11 requests from Offerer 1 (Accepted for the future)
(2, NULL, 11, 1, 101, '2026-07-20 14:30:00', 'ACCEPTED', 1, 85.50, '2026-06-12 08:15:00', NULL, '2026-06-13 11:00:00'),

-- 3. Client 1 requests a service from Offerer 22 (Marked as rejected)
(3, NULL, 1, 22, 103, '2026-08-01 16:00:00', 'REJECTED', 22, 350.00, '2026-06-12 17:45:00', NULL, '2026-06-13 09:30:00'),

-- 4. Client 12 requests from Offerer 1 (Later cancelled by the client)
(2, NULL, 12, 1, 100, '2026-08-15 11:00:00', 'CANCELLED', 12, 90.00, '2026-06-12 10:00:00', NULL, '2026-06-14 14:20:00'),

-- 5. Client 1 creates a request that gets rescheduled
(4, NULL, 1, 23, 104, '2026-07-05 13:00:00', 'RESCHEDULED', 1, 120.00, '2026-06-12 08:00:00', NULL, '2026-06-15 10:00:00'),

-- 6. The follow-up request to the one above, maintaining Client 1 (Assumes previous row gets ID 5)
(4, 5, 1, 23, 104, '2026-07-19 13:00:00', 'ACCEPTED', 23, 120.00, '2026-06-15 10:00:00', NULL, '2026-06-15 10:30:00'),

-- 7. Client 13 requests from Offerer 1 (Simulating a future "No Show" tracking record)
(1, NULL, 13, 1, 105, '2026-09-10 15:00:00', 'NOT_PROVIDED', 13, 175.00, '2026-06-12 12:00:00', NULL, '2026-06-16 17:00:00'),

-- 8. Client 1 requests a service from Offerer 20 (Planned completion scenario for late July)
(1, NULL, 1, 20, 102, '2026-07-28 09:00:00', 'COMPLETED', 20, 200.00, '2026-06-12 13:00:00', '2026-07-28 11:15:00', '2026-07-28 11:15:00');

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------
-- Verification Query
-- -----------------------------------------------------
SELECT id, client_id, offerer_id, scheduled_date, status 
FROM service_requests;