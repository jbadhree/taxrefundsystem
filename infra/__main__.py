"""
Tax Refund System Infrastructure
Deploys NextJS application to Google Cloud Run
"""

import os
import pulumi
import pulumi_gcp as gcp
from pulumi import Config, export

# Get configuration
config = Config()
project_id = os.getenv("GOOGLE_CLOUD_PROJECT") or gcp.organizations.get_project().project_id
region = config.get("region") or "us-central1"

# Service configuration
web_service_name = config.require("webServiceName")
web_image_name = config.require("webImageName")
file_service_name = config.require("fileServiceName")
file_image_name = config.require("fileImageName")
predict_service_name = config.require("predictServiceName")
predict_image_name = config.require("predictImageName")
batch_job_name = config.require("batchJobName")
batch_image_name = config.require("batchImageName")

# Database configuration
db_instance_name = config.get("dbInstanceName") or "taxrefund-db"
db_name = config.get("dbName") or "taxrefund"
db_user = config.get("dbUser") or "taxrefund_user"
db_password = config.require_secret("dbPassword")

# Enable required APIs
cloud_run_api = gcp.projects.Service(
    "cloud-run-api",
    service="run.googleapis.com",
    disable_on_destroy=False
)

sql_admin_api = gcp.projects.Service(
    "sql-admin-api",
    service="sqladmin.googleapis.com",
    disable_on_destroy=False
)

sql_component_api = gcp.projects.Service(
    "sql-component-api",
    service="sql-component.googleapis.com",
    disable_on_destroy=False
)

pubsub_api = gcp.projects.Service(
    "pubsub-api",
    service="pubsub.googleapis.com",
    disable_on_destroy=False
)

cloudscheduler_api = gcp.projects.Service(
    "cloudscheduler-api",
    service="cloudscheduler.googleapis.com",
    disable_on_destroy=False
)

cloudrunjobs_api = gcp.projects.Service(
    "cloudrunjobs-api",
    service="run.googleapis.com",
    disable_on_destroy=False
)

redis_api = gcp.projects.Service(
    "redis-api",
    service="redis.googleapis.com",
    disable_on_destroy=False
)

vpc_access_api = gcp.projects.Service(
    "vpc-access-api",
    service="vpcaccess.googleapis.com",
    disable_on_destroy=False
)

compute_api = gcp.projects.Service(
    "compute-api",
    service="compute.googleapis.com",
    disable_on_destroy=False
)

servicenetworking_api = gcp.projects.Service(
    "servicenetworking-api",
    service="servicenetworking.googleapis.com",
    disable_on_destroy=False
)

# Create VPC network for Redis connectivity
vpc_network = gcp.compute.Network(
    "taxrefund-vpc",
    name="taxrefund-vpc",
    auto_create_subnetworks=False,
    opts=pulumi.ResourceOptions(depends_on=[compute_api])
)

# Create subnet for the VPC
vpc_subnet = gcp.compute.Subnetwork(
    "taxrefund-subnet",
    name="taxrefund-subnet",
    ip_cidr_range="10.0.0.0/24",
    region=region,
    network=vpc_network.id,
    opts=pulumi.ResourceOptions(depends_on=[vpc_network])
)

# Create private service connection for Redis
private_connection = gcp.servicenetworking.Connection(
    "taxrefund-private-connection",
    network=vpc_network.id,
    service="servicenetworking.googleapis.com",
    reserved_peering_ranges=[gcp.compute.GlobalAddress(
        "taxrefund-peering-range",
        name="taxrefund-peering-range",
        purpose="VPC_PEERING",
        address_type="INTERNAL",
        prefix_length=16,
        network=vpc_network.id,
        opts=pulumi.ResourceOptions(depends_on=[vpc_network])
    ).name],
    opts=pulumi.ResourceOptions(depends_on=[servicenetworking_api, vpc_network])
)

# Create Cloud SQL instance
db_instance = gcp.sql.DatabaseInstance(
    "taxrefund-db-instance",
    name=db_instance_name,
    database_version="POSTGRES_15",
    region=region,
    settings=gcp.sql.DatabaseInstanceSettingsArgs(
        tier="db-f1-micro",  # Small instance for development
        disk_type="PD_SSD",
        disk_size=20,
        disk_autoresize=True,
        disk_autoresize_limit=100,
        availability_type="ZONAL",
        backup_configuration=gcp.sql.DatabaseInstanceSettingsBackupConfigurationArgs(
            enabled=True,
            start_time="03:00",
            location=region,
            point_in_time_recovery_enabled=True
        ),
        ip_configuration=gcp.sql.DatabaseInstanceSettingsIpConfigurationArgs(
            ipv4_enabled=True,
            authorized_networks=[
                gcp.sql.DatabaseInstanceSettingsIpConfigurationAuthorizedNetworkArgs(
                    name="cloud-run",
                    value="0.0.0.0/0"  # Allow Cloud Run to connect
                )
            ]
        ),
        database_flags=[
            gcp.sql.DatabaseInstanceSettingsDatabaseFlagArgs(
                name="log_statement",
                value="all"
            ),
            gcp.sql.DatabaseInstanceSettingsDatabaseFlagArgs(
                name="log_min_duration_statement",
                value="1000"
            )
        ]
    ),
    deletion_protection=False,  # Set to True for production
    opts=pulumi.ResourceOptions(depends_on=[sql_admin_api, sql_component_api])
)

# Create database
database = gcp.sql.Database(
    "taxrefund-database",
    name=db_name,
    instance=db_instance.name,
    charset="UTF8",
    collation="en_US.UTF8",
    opts=pulumi.ResourceOptions(depends_on=[db_instance])
)

# Create database user
db_user_instance = gcp.sql.User(
    "taxrefund-db-user",
    name=db_user,
    instance=db_instance.name,
    password=db_password,
    opts=pulumi.ResourceOptions(depends_on=[database])
)

# Note: Database schemas are created by the services themselves during migration
# The init-db.sql script handles schema creation when the database is first set up

# Create Redis instance
redis_instance = gcp.redis.Instance(
    "taxrefund-redis",
    name="taxrefund-redis",
    memory_size_gb=1,
    region=region,
    tier="BASIC",
    redis_version="REDIS_7_0",
    display_name="Tax Refund System Redis Cache",
    connect_mode="PRIVATE_SERVICE_ACCESS",
    authorized_network=vpc_network.id,
    opts=pulumi.ResourceOptions(depends_on=[redis_api, private_connection])
)

# Create VPC connector for Cloud Run
vpc_connector = gcp.vpcaccess.Connector(
    "taxrefund-vpc-connector",
    name="taxrefund-vpc-connector",
    region=region,
    ip_cidr_range="10.8.0.0/28",
    network=vpc_network.name,
    min_instances=2,
    max_instances=3,
    opts=pulumi.ResourceOptions(depends_on=[vpc_access_api, vpc_network])
)

# Create Pub/Sub topics
refund_update_topic = gcp.pubsub.Topic(
    "refund-update-from-irs-topic",
    name="refund-update-from-irs",
    opts=pulumi.ResourceOptions(depends_on=[pubsub_api])
)

send_refund_topic = gcp.pubsub.Topic(
    "send-refund-to-irs-topic",
    name="send-refund-to-irs",
    opts=pulumi.ResourceOptions(depends_on=[pubsub_api])
)

# Create Cloud Run service for web component
web_cloud_run_service = gcp.cloudrun.Service(
    "badhtaxrefundweb-service",
    name=web_service_name,
    location=region,
    template=gcp.cloudrun.ServiceTemplateArgs(
        spec=gcp.cloudrun.ServiceTemplateSpecArgs(
            containers=[
                gcp.cloudrun.ServiceTemplateSpecContainerArgs(
                    image=web_image_name,
                    ports=[
                        gcp.cloudrun.ServiceTemplateSpecContainerPortArgs(
                            container_port=8080,
                            protocol="TCP"
                        )
                    ],
                    envs=[
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="NODE_ENV",
                            value="production"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="BADHTAXFILESERV_BASEURL",
                            value="https://badhtaxfileserv-lumy2fdqia-uc.a.run.app"
                        )
                    ],
                    resources=gcp.cloudrun.ServiceTemplateSpecContainerResourcesArgs(
                        limits={
                            "cpu": "1000m",
                            "memory": "512Mi"
                        },
                        requests={
                            "cpu": "500m",
                            "memory": "256Mi"
                        }
                    )
                )
            ],
            container_concurrency=80,
            timeout_seconds=300
        ),
        metadata=gcp.cloudrun.ServiceTemplateMetadataArgs(
            annotations={
                "autoscaling.knative.dev/maxScale": "10",
                "autoscaling.knative.dev/minScale": "0",
                "run.googleapis.com/execution-environment": "gen2"
            }
        )
    ),
    opts=pulumi.ResourceOptions(depends_on=[cloud_run_api])
)

# Allow unauthenticated access to the web service
web_noauth_iam_member = gcp.cloudrun.IamMember(
    "web-noauth-iam-member",
    location=web_cloud_run_service.location,
    project=web_cloud_run_service.project,
    service=web_cloud_run_service.name,
    role="roles/run.invoker",
    member="allUsers"
)

# Create service account for file service
file_service_account = gcp.serviceaccount.Account(
    "badhtaxfileserv-service-account",
    account_id="badhtaxfileserv",
    display_name="BadhTaxFileServ Service Account",
    description="Service account for tax file service with Pub/Sub access"
)

# Grant Pub/Sub permissions to file service account
file_pubsub_publisher_role = gcp.projects.IAMMember(
    "file-pubsub-publisher-role",
    project=project_id,
    role="roles/pubsub.publisher",
    member=pulumi.Output.concat("serviceAccount:", file_service_account.email)
)

file_pubsub_subscriber_role = gcp.projects.IAMMember(
    "file-pubsub-subscriber-role",
    project=project_id,
    role="roles/pubsub.subscriber",
    member=pulumi.Output.concat("serviceAccount:", file_service_account.email)
)

# Create Cloud Run service for file service
file_cloud_run_service = gcp.cloudrun.Service(
    "badhtaxfileserv-service",
    name=file_service_name,
    location=region,
    template=gcp.cloudrun.ServiceTemplateArgs(
        spec=gcp.cloudrun.ServiceTemplateSpecArgs(
            service_account_name=file_service_account.email,
            containers=[
                gcp.cloudrun.ServiceTemplateSpecContainerArgs(
                    image="jbadhree/badhtaxfileserv:v1.0.31",
                    ports=[
                        gcp.cloudrun.ServiceTemplateSpecContainerPortArgs(
                            container_port=4000,
                            protocol="TCP"
                        )
                    ],
                    envs=[
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_PROFILES_ACTIVE",
                            value="production"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="DATABASE_URL",
                            value=pulumi.Output.concat(
                                "jdbc:postgresql://",
                                db_instance.public_ip_address,
                                ":5432/",
                                db_name,
                                "?sslmode=require&currentSchema=taxfileservdb"
                            )
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="POSTGRES_USER",
                            value=db_user
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="POSTGRES_PASSWORD",
                            value=db_password
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_JPA_DATABASE_PLATFORM",
                            value="org.hibernate.dialect.PostgreSQLDialect"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_JPA_HIBERNATE_DDL_AUTO",
                            value="update"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_JPA_SHOW_SQL",
                            value="false"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SERVER_PORT",
                            value="4000"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT",
                            value="30000"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE",
                            value="5"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="GOOGLE_CLOUD_PROJECT",
                            value=project_id
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="PUBSUB_REFUND_UPDATE_TOPIC",
                            value=refund_update_topic.name
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="PUBSUB_SEND_REFUND_TOPIC",
                            value=send_refund_topic.name
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="PUBSUB_ENABLED",
                            value="true"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_CLOUD_GCP_PUBSUB_ENABLED",
                            value="true"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="FLYWAY_SCHEMAS",
                            value="taxfileservdb"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="REDIS_ENABLED",
                            value="true"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="REDIS_HOST",
                            value=redis_instance.host
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="REDIS_PORT",
                            value="6379"
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="REDIS_PASSWORD",
                            value=redis_instance.auth_string
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SERVER_URL",
                            value=pulumi.Output.concat("https://", file_service_name, "-797008539263.us-central1.run.app")
                        )
                    ],
                    resources=gcp.cloudrun.ServiceTemplateSpecContainerResourcesArgs(
                        limits={
                            "cpu": "1000m",
                            "memory": "1Gi"
                        },
                        requests={
                            "cpu": "500m",
                            "memory": "512Mi"
                        }
                    )
                )
            ],
            container_concurrency=80,
            timeout_seconds=600
        ),
        metadata=gcp.cloudrun.ServiceTemplateMetadataArgs(
            annotations={
                "autoscaling.knative.dev/maxScale": "10",
                "autoscaling.knative.dev/minScale": "0",
                "run.googleapis.com/execution-environment": "gen2",
                "run.googleapis.com/startup-cpu-boost": "true",
                "run.googleapis.com/vpc-access-connector": vpc_connector.name,
                "run.googleapis.com/vpc-access-egress": "private-ranges-only"
            }
        )
    ),
    opts=pulumi.ResourceOptions(depends_on=[cloud_run_api, db_user_instance, file_pubsub_publisher_role, file_pubsub_subscriber_role, vpc_connector])
)

# Allow unauthenticated access to the file service
file_noauth_iam_member = gcp.cloudrun.IamMember(
    "file-noauth-iam-member",
    location=file_cloud_run_service.location,
    project=file_cloud_run_service.project,
    service=file_cloud_run_service.name,
    role="roles/run.invoker",
    member="allUsers"
)

# Create Cloud Run service for prediction service
predict_cloud_run_service = gcp.cloudrun.Service(
    "badhrefundpredictserv-service",
    name=predict_service_name,
    location=region,
    template=gcp.cloudrun.ServiceTemplateArgs(
        spec=gcp.cloudrun.ServiceTemplateSpecArgs(
            containers=[
                gcp.cloudrun.ServiceTemplateSpecContainerArgs(
                    image=predict_image_name,
                    ports=[
                        gcp.cloudrun.ServiceTemplateSpecContainerPortArgs(
                            container_port=8090,
                            protocol="TCP"
                        )
                    ],
                    envs=[
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="PYTHONUNBUFFERED",
                            value="1"
                        )
                    ],
                    resources=gcp.cloudrun.ServiceTemplateSpecContainerResourcesArgs(
                        limits={
                            "cpu": "1000m",
                            "memory": "1Gi"  # ML models need more memory
                        },
                        requests={
                            "cpu": "500m",
                            "memory": "512Mi"
                        }
                    )
                )
            ],
            container_concurrency=10,  # Lower concurrency for ML workloads
            timeout_seconds=300
        ),
        metadata=gcp.cloudrun.ServiceTemplateMetadataArgs(
            annotations={
                "autoscaling.knative.dev/maxScale": "5",  # Lower max scale for ML
                "autoscaling.knative.dev/minScale": "0",
                "run.googleapis.com/execution-environment": "gen2"
            }
        )
    ),
    opts=pulumi.ResourceOptions(depends_on=[cloud_run_api])
)

# Allow unauthenticated access to the prediction service
predict_noauth_iam_member = gcp.cloudrun.IamMember(
    "predict-noauth-iam-member",
    location=predict_cloud_run_service.location,
    project=predict_cloud_run_service.project,
    service=predict_cloud_run_service.name,
    role="roles/run.invoker",
    member="allUsers"
)

# Create push subscription for refund-update-from-irs topic to call service endpoint
refund_update_subscription = gcp.pubsub.Subscription(
    "refund-update-subscription",
    name="refund-update-subscription",
    topic=refund_update_topic.name,
    push_config=gcp.pubsub.SubscriptionPushConfigArgs(
        push_endpoint=pulumi.Output.concat(file_cloud_run_service.statuses[0].url, "/processRefundEvent"),
        oidc_token=gcp.pubsub.SubscriptionPushConfigOidcTokenArgs(
            service_account_email=file_service_account.email,
        ),
    ),
    ack_deadline_seconds=600,
    opts=pulumi.ResourceOptions(depends_on=[refund_update_topic, file_cloud_run_service])
)

# Create service account for batch job
batch_service_account = gcp.serviceaccount.Account(
    "badhtaxrefundbatch-service-account",
    account_id="badhtaxrefundbatch",
    display_name="BadhTaxRefundBatch Service Account",
    description="Service account for refund batch processing"
)

# Create service account for Cloud Scheduler
scheduler_service_account = gcp.serviceaccount.Account(
    "cloudscheduler-service-account",
    account_id="cloudscheduler-sa",
    display_name="Cloud Scheduler Service Account",
    description="Service account for Cloud Scheduler to trigger batch jobs"
)

# Create Cloud Run batch job
batch_job = gcp.cloudrunv2.Job(
    "badhtaxrefundbatch-job",
    name=batch_job_name,
    location=region,
    deletion_protection=False,
    template=gcp.cloudrunv2.JobTemplateArgs(
        template=gcp.cloudrunv2.JobTemplateTemplateArgs(
            containers=[
                       gcp.cloudrunv2.JobTemplateTemplateContainerArgs(
                           image=batch_image_name,
                    envs=[
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="DATABASE_URL",
                            value=pulumi.Output.concat(
                                "postgresql://",
                                db_user,
                                ":",
                                db_password,
                                "@",
                                db_instance.public_ip_address,
                                ":5432/",
                                db_name,
                                "?search_path=taxrefundbatchdb"
                            )
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="MAX_CONCURRENT_WORKERS",
                            value="10"
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="BATCH_SIZE",
                            value="100"
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="PROCESSING_INTERVAL",
                            value="0"  # Run once for batch job
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="LOG_LEVEL",
                            value="info"
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="GOOGLE_CLOUD_PROJECT",
                            value=project_id
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="PUBSUB_SEND_REFUND_TOPIC",
                            value=send_refund_topic.name
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="PUBSUB_SUBSCRIPTION_NAME",
                            value="refund-batch-subscription"
                        ),
                        gcp.cloudrunv2.JobTemplateTemplateContainerEnvArgs(
                            name="ENABLE_PUBSUB",
                            value="true"
                        )
                    ],
                    resources=gcp.cloudrunv2.JobTemplateTemplateContainerResourcesArgs(
                        limits={
                            "cpu": "1000m",
                            "memory": "1Gi"
                        }
                    )
                )
            ],
            timeout="3600s",  # 1 hour timeout
            service_account=batch_service_account.email
        )
    ),
    opts=pulumi.ResourceOptions(depends_on=[cloud_run_api, db_user_instance])
)

# Create Cloud Scheduler job to run batch job every 30 minutes
scheduler_job = gcp.cloudscheduler.Job(
    "badhtaxrefundbatch-scheduler",
    name="badhtaxrefundbatch-scheduler",
    description="Run refund batch processing every 30 minutes",
    schedule="*/30 * * * *",  # Every 30 minutes
    time_zone="America/New_York",
    region=region,
    http_target=gcp.cloudscheduler.JobHttpTargetArgs(
        uri=pulumi.Output.concat(
            "https://",
            region,
            "-run.googleapis.com/v2/projects/",
            project_id,
            "/locations/",
            region,
            "/jobs/",
            batch_job.name,
            ":run"
        ),
        http_method="POST",
        headers={
            "Content-Type": "application/json"
        },
        oauth_token=gcp.cloudscheduler.JobHttpTargetOauthTokenArgs(
            service_account_email=scheduler_service_account.email,
            scope="https://www.googleapis.com/auth/cloud-platform"
        )
    ),
    opts=pulumi.ResourceOptions(depends_on=[cloudscheduler_api, cloudrunjobs_api, batch_job, scheduler_service_account])
)

# Grant necessary permissions to service account
batch_sql_client_role = gcp.projects.IAMMember(
    "batch-sql-client-role",
    project=project_id,
    role="roles/cloudsql.client",
    member=pulumi.Output.concat("serviceAccount:", batch_service_account.email)
)

batch_scheduler_invoker_role = gcp.projects.IAMMember(
    "batch-scheduler-invoker-role",
    project=project_id,
    role="roles/run.invoker",
    member=pulumi.Output.concat("serviceAccount:", batch_service_account.email)
)

# Grant Cloud Run Jobs Runner role to batch service account
batch_jobs_runner_role = gcp.projects.IAMMember(
    "batch-jobs-runner-role",
    project=project_id,
    role="roles/run.invoker",
    member=pulumi.Output.concat("serviceAccount:", batch_service_account.email)
)


# Grant Pub/Sub permissions to batch service account
batch_pubsub_subscriber_role = gcp.projects.IAMMember(
    "batch-pubsub-subscriber-role",
    project=project_id,
    role="roles/pubsub.subscriber",
    member=pulumi.Output.concat("serviceAccount:", batch_service_account.email)
)

batch_pubsub_viewer_role = gcp.projects.IAMMember(
    "batch-pubsub-viewer-role",
    project=project_id,
    role="roles/pubsub.viewer",
    member=pulumi.Output.concat("serviceAccount:", batch_service_account.email)
)

# Grant Cloud Scheduler service account permission to impersonate batch service account
scheduler_impersonation_role = gcp.serviceaccount.IAMMember(
    "scheduler-impersonation-role",
    service_account_id=batch_service_account.name,
    role="roles/iam.serviceAccountTokenCreator",
    member=pulumi.Output.concat("serviceAccount:", scheduler_service_account.email)
)

# Grant Cloud Scheduler service account permission to run Cloud Run jobs
scheduler_jobs_runner_role = gcp.projects.IAMMember(
    "scheduler-jobs-runner-role",
    project=project_id,
    role="roles/run.invoker",
    member=pulumi.Output.concat("serviceAccount:", scheduler_service_account.email)
)

# Grant Cloud Scheduler service account permission to execute the specific Cloud Run job
scheduler_job_invoker_role = gcp.cloudrunv2.JobIamMember(
    "scheduler-job-invoker-role",
    location=batch_job.location,
    project=batch_job.project,
    name=batch_job.name,
    role="roles/run.invoker",
    member=pulumi.Output.concat("serviceAccount:", scheduler_service_account.email)
)

batch_pubsub_admin_role = gcp.projects.IAMMember(
    "batch-pubsub-admin-role",
    project=project_id,
    role="roles/pubsub.admin",
    member=pulumi.Output.concat("serviceAccount:", batch_service_account.email)
)

# Export important values
export("project_id", project_id)
export("region", region)

# Database exports
export("db_instance_name", db_instance.name)
export("db_instance_connection_name", db_instance.connection_name)
export("db_instance_private_ip", db_instance.private_ip_address)
export("db_name", database.name)
export("db_user", db_user_instance.name)

# Redis exports
export("redis_instance_name", redis_instance.name)
export("redis_instance_host", redis_instance.host)
export("redis_instance_port", redis_instance.port)

# VPC exports
export("vpc_network_name", vpc_network.name)
export("vpc_connector_name", vpc_connector.name)

# Web service exports
export("web_service_name", web_service_name)
export("web_service_url", web_cloud_run_service.statuses[0].url)
export("web_image_name", web_image_name)
export("web_docker_hub_url", "https://hub.docker.com/r/jbadhree/badhtaxrefundweb")

# File service exports
export("file_service_name", file_service_name)
export("file_service_url", file_cloud_run_service.statuses[0].url)
export("file_image_name", file_image_name)
export("file_docker_hub_url", "https://hub.docker.com/r/jbadhree/badhtaxfileserv")
export("file_service_account_email", file_service_account.email)

# Prediction service exports
export("predict_service_name", predict_service_name)
export("predict_service_url", predict_cloud_run_service.statuses[0].url)
export("predict_image_name", predict_image_name)
export("predict_docker_hub_url", "https://hub.docker.com/r/jbadhree/badhrefundpredictserv")

# Pub/Sub exports
export("refund_update_topic_name", refund_update_topic.name)
export("send_refund_topic_name", send_refund_topic.name)

# Batch job exports
export("batch_job_name", batch_job.name)
export("batch_job_image", batch_image_name)
export("scheduler_job_name", scheduler_job.name)
export("batch_service_account_email", batch_service_account.email)
