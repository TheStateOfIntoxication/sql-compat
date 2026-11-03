-- Setup script for the consistency suite. Applied to BOTH databases before the cases run.
-- Everything here must be portable: no AUTO_INCREMENT, no backticks, no engine clause.

DROP TABLE IF EXISTS demo_owner;
DROP TABLE IF EXISTS demo_asset;

CREATE TABLE demo_owner (
    id         BIGINT       NOT NULL,
    owner_name VARCHAR(64),
    created_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE demo_asset (
    id         BIGINT       NOT NULL,
    asset_name VARCHAR(64)  NOT NULL,
    owner_id   BIGINT,
    status     INTEGER      NOT NULL,
    amount     NUMERIC(12,2),
    created_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO demo_owner (id, owner_name, created_at) VALUES (1, 'alpha', TIMESTAMP '2024-01-05 09:00:00');
INSERT INTO demo_owner (id, owner_name, created_at) VALUES (2, NULL,    TIMESTAMP '2024-01-06 10:00:00');
INSERT INTO demo_owner (id, owner_name, created_at) VALUES (3, 'gamma', TIMESTAMP '2024-01-07 11:00:00');

INSERT INTO demo_asset (id, asset_name, owner_id, status, amount, created_at) VALUES (10, 'asset-a', 1, 1, 100.50, TIMESTAMP '2024-02-01 08:00:00');
INSERT INTO demo_asset (id, asset_name, owner_id, status, amount, created_at) VALUES (11, 'asset-b', 1, 0,  20.00, TIMESTAMP '2024-02-02 09:30:00');
INSERT INTO demo_asset (id, asset_name, owner_id, status, amount, created_at) VALUES (12, 'asset-c', 2, 1, 300.00, TIMESTAMP '2024-02-03 10:15:00');
INSERT INTO demo_asset (id, asset_name, owner_id, status, amount, created_at) VALUES (13, 'asset-d', 3, 1,  75.25, TIMESTAMP '2024-02-04 11:45:00');
INSERT INTO demo_asset (id, asset_name, owner_id, status, amount, created_at) VALUES (14, 'asset-e', NULL, 0, NULL, TIMESTAMP '2024-02-05 12:00:00');
