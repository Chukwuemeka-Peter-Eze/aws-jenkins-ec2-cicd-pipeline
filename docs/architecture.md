# AWS Jenkins CI/CD Pipeline — Architecture

## 1. Purpose

This document describes the architecture of the Jenkins CI/CD deployment workflow used to build and deploy applications to AWS.

The architecture evolves through three stages:

```text
Part I
Jenkins → SSH → EC2 → Docker

Part II
Jenkins → Docker → ECR → EC2 → Docker Compose

Part III
Jenkins → Dynamic Versioning → ECR
        → EC2 → Docker Compose
```

The AWS project material identifies Jenkins SSH Agent, EC2 SSH credentials, Docker, Docker Compose, ECR, multi-branch pipelines, and dynamic image versioning as the key components introduced across these stages.

---

# 2. High-Level Architecture

The final architecture can be represented as:

```text
                         ┌──────────────────────┐
                         │      Developer       │
                         └──────────┬───────────┘
                                    │
                                    │ Source Code
                                    ▼
                         ┌──────────────────────┐
                         │   Source Repository  │
                         └──────────┬───────────┘
                                    │
                                    │ Webhook / Trigger
                                    ▼
                         ┌──────────────────────┐
                         │       Jenkins        │
                         │                      │
                         │  CI/CD Pipeline      │
                         └──────────┬───────────┘
                                    │
                  ┌─────────────────┼─────────────────┐
                  │                 │                 │
                  ▼                 ▼                 ▼
               Build            Docker Build      Versioning
                  │                 │                 │
                  └─────────────────┼─────────────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │     Amazon ECR       │
                         │                      │
                         │   Docker Images      │
                         └──────────┬───────────┘
                                    │
                                    │ Pull
                                    ▼
                         ┌──────────────────────┐
                         │     Amazon EC2       │
                         │                      │
                         │  Docker Compose      │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Application       │
                         │      Container       │
                         └──────────────────────┘
```

---

# 3. Architecture Components

## 3.1 Developer

The developer makes changes to the application source code.

The change is committed to the source repository, which provides the starting point for the CI/CD workflow.

```text
Developer
    │
    ▼
Source Repository
```

The developer does not need to manually perform every deployment operation on the EC2 server once the pipeline is configured.

---

# 4. Source Repository

The source repository contains the application source code and pipeline configuration.

The Jenkinsfile defines the pipeline logic that Jenkins executes.

A simplified relationship is:

```text
Source Repository
       │
       ├── Application Code
       │
       ├── Docker Configuration
       │
       ├── Docker Compose Configuration
       │
       └── Jenkinsfile
```

The exact files depend on the stage of the project being implemented.

---

# 5. Jenkins

Jenkins acts as the CI/CD automation engine.

It coordinates the build and deployment workflow.

Conceptually:

```text
                    Jenkins
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
      Build          Package        Deploy
        │              │              │
        ▼              ▼              ▼
     Docker          ECR             EC2
```

The pipeline converts previously manual deployment operations into repeatable automated stages.

---

# 6. Jenkins Credentials

Jenkins requires access to external systems involved in the deployment.

The first stage specifically introduces the Jenkins SSH Agent plugin and SSH credentials for the EC2 instance.

The conceptual architecture is:

```text
Jenkins
   │
   ├── SSH Credentials
   │       │
   │       ▼
   │      EC2
   │
   └── Registry/AWS Credentials
           │
           ▼
          ECR
```

Credentials should be managed through Jenkins' credential mechanism rather than hard-coded inside pipeline files.

---

# 7. Part I Architecture — Jenkins to EC2

## 7.1 Overview

The first stage focuses on automating deployment to EC2 using Jenkins and SSH.

The architecture is:

```text
┌─────────────────┐
│ Source Repository│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Jenkins     │
└────────┬────────┘
         │
         │ SSH
         ▼
┌─────────────────┐
│       EC2       │
│                 │
│     Docker      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Application   │
│    Container    │
└─────────────────┘
```

Jenkins connects to the EC2 instance and executes the required Docker deployment commands.

---

# 8. Part I Deployment Flow

The first stage follows this general sequence:

```text
Source Code
    │
    ▼
Jenkins Pipeline
    │
    ├── Build
    │
    ├── Docker Operations
    │
    └── SSH Deployment
            │
            ▼
          EC2
            │
            ▼
        Docker
            │
            ▼
       Application
```

The project material identifies remote Docker command execution on EC2 and Docker repository authentication as part of this stage.

---

# 9. Part I Network Boundary

The deployment still depends on the EC2 Security Group.

```text
                  Internet
                     │
                     ▼
             ┌───────────────┐
             │ Security Group│
             └───────┬───────┘
                     │
                     ▼
                  EC2
                     │
                  Docker
                     │
                     ▼
               Application
```

The Security Group controls which network traffic can reach the EC2 instance.

---

# 10. Part II Architecture — Docker Compose + ECR

The second stage expands the architecture by introducing:

* Docker Compose
* Amazon ECR
* Jenkins-driven image publication
* EC2 image retrieval

The architecture becomes:

```text
┌──────────────────┐
│ Source Repository│
└─────────┬────────┘
          │
          ▼
┌──────────────────┐
│     Jenkins      │
└─────────┬────────┘
          │
          │ Docker Build
          ▼
┌──────────────────┐
│   Docker Image   │
└─────────┬────────┘
          │
          │ Push
          ▼
┌──────────────────┐
│    Amazon ECR    │
└─────────┬────────┘
          │
          │ Pull
          ▼
┌──────────────────┐
│       EC2        │
│                  │
│ Docker Compose   │
└─────────┬────────┘
          │
          ▼
┌──────────────────┐
│   Application    │
│    Container     │
└──────────────────┘
```

---

# 11. Why ECR Is Introduced

The use of ECR creates a dedicated image distribution layer.

Instead of relying on the EC2 host to obtain the application image directly from the development environment, the image is published to a centralized AWS registry.

```text
Build Environment
       │
       ▼
Docker Image
       │
       ▼
Amazon ECR
       │
       ▼
EC2
```

This separates:

* Image creation
* Image storage
* Image deployment

---

# 12. Docker Compose Layer

Docker Compose provides the deployment configuration used on EC2.

Conceptually:

```text
EC2
 │
 └── Docker Compose
       │
       ├── Image
       ├── Port Configuration
       ├── Environment
       └── Container Configuration
```

The project material specifically introduces installing Docker Compose on EC2 and creating a `docker-compose.yaml` file.

---

# 13. Part II Deployment Flow

The second stage can be represented as:

```text
Developer
    │
    ▼
Source Repository
    │
    ▼
Jenkins
    │
    ├── Build Application
    │
    ├── Build Docker Image
    │
    └── Push Image
            │
            ▼
          ECR
            │
            │ Pull
            ▼
           EC2
            │
            ▼
      Docker Compose
            │
            ▼
        Application
```

---

# 14. Part III Architecture — Dynamic Versioning

The final stage adds dynamic image versioning.

The architecture becomes:

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
    │
    ├── Test
    │
    ├── Generate Version
    │
    ├── Build Image
    │
    ├── Tag Image
    │
    └── Push Image
            │
            ▼
          ECR
            │
            │ Pull Versioned Image
            ▼
           EC2
            │
            ▼
      Docker Compose
            │
            ▼
        Application
```

The project material identifies dynamic versioning as part of the complete pipeline stage.

---

# 15. Dynamic Image Versioning

Dynamic versioning allows different builds to produce distinguishable image tags.

For example:

```text
Build 101
    ↓
application:101

Build 102
    ↓
application:102

Build 103
    ↓
application:103
```

The actual version-generation mechanism should match the implementation in the Jenkinsfile.

The important architectural principle is that the deployed image can be associated with a particular pipeline build.

---

# 16. Artifact Promotion Flow

The final architecture establishes a clear artifact lifecycle:

```text
Source Code
     │
     ▼
Jenkins Build
     │
     ▼
Docker Image
     │
     ▼
Versioned Image
     │
     ▼
Amazon ECR
     │
     ▼
EC2
     │
     ▼
Docker Compose
     │
     ▼
Running Application
```

The Docker image becomes the artifact that moves between the CI and deployment environments.

---

# 17. Separation of Responsibilities

The architecture separates responsibilities between systems.

| Component         | Responsibility                          |
| ----------------- | --------------------------------------- |
| Source Repository | Source code and pipeline configuration  |
| Jenkins           | CI/CD automation                        |
| Docker            | Application packaging                   |
| ECR               | Image storage and distribution          |
| EC2               | Application hosting                     |
| Docker Compose    | Container deployment configuration      |
| Security Group    | Network access control                  |
| SSH               | Remote administration/deployment access |

This separation makes the deployment workflow easier to understand and troubleshoot.

---

# 18. Security Architecture

Security exists at multiple layers.

```text
                    Jenkins
                       │
                ┌──────┴──────┐
                │ Credentials │
                └──────┬──────┘
                       │
          ┌────────────┴────────────┐
          │                         │
          ▼                         ▼
        SSH                       AWS/ECR
          │                         │
          ▼                         ▼
         EC2                       ECR
          │
          ▼
   Security Group
          │
          ▼
     Application
```

Important security boundaries include:

* Jenkins credentials
* EC2 SSH access
* AWS permissions
* ECR access
* Security Group rules

Credentials should never be embedded directly into source code or committed to the repository.

---

# 19. CI/CD Boundary

The architecture can be divided into two major areas:

```text
┌──────────────────────── CI ────────────────────────┐
│                                                    │
│ Source → Jenkins → Build → Docker → Version       │
│                                                    │
└─────────────────────────┬──────────────────────────┘
                          │
                          ▼
                    Amazon ECR
                          │
                          ▼
┌────────────────────── CD ──────────────────────────┐
│                                                    │
│ ECR → EC2 → Docker Compose → Application           │
│                                                    │
└────────────────────────────────────────────────────┘
```

This distinction is useful because it separates application artifact creation from deployment.

---

# 20. Manual vs Automated Architecture

The previous EC2 project used a predominantly manual workflow:

```text
Engineer
   │
   ▼
SSH
   │
   ▼
EC2
   │
   ▼
Docker
   │
   ▼
Application
```

The Jenkins project changes this to:

```text
Source Repository
       │
       ▼
     Jenkins
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
Application
```

The key architectural improvement is the removal of repetitive manual deployment operations.

---

# 21. Pipeline Evolution

The complete architectural progression is:

```text
Stage 1
────────────────────────────

Jenkins
   │
   │ SSH
   ▼
 EC2
   │
 Docker
   │
   ▼
Application


Stage 2
────────────────────────────

Jenkins
   │
 Docker Build
   │
   ▼
 ECR
   │
 Pull
   ▼
 EC2
   │
 Docker Compose
   │
   ▼
Application


Stage 3
────────────────────────────

Jenkins
   │
 Build
   │
 Version
   │
 Docker Build
   │
 Push
   ▼
 ECR
   │
 Versioned Image
   ▼
 EC2
   │
 Docker Compose
   │
   ▼
Application
```

Each stage builds upon the previous one.

---

# 22. Failure Boundaries

The architecture also provides clear troubleshooting boundaries.

A pipeline failure can occur at:

```text
Source Repository
       │
       ▼
    Jenkins
       │
       ├── Build Failure
       │
       ├── Docker Build Failure
       │
       ├── Authentication Failure
       │
       ▼
      ECR
       │
       ├── Push Failure
       ├── Pull Failure
       │
       ▼
      EC2
       │
       ├── SSH Failure
       ├── Docker Failure
       ├── Compose Failure
       │
       ▼
   Application
       │
       └── Runtime Failure
```

This structure provides a logical starting point for troubleshooting.

---

# 23. Observability and Evidence

Each major architectural boundary should produce evidence.

### Jenkins

Pipeline execution and console output.

### ECR

Published Docker image and tag.

### EC2

Running deployment environment.

### Docker

Running container.

### Docker Compose

Successful service deployment.

### Application

Successful browser or endpoint verification.

This allows the deployment to be verified progressively rather than relying solely on the final application result.

---

# 24. Architecture Principles

The project demonstrates several important engineering principles.

### Automation

Repeated deployment tasks are moved from manual execution into Jenkins.

### Artifact-Based Deployment

The Docker image acts as the deployment artifact.

### Separation of Concerns

Build, image storage, and application hosting are handled by different components.

### Versionability

Dynamic image tags provide identifiable deployment artifacts.

### Reproducibility

Pipeline stages provide a repeatable deployment process.

### Security

Credentials and infrastructure access are separated from application source code.

---

# 25. Final Architecture

The final CI/CD architecture is:

```text
                         ┌─────────────────┐
                         │    Developer    │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │ Source Repository│
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │     Jenkins     │
                         │                 │
                         │ Build           │
                         │ Test            │
                         │ Version         │
                         │ Docker Build    │
                         │ Image Push      │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   Amazon ECR    │
                         │                 │
                         │ Versioned Image │
                         └────────┬────────┘
                                  │
                                  │ Pull
                                  ▼
                         ┌─────────────────┐
                         │    Amazon EC2   │
                         │                 │
                         │ Docker Compose  │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   Application   │
                         │    Container    │
                         └─────────────────┘
```

The architecture demonstrates the progression from manually deploying applications to EC2 toward a repeatable CI/CD model in which Jenkins builds and publishes versioned container images and coordinates their deployment to EC2.

---

# 26. Architectural Summary

The fundamental transformation is:

```text
Manual Deployment
       ↓
Automation
       ↓
Artifact Management
       ↓
Versioned Releases
       ↓
Repeatable Deployment
```

The EC2 repository established the mechanics of running the application on AWS.

This Jenkins repository builds on those mechanics by automating the path from source code to deployed application.
