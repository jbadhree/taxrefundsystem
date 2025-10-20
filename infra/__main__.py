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

# Create Cloud Run service for file service
file_cloud_run_service = gcp.cloudrun.Service(
    "badhtaxfileserv-service",
    name=file_service_name,
    location=region,
    template=gcp.cloudrun.ServiceTemplateArgs(
        spec=gcp.cloudrun.ServiceTemplateSpecArgs(
            containers=[
                gcp.cloudrun.ServiceTemplateSpecContainerArgs(
                    image=file_image_name,
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
                            name="SPRING_DATASOURCE_URL",
                            value=pulumi.Output.concat(
                                "jdbc:postgresql://",
                                db_instance.public_ip_address,
                                ":5432/",
                                db_name,
                                "?sslmode=require"
                            )
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_DATASOURCE_USERNAME",
                            value=db_user
                        ),
                        gcp.cloudrun.ServiceTemplateSpecContainerEnvArgs(
                            name="SPRING_DATASOURCE_PASSWORD",
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
            timeout_seconds=300
        ),
        metadata=gcp.cloudrun.ServiceTemplateMetadataArgs(
            annotations={
                "autoscaling.knative.dev/maxScale": "10",
                "autoscaling.knative.dev/minScale": "0",
                "run.googleapis.com/execution-environment": "gen2",
                "run.googleapis.com/startup-cpu-boost": "true"
            }
        )
    ),
    opts=pulumi.ResourceOptions(depends_on=[cloud_run_api, db_user_instance])
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

# Export important values
export("project_id", project_id)
export("region", region)

# Database exports
export("db_instance_name", db_instance.name)
export("db_instance_connection_name", db_instance.connection_name)
export("db_instance_private_ip", db_instance.private_ip_address)
export("db_name", database.name)
export("db_user", db_user_instance.name)

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
