-- Test fixture with MySQL-only syntax. Invented schema.

SELECT IFNULL(o.owner_name, 'unknown') AS owner_name
FROM demo_owner o WHERE o.id = 1;

DELETE FROM demo_task WHERE created_at < '2024-01-01 00:00:00' LIMIT 500;

-- Portable: expect no findings.
SELECT a.id, COALESCE(a.owner_name, 'unknown') AS owner_name
FROM demo_asset a WHERE a.status = 1;
