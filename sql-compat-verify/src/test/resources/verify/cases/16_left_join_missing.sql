SELECT a.id, o.owner_name FROM demo_asset a LEFT JOIN demo_owner o ON o.id = a.owner_id WHERE o.id IS NULL ORDER BY a.id;
