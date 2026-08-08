# AWS Jenkins CI/CD Pipeline — Deployment Guide

## 1. Purpose

This document describes the deployment workflow for the Jenkins CI/CD pipeline used to build and deploy a containerized application to AWS.

The deployment process evolves through three stages:

```text
Part I
Jenkins + SSH + EC2 + Docker

        ↓

Part II
Jenkins + Docker Compose + ECR + EC2

        ↓

Part III
Jenkins + Dynamic Versioning + ECR
        + Docker Compose + EC2
```

The project material identifies Jenkins SSH Agent, EC2 SSH credentials, Docker, Docker Compose, ECR, multi-branch pipelines, and dynamic image versioning as the major implementation components across these stages.

---

# 2. Deployment Prerequisites

Before starting, confirm that the required infrastructure and tools are available.

## Required Components

* AWS account
* EC2 instance
* Jenkins server
* Source code repository
* Docker
* Docker Compose for the later stages
* Amazon ECR for the later stages
* SSH access to EC2
* Jenkins credentials
* Appropriate AWS permissions
* EC2 Security Group configuration

---

# 3. Deployment Model

The complete deployment path is:

```text
Developer
    │
    ▼
Source Repository
    │
    ▼
Jenkins
    │
    ├── Build
    ├── Test
    ├── Docker Build
    ├── Version
    └── Publish
          │
          ▼
        Amazon ECR
          │
          ▼
        Amazon EC2
          │
          ▼
    Docker Compose
          │
          ▼
      Application
```

The exact steps depend on which project stage is being executed.

---

# 4. Part I — Jenkins Pipeline to EC2

## 4.1 Objective

The first stage introduces Jenkins as the automation layer for deployment to EC2.

The project material identifies the following activities:

1. Install the Jenkins SSH Agent plugin.
2. Create SSH credentials for the EC2 instance.
3. Configure the Jenkinsfile to use the SSH agent.
4. Execute Docker commands remotely on EC2.
5. Authenticate with the Docker image repository.
6. Configure the EC2 Security Group.
7. Execute a multi-branch pipeline.
8. Deploy the web application to EC2.

---

# 5. Configure Jenkins SSH Agent

Jenkins requires a mechanism for securely using the SSH credentials needed to access EC2.

The SSH Agent plugin is used for this purpose.

The general workflow is:

```text
Jenkins
   │
   │ SSH Credentials
   ▼
EC2 Instance
```

Install the required Jenkins SSH Agent plugin through the Jenkins plugin management interface.

After installation, verify that Jenkins recognizes the plugin.

---

# 6. Create EC2 SSH Credentials

Create the SSH credential required for the EC2 deployment host.

The credential should contain the appropriate private key associated with the EC2 instance.

### Security Requirements

Do not:

* Put the private key directly in the Jenkinsfile.
* Commit the private key to Git.
* Place the private key inside the application repository.
* Print the private key in Jenkins console output.

Store the credential using Jenkins' credential management system.

---

# 7. Configure the Jenkinsfile

The Jenkinsfile should reference the configured Jenkins credential rather than containing the credential itself.

Conceptually:

```text
Jenkinsfile
     │
     ▼
SSH Credential ID
     │
     ▼
Jenkins Credential Store
     │
     ▼
EC2 SSH Connection
```

The actual credential identifier should match the one configured in Jenkins.

Do not copy credentials directly into the Jenkinsfile.

---

# 8. Verify EC2 SSH Access

Before troubleshooting the pipeline itself, confirm that the EC2 host is accessible.

Verify:

* EC2 instance is running.
* Correct public/private network path is available.
* Correct SSH username is being used.
* Correct SSH credential is configured.
* Security Group permits SSH traffic from the appropriate source.

The objective is to establish:

```text
Jenkins
   │
   │ SSH
   ▼
EC2
```

before adding additional deployment complexity.

---

# 9. Configure Docker on EC2

The EC2 deployment host must have Docker installed.

Verify:

```bash
docker --version
```

Then verify that Docker can be used on the host.

For example:

```bash
docker ps
```

If Jenkins connects successfully but Docker commands fail, investigate the Docker installation and permissions independently of Jenkins.

---

# 10. Configure Docker Image Access

The first stage also requires the EC2 environment to obtain the application image from the configured Docker repository.

The workflow is:

```text
Docker Image
     │
     ▼
Private Repository
     │
     ▼
EC2
     │
     ▼
Docker Container
```

The EC2 host must be able to authenticate to the repository before attempting to pull a private image.

Keep registry credentials outside the repository.

---

# 11. Configure the Security Group

The EC2 Security Group must allow the network traffic required for:

* Jenkins/administrative access where applicable
* SSH access
* Application access

The application port must be accessible according to the intended deployment design.

Avoid opening unnecessary ports.

The project material explicitly includes Security Group configuration as part of the Jenkins-to-EC2 deployment workflow.

---

# 12. Configure the Multi-Branch Pipeline

Create the Jenkins multi-branch pipeline pointing to the source repository.

The general flow is:

```text
Source Repository
       │
       ▼
Multi-Branch Pipeline
       │
       ▼
Jenkins
       │
       ▼
Jenkinsfile
```

Jenkins discovers branches containing the pipeline definition according to the configured repository and branch discovery settings.

The project material identifies multi-branch pipeline execution as part of Part I.

---

# 13. Execute Part I Pipeline

Trigger the pipeline after the configuration is complete.

Monitor the Jenkins console output.

A successful flow should resemble:

```text
Checkout
   ↓
Build
   ↓
Docker Operations
   ↓
SSH to EC2
   ↓
Deploy Container
   ↓
Verification
```

Do not consider the pipeline successful solely because Jenkins reports a successful build.

Verify the application on the EC2 host as well.

---

# 14. Verify Part I Deployment

On EC2:

```bash
docker ps
```

Confirm that the expected application container is running.

Then verify the application from the intended endpoint.

The complete verification path is:

```text
Jenkins
   ↓
EC2
   ↓
Docker
   ↓
Container
   ↓
Application
   ↓
Browser
```

---

# 15. Part II — Docker Compose + ECR

Part II expands the deployment model.

The project material identifies:

* Docker Compose installation on EC2.
* Creation of `docker-compose.yaml`.
* Jenkins execution of Docker Compose commands.
* Pipeline execution.
* EC2 deployment.
* Extraction of deployment logic into a shell script as an improvement.

---

# 16. Install Docker Compose on EC2

Install Docker Compose using the appropriate method for the operating system and Docker installation used by the EC2 host.

Verify the installation.

For Docker Compose installations using the modern Docker CLI integration, the command is commonly:

```bash
docker compose version
```

The exact command should match the Compose implementation installed on the EC2 host.

---

# 17. Create `docker-compose.yaml`

The Compose configuration defines how the application should be deployed.

A conceptual configuration contains:

```text
docker-compose.yaml
       │
       ├── Image
       ├── Container
       ├── Port
       ├── Environment
       └── Runtime Configuration
```

The actual values must correspond to the application being deployed.

Do not copy production credentials into the Compose file.

---

# 18. Introduce Amazon ECR

Create or use the appropriate ECR repository for the application image.

The image lifecycle becomes:

```text
Jenkins
   │
   ▼
Docker Build
   │
   ▼
Docker Image
   │
   ▼
Amazon ECR
```

ECR becomes the central image repository used by the deployment workflow.

---

# 19. Configure Jenkins for ECR

Jenkins needs the appropriate AWS permissions to authenticate with ECR and push the application image.

The authentication mechanism should be configured through the Jenkins credential and AWS integration approach used by the implementation.

Do not hard-code AWS credentials in:

* Jenkinsfile
* Shell scripts
* Docker Compose files
* Application source code

---

# 20. Build the Docker Image

The pipeline builds the application image.

Conceptually:

```text
Source Code
     │
     ▼
Docker Build
     │
     ▼
Application Image
```

The image should be tagged with the repository and tag expected by ECR.

Example structure:

```text
<ecr-registry>/<repository>:<tag>
```

Use the actual ECR registry and repository values configured for the project.

---

# 21. Push the Image to ECR

After the image is built and tagged, Jenkins publishes it to ECR.

Conceptually:

```text
Docker Image
     │
     │ Push
     ▼
Amazon ECR
```

Verify that the image exists in ECR after the pipeline completes the image publication stage.

---

# 22. Deploy from ECR to EC2

The EC2 host retrieves the required image from ECR.

The flow becomes:

```text
Amazon ECR
     │
     │ Pull
     ▼
EC2
     │
     ▼
Docker
```

The EC2 environment must have the required AWS/ECR permissions and network connectivity to retrieve the image.

---

# 23. Docker Compose Deployment

Docker Compose is then used to start or update the application.

The conceptual sequence is:

```text
ECR
 │
 ▼
Pull Image
 │
 ▼
Docker Compose
 │
 ▼
Application Container
```

Depending on the implementation, the deployment may involve stopping or replacing the existing application container before starting the updated version.

The exact commands should follow the Compose configuration in the repository.

---

# 24. Extract Deployment Logic into a Shell Script

The project material identifies extracting deployment logic into a shell script as an improvement in Part II.

Instead of putting every deployment command directly into the Jenkinsfile:

```text
Jenkinsfile
    │
    ├── Command
    ├── Command
    ├── Command
    ├── Command
    └── Command
```

the deployment logic can be organized as:

```text
Jenkinsfile
     │
     ▼
Deployment Script
     │
     ├── Authenticate
     ├── Pull Image
     ├── Deploy
     └── Verify
```

This makes the Jenkinsfile easier to maintain while keeping deployment-specific shell logic in a dedicated script.

---

# 25. Execute Part II Pipeline

Trigger the Jenkins pipeline.

Monitor each stage:

```text
Checkout
   ↓
Build
   ↓
Docker Build
   ↓
ECR Authentication
   ↓
Push Image
   ↓
SSH / Deployment
   ↓
Docker Compose
   ↓
Verification
```

If the pipeline fails, identify the exact stage before making changes.

---

# 26. Verify Part II

Verify the image in ECR.

Then verify the EC2 deployment.

Useful commands include:

```bash
docker ps
```

and:

```bash
docker images
```

If Docker Compose is being used, inspect the Compose-managed services using the appropriate Compose command.

Finally, verify the application endpoint.

---

# 27. Part III — Complete CI/CD Pipeline

Part III completes the workflow by introducing dynamic versioning.

The project material identifies the final workflow as combining:

* Docker Compose
* ECR
* Dynamic versioning
* Jenkins
* EC2 deployment.

The pipeline becomes:

```text
Source
  ↓
Jenkins
  ↓
Build
  ↓
Version
  ↓
Docker Build
  ↓
ECR
  ↓
EC2
  ↓
Docker Compose
  ↓
Application
```

---

# 28. Dynamic Image Versioning

A version should be generated as part of the pipeline.

The version can then be used to tag the Docker image.

Conceptually:

```text
Jenkins Build
      │
      ▼
Version Generation
      │
      ▼
Docker Image
      │
      ▼
application:<version>
```

For example:

```text
application:101
application:102
application:103
```

The actual versioning strategy should match the implementation used in the repository.

---

# 29. Push the Versioned Image

The versioned image is pushed to ECR.

```text
Docker Image
     │
     │ Tag
     ▼
Versioned Image
     │
     │ Push
     ▼
Amazon ECR
```

This makes each build identifiable.

---

# 30. Deploy the Versioned Image

The EC2 deployment configuration must reference the version that Jenkins has published.

The flow becomes:

```text
Jenkins
   │
   ▼
Version = <build-version>
   │
   ▼
ECR
   │
   ▼
EC2
   │
   ▼
Docker Compose
   │
   ▼
Versioned Application
```

This prevents the deployment workflow from depending entirely on one static image tag.

---

# 31. Complete Pipeline Execution

The final pipeline should follow a sequence similar to:

```text
1. Checkout Source
       ↓
2. Build Application
       ↓
3. Run Tests
       ↓
4. Generate Version
       ↓
5. Build Docker Image
       ↓
6. Tag Docker Image
       ↓
7. Authenticate with ECR
       ↓
8. Push Image to ECR
       ↓
9. Connect to EC2
       ↓
10. Pull Versioned Image
       ↓
11. Deploy with Docker Compose
       ↓
12. Verify Application
```

The exact Jenkins stages should match the actual Jenkinsfile.

---

# 32. Pipeline Verification

A successful final pipeline should be verified at several levels.

## Jenkins

Confirm that all required stages completed successfully.

## ECR

Confirm that the expected versioned image exists.

## EC2

Confirm that the deployment host is available.

## Docker

Confirm that the expected image and container are present.

## Docker Compose

Confirm that the application services are running.

## Application

Confirm that the application responds through the expected endpoint.

---

# 33. Deployment Rollout Logic

When a new version is deployed, the process should make the transition from the previous version to the new version clear.

Conceptually:

```text
Current Version
      │
      ▼
New Version Published
      │
      ▼
EC2 Pulls New Version
      │
      ▼
Docker Compose
      │
      ▼
New Version Running
      │
      ▼
Application Verification
```

If the application does not function correctly after deployment, use the pipeline output, Docker logs, Compose status, and application behavior to identify the failure.

---

# 34. Cleanup

After testing, remove resources that are no longer required.

Potential cleanup areas include:

* Unused EC2 resources
* Unused Docker containers
* Unused local Docker images
* Unused ECR images
* Temporary Jenkins resources

Do not delete resources that are still required by another project.

---

# 35. Deployment Verification Checklist

## Part I

* [ ] Jenkins installed and operational
* [ ] SSH Agent plugin installed
* [ ] EC2 SSH credentials configured
* [ ] Jenkinsfile configured
* [ ] EC2 reachable through SSH
* [ ] Docker available on EC2
* [ ] Docker image accessible
* [ ] Security Group configured
* [ ] Multi-branch pipeline configured
* [ ] Application deployed
* [ ] Application verified

## Part II

* [ ] Docker Compose installed
* [ ] `docker-compose.yaml` created
* [ ] ECR repository available
* [ ] Jenkins can authenticate with ECR
* [ ] Docker image built
* [ ] Image pushed to ECR
* [ ] EC2 can retrieve image
* [ ] Docker Compose deployment succeeds
* [ ] Application verified
* [ ] Deployment logic extracted into script where implemented

## Part III

* [ ] Version generated
* [ ] Image tagged with version
* [ ] Versioned image pushed to ECR
* [ ] EC2 retrieves correct version
* [ ] Docker Compose deploys correct version
* [ ] Jenkins pipeline completes successfully
* [ ] Application verified

---

# 36. Deployment Failure Isolation

When the pipeline fails, identify the first failing stage.

```text
Source Checkout
      │
      ▼
Build
      │
      ▼
Docker Build
      │
      ▼
Versioning
      │
      ▼
ECR Authentication
      │
      ▼
ECR Push
      │
      ▼
SSH
      │
      ▼
ECR Pull
      │
      ▼
Docker Compose
      │
      ▼
Application
```

Do not modify multiple pipeline stages simultaneously.

First identify the failing stage, collect the relevant logs, make one change, and rerun the affected workflow.

---

# 37. Security Checklist

Before considering the deployment complete:

* [ ] SSH private key stored securely
* [ ] AWS credentials stored securely
* [ ] ECR credentials protected
* [ ] No secrets committed to Git
* [ ] No credentials embedded in Jenkinsfile
* [ ] No credentials embedded in Compose files
* [ ] EC2 Security Group reviewed
* [ ] Only required ports exposed
* [ ] Jenkins access appropriately secured

---

# 38. Final Deployment Architecture

The completed deployment workflow is:

```text
                         Source Repository
                                │
                                ▼
                         ┌─────────────┐
                         │   Jenkins   │
                         └──────┬──────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                    ▼           ▼           ▼
                  Build      Version     Docker
                                           Build
                                              │
                                              ▼
                                      ┌─────────────┐
                                      │     ECR     │
                                      └──────┬──────┘
                                             │
                                             │ Pull
                                             ▼
                                      ┌─────────────┐
                                      │     EC2     │
                                      └──────┬──────┘
                                             │
                                             ▼
                                      Docker Compose
                                             │
                                             ▼
                                      Application
```

---

# 39. Deployment Outcome

A successful deployment demonstrates:

```text
Source Code
    ↓
Automated Build
    ↓
Container Image
    ↓
Versioned Artifact
    ↓
Amazon ECR
    ↓
EC2 Deployment
    ↓
Docker Compose
    ↓
Running Application
```

The key improvement over the previous manual EC2 deployment project is that Jenkins coordinates these operations as a repeatable CI/CD workflow.

---

# 40. Final Verification

Before marking the deployment complete, confirm:

```text
[✓] Jenkins pipeline executes
[✓] Application builds successfully
[✓] Docker image is created
[✓] Image is versioned where applicable
[✓] Image reaches ECR
[✓] EC2 can retrieve the image
[✓] Docker Compose deploys the application
[✓] Application container is running
[✓] Application is externally accessible
[✓] No credentials are exposed
```

**Deployment Status:** Complete when all applicable checks for the implemented stage pass.
