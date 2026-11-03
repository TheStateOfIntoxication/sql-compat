SELECT status, COUNT(*) AS cnt, SUM(amount) AS total FROM demo_asset GROUP BY status ORDER BY status;
