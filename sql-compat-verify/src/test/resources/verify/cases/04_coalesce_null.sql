SELECT id, COALESCE(owner_id, 0) AS owner_id FROM demo_asset ORDER BY id;
