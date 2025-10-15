

	roles/serviceusage.serviceUsageAdmin – enable/disable required APIs.
	•	roles/iam.serviceAccountAdmin – create/delete service accounts.
	•	roles/iam.serviceAccountUser – attach runtime SAs to Cloud Run services/jobs.
    roles/run.admin – create/update/delete Cloud Run Services and Cloud Run Jobs, set invokers.
    roles/artifactregistry.admin – create/delete repos, manage permissions.
    roles/pubsub.admin – create/delete topics & subscriptions, manage IAM.
	•	roles/cloudscheduler.admin – create/delete Scheduler jobs (HTTP or Pub/Sub targets).
    roles/secretmanager.admin – create/delete secrets & versions, manage IAM.
    roles/logging.configWriter – create sinks, buckets, exclusions.
	•	roles/monitoring.admin – dashboards, alerting policies, uptime checks.