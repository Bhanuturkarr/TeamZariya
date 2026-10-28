CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Existing projects belong to the local demo account so data created in earlier phases stays available.
INSERT INTO app_users (id, username, password_hash)
VALUES ('00000000-0000-0000-0000-000000000001', 'demo',
        'DISABLED_UNTIL_BOOTSTRAP_UPDATES_THIS_HASH');

ALTER TABLE learning_projects ADD COLUMN owner_id UUID;
UPDATE learning_projects SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;
ALTER TABLE learning_projects ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE learning_projects ADD CONSTRAINT fk_learning_projects_owner
    FOREIGN KEY (owner_id) REFERENCES app_users(id);
CREATE INDEX idx_learning_projects_owner_created ON learning_projects (owner_id, created_at DESC);
