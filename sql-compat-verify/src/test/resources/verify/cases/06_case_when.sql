SELECT id, CASE WHEN status = 1 THEN 'active' ELSE 'inactive' END AS state FROM demo_asset ORDER BY id;
