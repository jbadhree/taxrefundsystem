-- Create the taxrefundbatchdb schema
CREATE SCHEMA IF NOT EXISTS taxrefundbatchdb;

-- Grant permissions to the user
GRANT ALL PRIVILEGES ON SCHEMA taxrefundbatchdb TO taxrefund_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA taxrefundbatchdb TO taxrefund_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA taxrefundbatchdb TO taxrefund_user;

-- Set the search path for the user
ALTER USER taxrefund_user SET search_path = taxrefundbatchdb, public;

-- Create the refunds table
CREATE TABLE IF NOT EXISTS taxrefundbatchdb.refunds (
    id SERIAL PRIMARY KEY,
    file_id VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_refunds_status ON taxrefundbatchdb.refunds(status);
CREATE INDEX IF NOT EXISTS idx_refunds_file_id ON taxrefundbatchdb.refunds(file_id);
CREATE INDEX IF NOT EXISTS idx_refunds_created_at ON taxrefundbatchdb.refunds(created_at);

-- Grant permissions on the table
GRANT ALL PRIVILEGES ON TABLE taxrefundbatchdb.refunds TO taxrefund_user;
GRANT ALL PRIVILEGES ON SEQUENCE taxrefundbatchdb.refunds_id_seq TO taxrefund_user;
