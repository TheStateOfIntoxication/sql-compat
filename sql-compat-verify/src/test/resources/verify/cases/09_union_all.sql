SELECT id FROM demo_asset WHERE status = 1 UNION ALL SELECT id FROM demo_asset WHERE status = 0 ORDER BY id;
