SELECT id FROM demo_asset WHERE owner_id IN (SELECT id FROM demo_owner WHERE owner_name = 'alpha') ORDER BY id;
