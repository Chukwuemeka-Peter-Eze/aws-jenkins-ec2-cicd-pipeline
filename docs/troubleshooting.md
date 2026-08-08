# AWS Jenkins CI/CD Pipeline - Troubleshooting Guide

## 1. Purpose

This guide provides a structured troubleshooting process for the AWS Jenkins CI/CD deployment pipeline.

The project progresses through three deployment stages:

```text
Part I
Jenkins → SSH → EC2 → Docker

Part II
Jenkins → Docker → ECR → EC2 → Docker Compose

Part III
Jenkins → Versioning → ECR → EC2 → Docker Compose
```

The AWS project material identifies Jenkins SSH Agent, EC2 SSH credentials, Docker, Docker Compose, ECR, multi-branch pipelines, and dynamic versioning as the major components of the workflow.

---

# 2. Troubleshooting Philosophy

Do not immediately change configuration when a pipeline fails.

Use this sequence:

```text
Observe
   ↓
Identify failing stage
   ↓
Read logs
   ↓
Identify affected component
   ↓
Test component independently
   ↓
Apply one change
   ↓
Rerun
   ↓
Verify
```

The most important question is:

> **What is the first stage that actually failed?**

Everything after that point may simply be a consequence of the earlier failure.

---

# 3. Pipeline Failure Map

Use the following pipeline as the primary troubleshooting map:

```text
Source Checkout
      │
      ▼
Jenkins
      │
      ├── Build
      │
      ├── Test
      │
      ├── Docker Build
      │
      ├── Versioning
      │
      ├── ECR Authentication
      │
      ├── ECR Push
      │
      ├── SSH
      │
      ├── ECR Pull
      │
      ├── Docker Compose
      │
      └── Application
```

Find the first failed stage and start there.

---

# 4. Jenkins Pipeline Does Not Start

## Symptoms

* Pipeline does not execute.
* Branch is not discovered.
* Jenkins shows no build.
* Jenkinsfile changes do not appear.

## Checks

Confirm:

* Jenkins is running.
* The correct repository is configured.
* The correct branch is available.
* The Jenkinsfile exists in the expected location.
* The multi-branch pipeline is configured correctly.
* Jenkins can access the source repository.

The project material identifies execution of a multi-branch pipeline as part of the initial Jenkins deployment stage.

---

# 5. Jenkins Cannot Access the Repository

## Symptoms

The pipeline fails during checkout.

Possible causes include:

* Incorrect repository URL
* Invalid credentials
* Repository access restrictions
* Incorrect branch configuration
* Network connectivity problems

## Troubleshooting

Check the Jenkins job configuration.

Verify the repository URL.

Verify that the credentials configured in Jenkins are the credentials intended for repository access.

Do not place repository credentials directly into the Jenkinsfile.

---

# 6. Jenkinsfile Not Found

## Symptoms

Jenkins discovers the repository but cannot find the pipeline definition.

## Checks

Confirm that:

```text
Jenkinsfile
```

exists in the expected repository location.

Check:

```text
Repository
   │
   └── Jenkinsfile
```

Also confirm that the configured pipeline script path matches the actual file location.

---

# 7. Jenkins Build Stage Fails

## Symptoms

The pipeline starts but fails during the application build.

## Investigation

Start with the Jenkins console output.

Determine:

* Which command failed?
* Which directory was Jenkins operating in?
* Were required files available?
* Were required dependencies installed?
* Did the build command return a non-zero exit code?

Do not troubleshoot Docker or ECR until the application build itself succeeds.

The correct sequence is:

```text
Application Build
      │
      ▼
Successful?
   ┌──┴──┐
  No    Yes
  │      │
Fix     Continue
Build
```

---

# 8. Docker Build Fails

## Symptoms

The Jenkins pipeline reaches the Docker build stage but fails to create the image.

## Checks

Verify:

* `Dockerfile` exists.
* Dockerfile path is correct.
* Build context is correct.
* Required application files exist.
* Docker is available to Jenkins.
* The Docker command is syntactically correct.

Test independently where appropriate:

```bash
docker build .
```

If the same command fails outside Jenkins, the issue is likely related to the Docker build rather than Jenkins.

---

# 9. Jenkins Cannot Execute Docker

## Symptoms

Jenkins reports that Docker cannot be found or executed.

Possible causes:

* Docker is not installed.
* Jenkins does not have permission to access Docker.
* Docker is not available in Jenkins' execution environment.
* PATH configuration differs between interactive and Jenkins sessions.

## Investigation

Check Docker availability from the environment where Jenkins executes commands.

For example:

```bash
docker --version
```

Then:

```bash
docker ps
```

The two commands help distinguish between Docker installation and Docker access problems.

---

# 10. SSH Connection to EC2 Fails

## Symptoms

The Jenkins pipeline reaches the deployment stage but cannot connect to EC2.

Possible causes:

* Incorrect SSH credential
* Incorrect username
* Incorrect host
* EC2 instance unavailable
* Security Group restriction
* SSH service problem
* Incorrect key configuration

The project material specifically identifies the Jenkins SSH Agent plugin and EC2 SSH credentials as part of Part I.

---

# 11. SSH Credential Problems

## Symptoms

Jenkins reports authentication or key-related errors.

## Checks

Verify:

* Correct Jenkins credential is selected.
* Correct private key is stored.
* Correct EC2 username is being used.
* Credential ID matches the Jenkinsfile configuration.
* The key corresponds to the EC2 instance.

Do not solve credential problems by placing the private key directly inside the repository.

---

# 12. EC2 Security Group Blocks SSH

## Symptoms

Jenkins cannot establish the SSH connection even though the credential appears correct.

Check the EC2 Security Group.

Confirm that SSH access is allowed from the intended source.

Avoid using unrestricted access when a narrower rule is appropriate.

The project includes Security Group configuration as part of the Jenkins deployment workflow.

---

# 13. Jenkins Connects to EC2 but Commands Fail

## Symptoms

SSH succeeds, but deployment commands fail.

This usually indicates that the SSH connection itself is not the problem.

Investigate:

```text
SSH
 │
 ├── Successful
 │
 ▼
Remote Command
 │
 └── Failed
```

Check:

* Docker installation
* Docker permissions
* Working directory
* File paths
* Environment variables
* Command syntax

---

# 14. Docker Image Push to ECR Fails

## Symptoms

The image builds successfully but cannot be pushed to ECR.

The workflow is:

```text
Docker Build
     │
     ▼
Image
     │
     ▼
ECR Authentication
     │
     ▼
ECR Push
```

If the build succeeds but the push fails, focus on:

* AWS authentication
* ECR permissions
* Repository name
* Registry URI
* Image tag
* Network connectivity

---

# 15. ECR Authentication Failure

## Symptoms

Jenkins cannot authenticate with ECR.

Possible causes include:

* Missing AWS permissions
* Incorrect AWS credentials
* Incorrect region
* Incorrect registry configuration
* Authentication command failure

Verify the AWS/ECR configuration used by the pipeline.

Do not expose AWS credentials in Jenkins console output.

---

# 16. ECR Repository Does Not Exist

## Symptoms

The pipeline authenticates successfully but cannot push the image.

Verify that the expected ECR repository exists.

Check:

```text
AWS
 │
 └── ECR
      │
      └── Expected Repository
```

Also verify that the repository name used by Jenkins exactly matches the intended repository.

---

# 17. Docker Image Tag Is Incorrect

## Symptoms

The image builds but the push or deployment references the wrong image.

Check the image tag.

Conceptually:

```text
<registry>/<repository>:<tag>
```

Verify each component:

```text
Registry
Repository
Tag
```

A mismatch between the tag generated during the build and the tag used during deployment can cause the EC2 deployment to retrieve the wrong image or fail to find the image.

---

# 18. ECR Image Push Succeeds but EC2 Cannot Pull

## Symptoms

The image exists in ECR, but EC2 cannot retrieve it.

Investigate:

* EC2 AWS permissions
* ECR authentication
* Region configuration
* Repository name
* Image tag
* Network connectivity
* Docker availability

The deployment path is:

```text
ECR
 │
 │ Pull
 ▼
EC2
 │
 ▼
Docker
```

Test each layer independently.

---

# 19. Docker Pull Fails on EC2

## Symptoms

EC2 cannot retrieve the image.

Verify:

```bash
docker images
```

and inspect the exact image reference being requested.

Confirm that the image exists in ECR with the expected tag.

If the image exists but the pull fails, investigate authentication and permissions before changing Docker configuration.

---

# 20. Docker Compose Command Fails

## Symptoms

The pipeline reaches the deployment stage but Docker Compose fails.

The project introduces Docker Compose in Part II.

Check:

* Docker Compose installation
* Compose file location
* Compose file syntax
* Image reference
* Port configuration
* Environment configuration
* Docker availability

Verify the Compose installation:

```bash
docker compose version
```

---

# 21. `docker-compose.yaml` Cannot Be Found

## Symptoms

The deployment host cannot locate the Compose configuration.

Verify the working directory.

The expected relationship is:

```text
Deployment Directory
       │
       └── docker-compose.yaml
```

The Jenkins deployment command must execute from the correct directory or provide the correct Compose file path.

---

# 22. Docker Compose Starts but Application Fails

## Symptoms

Docker Compose executes successfully, but the application does not work.

Do not immediately assume that Compose itself is broken.

Separate the problem:

```text
Compose
   │
   ▼
Container
   │
   ▼
Application
```

If the container is running but the application is inaccessible, investigate the application runtime and network configuration.

---

# 23. Container Starts but Application Is Not Accessible

Check:

* Container status
* Port mapping
* EC2 Security Group
* Application listening address
* Application listening port

A common architecture is:

```text
Internet
   │
   ▼
EC2 Security Group
   │
   ▼
Host Port
   │
   ▼
Container Port
   │
   ▼
Application
```

A failure at any layer can prevent browser access.

---

# 24. Browser Shows Connection Failure

If the pipeline reports success but the application is inaccessible:

### Step 1

Confirm the EC2 instance is running.

### Step 2

Confirm the container is running.

```bash
docker ps
```

### Step 3

Confirm the expected port mapping.

### Step 4

Review the EC2 Security Group.

### Step 5

Check application logs.

### Step 6

Test the application directly from the EC2 host where appropriate.

The goal is to determine whether the problem is:

```text
Infrastructure
      or
Container
      or
Application
      or
Network
```

---

# 25. Jenkins Pipeline Succeeds but Deployment Is Wrong

A green Jenkins pipeline does not automatically prove that the correct application version is running.

Verify:

```text
Jenkins Build
      │
      ▼
ECR Image
      │
      ▼
EC2 Image
      │
      ▼
Running Container
      │
      ▼
Application
```

Confirm that the artifact produced by the successful Jenkins build is the artifact actually deployed.

---

# 26. Dynamic Versioning Problems

Dynamic versioning is introduced in Part III.

## Symptoms

* Version is missing.
* Version is empty.
* Incorrect image tag is generated.
* ECR contains unexpected tags.
* Deployment references an unavailable tag.

## Investigation

Trace the version through the pipeline:

```text
Version Generation
       │
       ▼
Docker Tag
       │
       ▼
ECR
       │
       ▼
Deployment
```

The value generated by Jenkins must match the value used by the deployment stage.

---

# 27. Wrong Version Deployed

If Jenkins creates:

```text
application:103
```

but EC2 attempts to pull:

```text
application:102
```

the pipeline and deployment configuration are not synchronized.

Verify that the same version flows through:

```text
Generate
   ↓
Tag
   ↓
Push
   ↓
Pull
   ↓
Deploy
```

---

# 28. Old Application Version Still Running

If the new image exists in ECR but the old application remains active, investigate:

* Image tag
* Compose configuration
* Running container
* Deployment command
* Container replacement behavior

Confirm which image the running container is actually using.

The important distinction is:

```text
Image Exists
      ≠
Image Is Running
```

---

# 29. Jenkins Credentials Exposed

If credentials appear in:

* Jenkins console logs
* Jenkinsfile
* Shell scripts
* Docker Compose files
* Git history
* Screenshots

treat the credential as compromised.

Do not publish the affected value.

Replace/revoke the credential as appropriate and remove sensitive information from the repository and evidence.

---

# 30. Deployment Script Fails

If deployment logic has been extracted into a shell script, isolate the script from Jenkins.

The project material identifies this extraction as an improvement in Part II.

Test the script independently where appropriate.

Use:

```text
Jenkins
   │
   ▼
Deployment Script
   │
   ├── Authentication
   ├── Image Pull
   ├── Compose
   └── Verification
```

If the script fails outside Jenkins, the problem is likely in the deployment logic rather than Jenkins itself.

---

# 31. Jenkinsfile Becomes Difficult to Debug

Avoid putting every operation into one large Jenkinsfile.

A useful separation is:

```text
Jenkinsfile
     │
     ├── Pipeline orchestration
     │
     └── Deployment script
              │
              ├── ECR
              ├── Docker
              └── Compose
```

This makes responsibilities clearer and simplifies troubleshooting.

---

# 32. AWS Permission Problems

If AWS operations fail, determine exactly which AWS action failed.

Possible operations include:

```text
Authenticate
    ↓
Access ECR
    ↓
Push Image
    ↓
Pull Image
```

Do not respond to a permission error by granting unrestricted administrative access.

Identify the required operation and review the relevant IAM permissions.

---

# 33. Network Troubleshooting

When an AWS service cannot be reached, distinguish between:

```text
DNS
 │
 ▼
Network
 │
 ▼
Security Group
 │
 ▼
Application Port
 │
 ▼
Application
```

Check the appropriate layer rather than changing multiple network settings at once.

---

# 34. Jenkins Console Output

The Jenkins console log should be treated as the primary evidence source for pipeline failures.

When investigating:

1. Find the first error.
2. Identify the command that produced it.
3. Identify the environment where it ran.
4. Reproduce the command independently where appropriate.
5. Fix the underlying issue.
6. Rerun the pipeline.

Avoid focusing only on the final error message because later stages may fail as a consequence of an earlier failure.

---

# 35. Troubleshooting Decision Tree

Use this simplified decision tree:

```text
Pipeline Failed?
      │
      ▼
Which Stage?
      │
      ├── Checkout
      │      └── Repository / Credentials
      │
      ├── Build
      │      └── Application
      │
      ├── Docker Build
      │      └── Dockerfile / Build Context
      │
      ├── Versioning
      │      └── Jenkins Version Logic
      │
      ├── ECR Authentication
      │      └── AWS Credentials / Permissions
      │
      ├── ECR Push
      │      └── Repository / Tag / Permissions
      │
      ├── SSH
      │      └── Credentials / Network / EC2
      │
      ├── ECR Pull
      │      └── Authentication / Permissions / Tag
      │
      ├── Docker Compose
      │      └── Compose / Configuration
      │
      └── Application
             └── Runtime / Ports / Network
```

---

# 36. Part-by-Part Troubleshooting

## Part I

Focus on:

```text
Jenkins
  ↓
SSH
  ↓
EC2
  ↓
Docker
  ↓
Application
```

Primary issues:

* Jenkins configuration
* SSH credentials
* Security Groups
* Docker
* Remote commands

---

## Part II

Focus on:

```text
Jenkins
  ↓
Docker
  ↓
ECR
  ↓
EC2
  ↓
Docker Compose
  ↓
Application
```

Additional issues:

* ECR authentication
* Image push
* Image pull
* Docker Compose
* Compose configuration

---

## Part III

Focus on:

```text
Jenkins
  ↓
Version
  ↓
Docker
  ↓
ECR
  ↓
EC2
  ↓
Docker Compose
```

Additional issue:

* Version consistency between build and deployment

---

# 37. Evidence Collection

When troubleshooting, capture evidence before changing configuration.

Useful evidence includes:

* Jenkins console output
* Jenkins stage status
* ECR repository contents
* Docker image list
* Docker container status
* Docker Compose status
* EC2 configuration
* Security Group configuration
* Application output
* Browser behavior

This evidence can also be used later in the project's technical documentation.

---

# 38. What Not to Do

Avoid these troubleshooting habits:

### Do not blindly rerun the pipeline repeatedly.

A failed pipeline usually requires investigation.

### Do not change several components simultaneously.

You will lose the ability to identify which change fixed the problem.

### Do not expose credentials to diagnose authentication.

Use secure credential mechanisms.

### Do not immediately grant Administrator-level AWS permissions.

Identify the required permission instead.

### Do not assume a green pipeline means a healthy application.

Verify the deployment independently.

---

# 39. Final Troubleshooting Checklist

```text
[ ] Identify the first failed Jenkins stage
[ ] Read the console output
[ ] Identify the affected component
[ ] Test the component independently
[ ] Verify credentials
[ ] Verify AWS permissions
[ ] Verify EC2 availability
[ ] Verify Security Group rules
[ ] Verify Docker
[ ] Verify ECR
[ ] Verify image tags
[ ] Verify Docker Compose
[ ] Verify running containers
[ ] Verify application ports
[ ] Verify application behavior
[ ] Apply one change
[ ] Rerun pipeline
[ ] Confirm resolution
```

---

# 40. Final Principle

The most reliable troubleshooting strategy for this project is:

```text
Do not troubleshoot "Jenkins" as one system.

Troubleshoot the pipeline stage.
        ↓
Identify the component.
        ↓
Collect evidence.
        ↓
Test the component.
        ↓
Fix the root cause.
        ↓
Verify the complete deployment.
```

The Jenkins pipeline is a chain:

```text
Source
  ↓
Build
  ↓
Docker
  ↓
Version
  ↓
ECR
  ↓
EC2
  ↓
Docker Compose
  ↓
Application
```

A failure anywhere in that chain should be isolated to its specific stage before changes are made.
