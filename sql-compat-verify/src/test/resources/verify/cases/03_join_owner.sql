SELECT a.id, o.owner_name FROM demo_asset a JOIN demo_owner o ON o.id = a.owner_id ORDER BY a.id;
