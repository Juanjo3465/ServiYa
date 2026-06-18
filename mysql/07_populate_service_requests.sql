insert into service_requests (service_id, client_id, offerer_id, address_id, scheduled_date, status, requested_price) values
    (1, 1, 2, 1, '2024-07-01', 'pending', 100.00),
    (1, 2, 4, 2, '2024-07-02', 'pending', 150.00),
    (1, 3, 6, 3, '2024-07-03', 'pending', 200.00),
    (1, 4, 8, 4, '2024-07-04', 'pending', 250.00),
    (1, 5, 10, 5, '2024-07-05', 'pending', 300.00);

insert into service_reviews (request_id, service_id, client_id, comment) values
    (1, 1, 1, 'Great service!'),
    (2, 1, 2, 'Very satisfied with the work.'),
    (3, 1, 3, 'Good job, but could be faster.'),
    (4, 1, 4, 'Excellent service and friendly staff.'),
    (5, 1, 5, 'Not happy with the quality of work.');