-- Cleanup script to remove seed data from taxrefundbatchdb
-- This script removes refunds that don't correspond to real tax files

SET search_path TO taxrefundbatchdb;

-- Delete refunds that have file IDs that don't exist in taxfileservdb
-- These are likely from seed data
DELETE FROM refunds 
WHERE file_id NOT IN (
    SELECT id::text 
    FROM taxfileservdb.tax_file
);

-- Show remaining refunds (should only be real ones from the service)
SELECT 
    file_id,
    status,
    created_at,
    updated_at
FROM refunds 
ORDER BY created_at DESC;

-- Show count of remaining refunds
SELECT COUNT(*) as remaining_refunds FROM refunds;
