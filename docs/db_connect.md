Cloud SQL Connect 

gcloud sql connect taxrefund-db --user=taxrefund_user --database=taxrefund

Enter DB Password by getting password from `pulumi config get dbPassword` 

SET search_path TO taxfileservdb, public;
or
SET search_path TO taxrefundbatchdb, public;

Run Your Query 


gcloud run jobs execute badhtaxrefundbatch --region=us-central1

SET search_path TO taxfileservdb;

SELECT 
    tf.id as file_id, r.refund_status
FROM tax_file tf
LEFT JOIN refund r ON r.tax_file_id = tf.id
WHERE tf.user_id IN (
    'user-mh1epac7-rhd15b',
    'user-mh1e8848-6vhck3', 
    'user-mh14094n-tdd800',
    'user-mh13zxo4-nlh7zp',
    'user-mh13zln0-e1268r',
    'user-mh13zf1l-npz2g0',
    'user-mh0xbbg8-hojt7z',
    'user-mh0x8vyr-l8v4w8'
)
ORDER BY tf.user_id;

SET search_path TO taxrefundbatchdb;

SELECT 
    file_id,
    status
FROM refunds 
WHERE file_id IN (
    '199681e1-3cb7-4daa-8f0c-abd7af4204db',
    'ede8242f-be93-4803-a131-73fe3d117379',
    'd80e3ab2-2d9f-454d-a8ef-846070f3be1c',
    '344d134c-2160-42a0-9d48-d1477d89a4dd',
    'd8d3e33d-855d-41cb-8910-dd2e3dbd0ebc',
    '6fd4d886-ab4f-406a-9280-3613edbd491b',
    '0b9941b1-59bf-4f18-b342-797b02c8808f',
    '2c8ff4e0-4e2e-43aa-b43a-9cccaa9181f8'
);


SET search_path TO taxfileservdb;

SELECT 
    tf.id as file_id,
    r.refund_status,
    re.error_reasons
FROM refund_events re
JOIN refund r ON r.id = re.refund_id
JOIN tax_file tf ON tf.id = r.tax_file_id
WHERE tf.id = '344d134c-2160-42a0-9d48-d1477d89a4dd'
ORDER BY re.created_at DESC;