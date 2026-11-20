CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    access_token_expiry TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_token_expiry TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'USED', 'REVOKED'))
);
