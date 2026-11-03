-- Fixture SQL file with MySQL-only syntax. Invented schema, for demo purposes only.

SELECT
    IFNULL(o.owner_name, 'unknown') AS owner_name,
    DATE_FORMAT(o.created_at, '%Y-%m-%d') AS created_text
FROM demo_owner o
WHERE o.id = 1;

SELECT GROUP_CONCAT(a.asset_name ORDER BY a.id SEPARATOR '|') AS asset_names
FROM demo_asset a
WHERE a.owner_id = 1;

SELECT a.id, SUBSTRING_INDEX(a.path, '/', 1) AS first_segment
FROM demo_asset a;

SELECT a.id
FROM demo_asset a
WHERE a.code REGEXP '^AB[0-9]+$';

SELECT a.id, JSON_EXTRACT(a.attrs, '$.owner') AS owner
FROM demo_asset a;

SELECT a.id
FROM demo_asset a
WHERE a.owner_id <=> 1;

SELECT DATE_ADD(o.expired_at, INTERVAL 30 DAY) AS expired_at
FROM demo_owner o;

SELECT STR_TO_DATE(o.day, '%Y-%m-%d') AS day
FROM demo_owner o;

UPDATE demo_task SET state = 'closed' WHERE state = 'stale' LIMIT 200;

DELETE FROM demo_task WHERE created_at < '2024-01-01 00:00:00' LIMIT 500;

REPLACE INTO demo_asset (id, asset_name) VALUES (1, 'replaced');

SELECT a.id, a.asset_name
FROM demo_asset a
ORDER BY a.created_at DESC
LIMIT 20;

-- This one is portable: no findings expected.
SELECT a.id, COALESCE(a.owner_name, 'unknown') AS owner_name
FROM demo_asset a
WHERE a.status = 1
ORDER BY a.id
LIMIT 20;
