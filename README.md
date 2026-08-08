# AWS Jenkins CI/CD Pipeline

Build and deploy applications to Amazon EC2 using Jenkins CI/CD automation, Docker, Docker Compose, and Amazon Elastic Container Registry (ECR).

## Overview

This project demonstrates the evolution from a manual application deployment process to an automated CI/CD workflow using Jenkins and AWS.

The pipeline automates application build and deployment activities and progressively introduces:

* Jenkins pipeline automation
* SSH-based deployment to Amazon EC2
* Docker
* Docker Compose
* Amazon Elastic Container Registry (ECR)
* Dynamic image versioning

The project is organized into three stages that progressively improve the deployment workflow.

---

## Project Objectives

The main objectives of this project are to:

* Automate application deployment using Jenkins.
* Deploy applications to an Amazon EC2 instance.
* Use Jenkins to execute deployment operations remotely.
* Build and deploy Dockerized applications.
* Use Docker Compose to manage application containers.
* Store Docker images in Amazon ECR.
* Introduce dynamic Docker image versioning.
* Understand how CI/CD reduces repetitive manual deployment operations.

---

# 1. CI/CD Architecture

The overall workflow can be represented as:

```text
Developer
    │
    │ Source Code Change
    ▼
Source Repository
    │
    ▼
  Jenkins
    │
    ├── Build
    ├── Test
    ├── Package
    ├── Build Docker Image
    ├── Push Image
    │
    ▼
 Amazon ECR
    │
    │ Pull Image
    ▼
 Amazon EC2
    │
    ├── Docker
    │
    └── Docker Compose
    │
    ▼
Application
```

The exact pipeline stages depend on the implementation stage being used.

---

# 2. Jenkins Pipeline Stages

The project is divided into three progressive stages.

## Part I — Jenkins Pipeline to EC2

The first stage introduces Jenkins-based deployment to an EC2 instance.

The checklist identifies the following implementation activities:

* Install the Jenkins SSH Agent plugin.
* Create SSH credentials for the EC2 instance.
* Configure the Jenkinsfile to use the SSH agent.
* Execute Docker commands remotely on EC2.
* Authenticate with Docker Hub or another private Docker repository.
* Configure Security Group access.
* Execute a multi-branch pipeline.
* Deploy the web application to EC2.

The conceptual flow is:

```text
Source Repository
       │
       ▼
     Jenkins
       │
       │ SSH
       ▼
    EC2 Instance
       │
       ▼
     Docker
       │
       ▼
Application Container
```

This stage replaces the manual SSH-based deployment process with Jenkins automation.

---

# 3. Part II — Docker Compose and ECR

The second stage introduces Docker Compose and Amazon ECR.

The checklist identifies:

* Installing Docker Compose on EC2.
* Creating a `docker-compose.yaml` file.
* Configuring the Jenkinsfile to execute Docker Compose commands.
* Executing the Jenkins pipeline.
* Deploying the application to EC2.
* Extracting deployment logic into a shell script as an improvement.

The deployment model becomes:

```text
Source Repository
       │
       ▼
     Jenkins
       │
       ├── Build
       ├── Package
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

Docker Compose provides a structured way to define and run the application's container configuration.

---

# 4. Part III — Complete Pipeline

The third stage completes the CI/CD workflow by introducing dynamic image versioning.

The checklist identifies the final stage as using:

* Docker Compose
* ECR
* Dynamic versioning
* Jenkins
* EC2 deployment

The resulting workflow is:

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
    ├── Create Version
    ├── Build Docker Image
    ├── Tag Image
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

Dynamic versioning allows individual application builds to be identified through their image tags instead of relying on a single static image tag.

---

# 5. Technologies

| Technology     | Role                                       |
| -------------- | ------------------------------------------ |
| Jenkins        | CI/CD automation                           |
| Amazon EC2     | Application deployment environment         |
| Amazon ECR     | Docker image registry                      |
| Docker         | Application containerization               |
| Docker Compose | Container deployment and orchestration     |
| SSH            | Remote EC2 access                          |
| Git            | Source control and pipeline trigger source |

---

# 6. Jenkins

Jenkins is the automation engine responsible for executing the CI/CD workflow.

Instead of manually performing deployment commands on EC2, Jenkins executes the defined pipeline steps.

The pipeline can automate activities such as:

```text
Build
  ↓
Package
  ↓
Containerize
  ↓
Publish Image
  ↓
Deploy
  ↓
Verify
```

This provides a repeatable deployment process.

---

# 7. Jenkinsfile

The pipeline configuration is defined through a Jenkinsfile.

The Jenkinsfile describes the stages and commands Jenkins should execute.

A simplified pipeline structure is:

```text
Jenkinsfile
    │
    ├── Build
    │
    ├── Test
    │
    ├── Docker Build
    │
    ├── Image Push
    │
    └── Deployment
```

The actual stages should reflect the implementation present in this repository.

---

# 8. SSH-Based Deployment

The initial Jenkins deployment stage uses SSH to communicate with the EC2 instance.

Jenkins uses configured credentials to establish the connection.

Conceptually:

```text
Jenkins
   │
   │ SSH
   ▼
EC2 Instance
   │
   ▼
Docker Commands
```

This allows Jenkins to execute deployment operations on the EC2 host without requiring an engineer to manually SSH into the instance for every deployment.

The checklist specifically identifies the Jenkins SSH Agent plugin and EC2 SSH credentials as part of the first Jenkins deployment stage.

---

# 9. Amazon ECR

Amazon Elastic Container Registry provides a private registry for Docker images.

The workflow becomes:

```text
Jenkins
   │
   │ Build
   ▼
Docker Image
   │
   │ Push
   ▼
Amazon ECR
   │
   │ Pull
   ▼
EC2
```

ECR separates image storage from the EC2 compute environment.

This also provides a more AWS-native image distribution workflow for the later stages of the project.

---

# 10. Docker Compose

Docker Compose is introduced in the second stage of the project.

The Compose configuration describes how the application containers should be run.

Example structure:

```text
docker-compose.yaml
        │
        ▼
Application Services
        │
        ├── Container
        ├── Ports
        ├── Environment
        └── Images
```

The Jenkins pipeline can execute Docker Compose commands on the EC2 deployment host.

The checklist specifically identifies creating a `docker-compose.yaml` file and configuring Jenkins to execute Docker Compose during deployment.

---

# 11. Dynamic Versioning

The final pipeline introduces dynamic image versioning.

Instead of relying exclusively on a static image tag, the pipeline generates a version associated with the build.

Conceptually:

```text
Build 101
   │
   ▼
Application Image
   │
   ▼
Version: 101
```

The image can then be represented as:

```text
<registry>/<repository>:<version>
```

This makes individual builds easier to identify and distinguish.

---

# 12. Deployment Workflow

The complete workflow can be summarized as:

```text
1. Developer changes source code
          │
          ▼
2. Jenkins pipeline starts
          │
          ▼
3. Application is built
          │
          ▼
4. Docker image is created
          │
          ▼
5. Image is versioned
          │
          ▼
6. Image is pushed to ECR
          │
          ▼
7. EC2 retrieves the image
          │
          ▼
8. Docker Compose deploys application
          │
          ▼
9. Application becomes available
```

---

# 13. Multi-Branch Pipeline

The project uses Jenkins multi-branch pipeline functionality.

This allows Jenkins to discover and execute pipeline definitions associated with branches in the source repository.

The checklist explicitly identifies execution of a multi-branch pipeline as part of the first deployment stage.

The general relationship is:

```text
Source Repository
       │
       ├── Branch A
       │     └── Jenkinsfile
       │
       ├── Branch B
       │     └── Jenkinsfile
       │
       └── Branch C
             └── Jenkinsfile
                    │
                    ▼
                 Jenkins
```

---

# 14. Security Considerations

CI/CD automation introduces credentials and infrastructure access that must be protected.

Important security considerations include:

* Protect Jenkins credentials.
* Protect EC2 SSH credentials.
* Protect AWS credentials.
* Avoid committing secrets to source control.
* Avoid exposing registry credentials.
* Restrict EC2 Security Group access.
* Use appropriate IAM permissions.
* Avoid unnecessary administrative access.

Credentials should be stored in the appropriate Jenkins credential mechanism rather than directly inside the Jenkinsfile.

---

# 15. Verification

A successful pipeline should be verified at multiple levels.

### Jenkins

Confirm that the pipeline completed successfully.

### Image Registry

Confirm that the expected Docker image exists in the registry.

### EC2

Confirm that the deployment host is running.

### Docker

Confirm that the expected container is running.

### Docker Compose

Confirm that the Compose deployment completed successfully.

### Application

Confirm that the application is accessible through the expected endpoint.

---

# 16. Troubleshooting Approach

When a pipeline fails, investigate the stage where the failure occurred.

```text
Jenkins
  │
  ├── Source Checkout
  │
  ├── Build
  │
  ├── Docker Build
  │
  ├── Registry Push
  │
  ├── SSH
  │
  ├── Image Pull
  │
  ├── Docker Compose
  │
  └── Application
```

The Jenkins console output should be the first source of evidence when a pipeline stage fails.

The failure should then be traced to the corresponding infrastructure or application layer.

---

# 17. Project Progression

The project demonstrates a clear progression:

```text
Manual EC2 Deployment
        │
        ▼
Jenkins + SSH
        │
        ▼
Jenkins + Docker Compose
        │
        ▼
Jenkins + Docker Compose + ECR
        │
        ▼
Dynamic Image Versioning
```

Each stage improves the deployment workflow without changing the fundamental goal: reliably getting the application from source code into the EC2 runtime environment.

---

# 18. Engineering Value

This project demonstrates practical understanding of CI/CD beyond simply creating a Jenkins pipeline.

It shows how multiple engineering components work together:

```text
Source Control
      +
Jenkins
      +
Docker
      +
ECR
      +
EC2
      +
Docker Compose
      +
SSH
      +
AWS Networking
```

The project therefore provides a practical foundation for more advanced CI/CD and cloud-native deployment patterns.

---

# 19. Key Takeaways

* Jenkins can automate repetitive deployment operations.
* SSH can be used by Jenkins to interact with an EC2 deployment host.
* Docker provides the application packaging mechanism.
* ECR provides private Docker image storage.
* Docker Compose simplifies multi-container application deployment.
* Dynamic image versioning improves build identification.
* CI/CD pipelines should separate build, artifact publication, and deployment responsibilities.
* Deployment automation reduces the need for repeated manual server operations.
* Jenkins credentials and AWS access must be handled securely.
* Pipeline failures should be investigated using stage-specific evidence.

---

# 20. Related Projects

This repository is part of the AWS Services project portfolio:

### AWS EC2 Web Deployment

Manual deployment of a containerized application to EC2.

### AWS Jenkins CI/CD Pipeline

Automated application build and deployment using Jenkins and AWS.

### AWS ECR Docker Registry

Working with Amazon ECR as a private Docker image registry.

### AWS CLI Automation

Automating AWS infrastructure and service operations through the AWS CLI.

---

# 21. Project Status

**Status:** Completed

**Primary capability demonstrated:**

> Automated application build and deployment to AWS using Jenkins CI/CD, Docker, Docker Compose, Amazon ECR, and Amazon EC2.

**Pipeline progression:**

```text
Part I
Jenkins → SSH → EC2 → Docker

        ↓

Part II
Jenkins → Docker → ECR → EC2 → Docker Compose

        ↓

Part III
Jenkins → Dynamic Versioning → ECR
        → EC2 → Docker Compose
```

The project demonstrates the transition from manual cloud deployment to repeatable CI/CD automation.
