-- Initialize the taxrefundbatchdb schema
CREATE SCHEMA IF NOT EXISTS taxrefundbatchdb;

-- Grant permissions to the user
GRANT ALL PRIVILEGES ON SCHEMA taxrefundbatchdb TO taxrefund_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA taxrefundbatchdb TO taxrefund_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA taxrefundbatchdb TO taxrefund_user;

-- Set the search path for the user
ALTER USER taxrefund_user SET search_path = taxrefundbatchdb, public;



