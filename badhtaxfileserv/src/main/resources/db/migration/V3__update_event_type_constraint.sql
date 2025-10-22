-- Update the event_type constraint to accept enum names instead of values
SET search_path TO taxfileservdb;

-- Drop the existing constraint
ALTER TABLE refund_events DROP CONSTRAINT IF EXISTS refund_events_event_type_check;

-- Add the new constraint with enum names
ALTER TABLE refund_events ADD CONSTRAINT refund_events_event_type_check 
    CHECK (event_type IN ('REFUND_INPROGRESS', 'REFUND_APPROVED', 'REFUND_REJECTED', 'REFUND_ERROR'));
