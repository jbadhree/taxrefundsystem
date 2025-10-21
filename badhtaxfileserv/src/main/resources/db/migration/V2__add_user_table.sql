-- Add user table to taxfileservdb schema
SET search_path TO taxfileservdb;

-- Create user table (using quotes because 'user' is a reserved keyword)
CREATE TABLE "user" (
    user_id VARCHAR(100) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create index for better performance
CREATE INDEX idx_user_created_at ON "user"(created_at);

-- Create trigger for updated_at
CREATE TRIGGER update_user_updated_at 
    BEFORE UPDATE ON "user" 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
