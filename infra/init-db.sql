-- Tax Refund System Database Initialization Script
-- This script creates all the required schemas and sets up the database

-- Create extensions that might be useful
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create all required schemas
CREATE SCHEMA IF NOT EXISTS taxfileservdb;
CREATE SCHEMA IF NOT EXISTS userservdb;
CREATE SCHEMA IF NOT EXISTS aimlservdb;
CREATE SCHEMA IF NOT EXISTS taxfilebatchdb;
CREATE SCHEMA IF NOT EXISTS taxrefundbatchdb;
CREATE SCHEMA IF NOT EXISTS irsdb;

-- Grant usage on all schemas to the main user
GRANT USAGE ON SCHEMA taxfileservdb TO taxrefund_user;
GRANT USAGE ON SCHEMA userservdb TO taxrefund_user;
GRANT USAGE ON SCHEMA aimlservdb TO taxrefund_user;
GRANT USAGE ON SCHEMA taxfilebatchdb TO taxrefund_user;
GRANT USAGE ON SCHEMA taxrefundbatchdb TO taxrefund_user;
GRANT USAGE ON SCHEMA irsdb TO taxrefund_user;

-- Grant create privileges on all schemas
GRANT CREATE ON SCHEMA taxfileservdb TO taxrefund_user;
GRANT CREATE ON SCHEMA userservdb TO taxrefund_user;
GRANT CREATE ON SCHEMA aimlservdb TO taxrefund_user;
GRANT CREATE ON SCHEMA taxfilebatchdb TO taxrefund_user;
GRANT CREATE ON SCHEMA taxrefundbatchdb TO taxrefund_user;
GRANT CREATE ON SCHEMA irsdb TO taxrefund_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA taxfileservdb GRANT ALL ON TABLES TO taxrefund_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA userservdb GRANT ALL ON TABLES TO taxrefund_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA aimlservdb GRANT ALL ON TABLES TO taxrefund_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA taxfilebatchdb GRANT ALL ON TABLES TO taxrefund_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA taxrefundbatchdb GRANT ALL ON TABLES TO taxrefund_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA irsdb GRANT ALL ON TABLES TO taxrefund_user;

-- Create a function to update the updated_at timestamp (shared across schemas)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Set search paths for each schema
-- User Service Database Schema
SET search_path TO userservdb, public;
-- Schema is ready for ORM table creation

-- Tax File Service Database Schema  
SET search_path TO taxfileservdb, public;
-- Schema is ready for ORM table creation

-- AI/ML Service Database Schema
SET search_path TO aimlservdb, public;
-- Schema is ready for ORM table creation

-- Tax File Batch Processing Database Schema
SET search_path TO taxfilebatchdb, public;
-- Schema is ready for ORM table creation

-- Tax Refund Batch Processing Database Schema
SET search_path TO taxrefundbatchdb, public;
-- Schema is ready for ORM table creation

-- IRS Database Schema
SET search_path TO irsdb, public;
-- Schema is ready for ORM table creation

-- Reset to default search path
SET search_path TO public;
