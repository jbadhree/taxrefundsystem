-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS taxfileservdb;

-- Set search path to the schema
SET search_path TO taxfileservdb;

-- Create tax_file table
CREATE TABLE tax_file (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100) NOT NULL,
    tax_year INT NOT NULL,
    income NUMERIC(14,2) NOT NULL CHECK (income >= 0),
    expense NUMERIC(14,2) NOT NULL CHECK (expense >= 0),
    tax_rate_percent NUMERIC(5,2) NOT NULL CHECK (tax_rate_percent >= 0 AND tax_rate_percent <= 100),
    deducted NUMERIC(14,2) NOT NULL CHECK (deducted >= 0),
    refund_amount NUMERIC(14,2) NOT NULL CHECK (refund_amount >= 0),
    tax_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (tax_status IN ('PENDING', 'COMPLETED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tax_file_user_year UNIQUE (user_id, tax_year)
);

-- Create refund table
CREATE TABLE refund (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tax_file_id UUID NOT NULL REFERENCES tax_file(id) ON DELETE CASCADE,
    refund_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (refund_status IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'ERROR')),
    refund_errors JSONB NULL,
    refund_eta TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_refund_tax_file UNIQUE (tax_file_id)
);

-- Create refund_events table (append-only for ML training)
CREATE TABLE refund_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    refund_id UUID NOT NULL REFERENCES refund(id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL CHECK (event_type IN ('refund.inprogress', 'refund.approved', 'refund.rejected', 'refund.error')),
    event_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    error_reasons JSONB NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX idx_tax_file_user_year ON tax_file(user_id, tax_year);
CREATE INDEX idx_refund_tax_file_id ON refund(tax_file_id);
CREATE INDEX idx_refund_events_refund_id ON refund_events(refund_id);
CREATE INDEX idx_refund_events_event_type ON refund_events(event_type);
CREATE INDEX idx_refund_events_event_date ON refund_events(event_date);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_tax_file_updated_at 
    BEFORE UPDATE ON tax_file 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refund_updated_at 
    BEFORE UPDATE ON refund 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

