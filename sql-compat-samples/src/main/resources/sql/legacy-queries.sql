-- Fixture SQL file with MySQL-only syntax. Invented schema, for demo purposes only.

SELECT
    IFNULL(o.owner_name, 'unknown') AS owner_name,
    DATE_FORMAT(o.created_at, '%Y-%m-%d') AS created_text
FROM demo_owner o
WHERE o.id = 1;

SELECT GROUP_CONCAT(a.asset_name ORDER BY a.id) AS asset_names
FROM demo_asset a
WHERE a.owner_id = 1;

UPDATE demo_task SET state = 'closed' WHERE state = 'stale' LIMIT 200;

DELETE FROM demo_task WHERE created_at < '2024-01-01 00:00:00' LIMIT 500;

SELECT a.id, SUBSTRING_INDEX(a.path, '/', 1) AS first_segment
FROM demo_asset a;

-- This one is portable: no findings expected.
SELECT a.id, COALESCE(a.owner_name, 'unknown') AS owner_name
FROM demo_asset a
WHERE a.status = 1;
