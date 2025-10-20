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

# Web service configuration
web_service_name = config.require("webServiceName")
web_image_name = config.require("webImageName")

# Enable required APIs
cloud_run_api = gcp.projects.Service(
    "cloud-run-api",
    service="run.googleapis.com",
    disable_on_destroy=False
)

# Image name is now required from config

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

# Export important values
export("project_id", project_id)
export("region", region)

# Web service exports
export("web_service_name", web_service_name)
export("web_service_url", web_cloud_run_service.statuses[0].url)
export("web_image_name", web_image_name)
export("web_docker_hub_url", "https://hub.docker.com/r/jbadhree/badhtaxrefundweb")
