I need to create a new service called `badhtaxfileserv`it needs to use local database .. it can use postgres database url `postgresql://taxrefund_user:taxrefund_password@localhost:5432/taxrefund?schema=taxfileservdb` [make sure to use only the schema specified taxfileservdb .. it needs to be java spring boot + gradle based component. It needs to have a docker file running on port 4000 ... It should have 3 endpoints 
1. POST /taxFile - this need to take in userid, year, Income, Expense, TaxRate, Deducted, Refund
2. GET /taxFile - this should take in userid, year, and return the rest of the data 
3. GET /refund - takes userid, year, fileid - returns refund status, errors (if any) and if refund is `In Progress` it returns a ETA .. for refund applicable fileids
4. POST /processRefundEvent can be a dummy end point for now .. it will get refund cloud event from a queue later 
Design the data model and API Spec before implementing 
First prepare plan and write it to badhtaxfileserv/docs/implmentaion_plan.md broken down in to steps