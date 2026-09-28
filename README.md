# AWS Jenkins CI/CD Pipeline

A practical CI/CD project built with **GitHub, Jenkins, Maven, Docker,
Docker Hub, SSH, and Amazon EC2**.

This repository documents an end-to-end pipeline I built and practiced
from source code commit through application deployment on an EC2 server.

The project started as a simple CI/CD exercise and became a useful
hands-on environment for understanding how source control, build
automation, containerization, image registries, Jenkins, Linux, SSH,
Docker, AWS infrastructure, and application deployment fit together.

------------------------------------------------------------------------

## Project Overview

The pipeline follows this workflow:

``` text
Developer
   |
   v
GitHub Repository
   |
   v
Jenkins Multibranch Pipeline
   |
   +----------------------+
   |                      |
   v                      v
Maven Build          Docker Build
   |                      |
   v                      v
JAR Artifact          Docker Image
                          |
                          v
                    Docker Hub
                          |
                          v
                 SSH to EC2 Server
                          |
                          v
                  Docker Pull Image
                          |
                          v
                  Docker Container
                          |
                          v
                  Spring Boot App
```

The application is exposed on **port 8081 on the EC2 host**, while the
application itself listens on **port 8080 inside the container**.

``` text
EC2 Host
  |
  | port 8081
  v
Docker Container
  |
  | port 8080
  v
Spring Boot Application
```

This separation is intentional because Jenkins is already using host
port `8080`.

------------------------------------------------------------------------

# What I Practiced

I practiced the project from beginning to end, including:

-   Git and GitHub source control
-   Jenkins Multibranch Pipeline
-   Jenkins Pipeline syntax
-   Jenkins shared/helper Groovy script
-   Maven application packaging
-   Docker image creation
-   Docker Hub authentication
-   Docker image publishing
-   Jenkins running inside Docker
-   Jenkins access to the host Docker daemon
-   Docker socket configuration
-   Docker group permissions
-   SSH-based deployment
-   Amazon EC2 administration
-   Linux command-line troubleshooting
-   Docker container lifecycle management
-   Application port mapping
-   Security Group configuration
-   CI/CD troubleshooting
-   Application container verification
-   Documentation of real deployment failures and fixes

No screenshots are required to understand or reproduce the project. The
repository focuses on the actual implementation, configuration,
commands, architecture, and troubleshooting decisions.

------------------------------------------------------------------------

# Technology Stack

  Technology                     Purpose
  ------------------------------ -----------------------------------
  Git                            Version control
  GitHub                         Source code hosting
  Jenkins                        CI/CD automation
  Jenkins Multibranch Pipeline   Branch-aware pipeline execution
  Groovy                         Jenkins pipeline/helper scripting
  Maven                          Java build and packaging
  Java                           Application runtime
  Docker                         Application containerization
  Docker Hub                     Container image registry
  SSH                            Remote EC2 deployment
  Amazon EC2                     Application deployment server
  AWS Security Groups            Network access control
  Linux                          Server administration

------------------------------------------------------------------------

# Repository Structure

``` text
aws-jenkins-ec2-cicd-pipeline/
│
├── Dockerfile
├── Jenkinsfile
├── script.groovy
├── pom.xml
├── src/
│   └── ...
│
├── target/
│   └── ...
│
├── docs/
│   ├── deployment.md
│   ├── troubleshooting.md
│   ├── lessons-learned.md
│   ├── publishing-checklist.md
│   └── system-design.md
│
└── README.md
```

The documentation folder is intentionally focused on implementation and
engineering decisions rather than screenshots.

------------------------------------------------------------------------

# Application

The project uses a Maven-based Java application.

The Maven project produces:

``` text
target/java-maven-app-1.1.0-SNAPSHOT.jar
```

The application is then packaged into a Docker image.

------------------------------------------------------------------------

# Dockerfile

The application Dockerfile uses Amazon Corretto 17:

``` dockerfile
FROM amazoncorretto:17-alpine-jdk

EXPOSE 8080

COPY ./target/java-maven-app-*.jar /usr/app/
WORKDIR /usr/app

ENTRYPOINT ["java", "-jar", "java-maven-app-1.1.0-SNAPSHOT.jar"]
```

The important distinction is:

-   `8080` is the application port inside the container.
-   `8081` is the port exposed on the EC2 host.

The resulting mapping is:

``` text
8081:8080
```

------------------------------------------------------------------------

# Jenkins Architecture

Jenkins runs inside a Docker container on the EC2 server.

The Jenkins container is connected to the host Docker daemon through:

``` text
/var/run/docker.sock
```

Conceptually:

``` text
                    EC2 HOST
┌───────────────────────────────────────────────┐
│                                               │
│   ┌───────────────────────────────────────┐   │
│   │          Jenkins Container            │   │
│   │                                       │   │
│   │  Jenkins Pipeline                     │   │
│   │  Maven                                │   │
│   │  Docker CLI                           │   │
│   └───────────────────┬───────────────────┘   │
│                       │                       │
│                       │ Docker Socket         │
│                       v                       │
│              /var/run/docker.sock             │
│                       │                       │
│                       v                       │
│              Host Docker Engine               │
│                       │                       │
│                       v                       │
│              Application Container            │
│                       │                       │
│                 8081:8080                     │
│                       │                       │
└───────────────────────┼───────────────────────┘
                        v
                 Spring Boot App
```

This was an important part of the project because Jenkins needed to
build Docker images even though Jenkins itself was running inside a
container.

------------------------------------------------------------------------

# Jenkins Custom Image

The Jenkins container required the Docker CLI.

The custom Jenkins image was created using:

``` dockerfile
FROM jenkins/jenkins:lts

USER root

RUN apt-get update \
    && apt-get install -y docker.io \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
```

The Jenkins container was then started with access to the host Docker
socket:

``` bash
docker run -d \
  --name jenkins \
  --group-add 109 \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins-docker:lts
```

The Docker group ID on the host was used so that Jenkins could
communicate with Docker without treating the Docker socket as an
ordinary file with unrestricted permissions.

------------------------------------------------------------------------

# Jenkins Pipeline

The Jenkins pipeline is intentionally separated into:

-   `Jenkinsfile` --- pipeline definition
-   `script.groovy` --- reusable pipeline functions

## Jenkinsfile

The pipeline contains the major stages:

``` text
init
  |
  v
build jar
  |
  v
build image
  |
  v
deploy
```

The structure is:

``` groovy
def gv

pipeline {
    agent any

    tools {
        maven 'maven-3.9'
    }

    stages {
        stage("init") {
            steps {
                script {
                    gv = load "script.groovy"
                }
            }
        }

        stage("build jar") {
            steps {
                script {
                    gv.buildJar()
                }
            }
        }

        stage("build image") {
            steps {
                script {
                    gv.buildImage()
                }
            }
        }

        stage("deploy") {
            steps {
                script {
                    gv.deployApp()
                }
            }
        }
    }
}
```

------------------------------------------------------------------------

# Pipeline Stage 1 --- Initialize

The pipeline loads the helper script:

``` groovy
gv = load "script.groovy"
```

This keeps the Jenkinsfile relatively small while moving reusable
functions into a separate Groovy file.

------------------------------------------------------------------------

# Pipeline Stage 2 --- Build the Application

Maven packages the Java application:

``` bash
mvn package
```

This produces the application JAR under:

``` text
target/
```

The build artifact used by the Docker image is:

``` text
java-maven-app-1.1.0-SNAPSHOT.jar
```

------------------------------------------------------------------------

# Pipeline Stage 3 --- Build and Push the Docker Image

The pipeline builds:

``` text
pierrechukason/demo-app.jma-1.1
```

The image is pushed to Docker Hub.

The process is:

``` text
Maven JAR
   |
   v
Docker Build
   |
   v
Docker Image
   |
   v
Docker Hub
```

The Docker Hub repository is:

``` text
pierrechukason/demo-app.jma-1.1
```

Credentials are stored in Jenkins rather than being hard-coded into the
pipeline.

------------------------------------------------------------------------

# Pipeline Stage 4 --- Deploy to EC2

Jenkins connects to the EC2 server using SSH.

The deployment process is:

``` text
Jenkins
   |
   | SSH
   v
EC2
   |
   +--> docker pull
   |
   +--> stop/remove previous container
   |
   +--> start new container
   |
   v
demo-app
```

The application container is started with:

``` bash
docker run -d \
  --name demo-app \
  -p 8081:8080 \
  pierrechukason/demo-app.jma-1.1
```

Therefore:

``` text
EC2 port 8081
       |
       v
Container port 8080
       |
       v
Spring Boot application
```

------------------------------------------------------------------------

# Why Port 8081?

Jenkins is already exposed on:

``` text
EC2:8080
```

The application therefore cannot also bind directly to host port `8080`.

The solution is:

``` text
Jenkins:
8080:8080

Application:
8081:8080
```

This allows both services to run on the same EC2 server.

The application is therefore accessed through:

``` text
http://<EC2-PUBLIC-IP>:8081/
```

------------------------------------------------------------------------

# Jenkins Credentials

The pipeline uses Jenkins-managed credentials.

The project uses:

  Credential ID       Purpose
  ------------------- --------------------------------------
  `docker-hub-repo`   Docker Hub authentication
  `ec2-server-key`    SSH access to EC2
  `GitHub-PAT`        GitHub authentication where required

Credentials are referenced by ID rather than placing passwords or
private keys directly in the repository.

------------------------------------------------------------------------

# AWS EC2

The EC2 instance acts as the deployment server.

The server is responsible for running:

-   Jenkins
-   Docker
-   the deployed application container

The deployment flow is:

``` text
GitHub
   |
   v
Jenkins
   |
   v
Docker Hub
   |
   v
EC2
   |
   v
Docker Container
```

------------------------------------------------------------------------

# AWS Security Group

The EC2 Security Group must allow the required traffic.

The project uses:

    Port Purpose
  ------ -------------
      22 SSH
    8080 Jenkins
    8081 Application

Port exposure should be limited to the networks that actually need
access.

For production environments, SSH and administration ports should not be
left broadly exposed unnecessarily.

------------------------------------------------------------------------

# Real Troubleshooting Experience

One of the most valuable parts of this project was not simply getting
the pipeline to run.

It was understanding why individual stages failed.

Several real issues were encountered during implementation.

------------------------------------------------------------------------

## 1. Jenkins Could Not Run Docker Commands

Jenkins initially did not have access to the Docker CLI.

The solution was to build a custom Jenkins image containing Docker CLI
support.

This changed the architecture from:

``` text
Jenkins Container
      X
Docker
```

to:

``` text
Jenkins Container
      |
      v
Docker CLI
      |
      v
Host Docker Socket
      |
      v
Docker Engine
```

------------------------------------------------------------------------

## 2. Docker Socket Permission Problem

Having the Docker CLI installed was not enough.

The Jenkins process also needed permission to communicate with:

``` text
/var/run/docker.sock
```

The host Docker group ID was therefore passed into the Jenkins
container.

This reinforced an important lesson:

> Installing a CLI does not automatically provide access to the service
> that CLI controls.

------------------------------------------------------------------------

## 3. Docker Hub Credential Problem

The pipeline initially failed because the expected Docker Hub credential
was not available in Jenkins.

The credential was then created with the expected ID:

``` text
docker-hub-repo
```

The pipeline could then authenticate and push the image.

This also reinforced the importance of keeping credential IDs consistent
between Jenkins configuration and pipeline code.

------------------------------------------------------------------------

## 4. Host Port Collision

The first deployment attempted to start the application on:

``` text
8080:8080
```

But Jenkins was already using host port `8080`.

Docker therefore could not bind the application to the same host port.

The deployment was changed to:

``` text
8081:8080
```

This allowed Jenkins and the application to run on the same EC2
instance.

------------------------------------------------------------------------

## 5. Application Container Exited

After the port issue was investigated, the application container was
found to have exited.

The container logs showed:

``` text
Error: Unable to access jarfile java-maven-app-1.0-SNAPSHOT.jar
```

The Maven project was actually producing:

``` text
java-maven-app-1.1.0-SNAPSHOT.jar
```

The Dockerfile expected the wrong filename.

The Dockerfile was corrected to:

``` dockerfile
ENTRYPOINT ["java", "-jar", "java-maven-app-1.1.0-SNAPSHOT.jar"]
```

This was a useful example of why container debugging should begin with
the container logs rather than assuming the deployment command itself is
the problem.

------------------------------------------------------------------------

# Troubleshooting Method Used

The project also established a repeatable troubleshooting approach.

When a deployment fails:

``` text
1. Check pipeline stage
        |
        v
2. Read Jenkins console output
        |
        v
3. Identify the exact failing command
        |
        v
4. Reproduce/check the command manually
        |
        v
5. Inspect Docker/container state
        |
        v
6. Check container logs
        |
        v
7. Check ports/processes
        |
        v
8. Verify AWS networking/security rules
        |
        v
9. Apply the smallest necessary fix
        |
        v
10. Run the pipeline again
```

Useful commands include:

``` bash
docker ps
docker ps -a
docker logs demo-app
docker inspect demo-app
docker images
docker pull <image>
docker run ...
```

For host port investigation:

``` bash
sudo ss -ltnp | grep -E ':8081|:8080'
```

For application verification:

``` bash
curl -v http://localhost:8081
```

This process is more useful than simply restarting containers repeatedly
because each command provides evidence about a different part of the
system.

------------------------------------------------------------------------

# System Design

The detailed architecture and engineering decisions for this project are
documented separately in:

``` text
docs/system-design.md
```

That document covers:

-   component relationships
-   Jenkins container architecture
-   Docker socket access
-   CI/CD flow
-   registry flow
-   EC2 deployment
-   network ports
-   credentials
-   security considerations
-   architectural trade-offs
-   future improvements

------------------------------------------------------------------------

# Deployment Documentation

The practical deployment procedure is documented in:

``` text
docs/deployment.md
```

It focuses on the actual steps required to reproduce the environment
rather than screenshots.

------------------------------------------------------------------------

# Troubleshooting Documentation

The troubleshooting record is documented in:

``` text
docs/troubleshooting.md
```

It captures the actual problems encountered while building the project,
the evidence used to diagnose them, and the resulting fixes.

This is intentionally based on real implementation experience rather
than invented examples.

------------------------------------------------------------------------

# Engineering Lessons

The project reinforced several engineering principles.

## 1. A pipeline is a system, not just a Jenkinsfile

A working CI/CD pipeline depends on several connected systems:

``` text
Source Control
      +
Build Tool
      +
Jenkins
      +
Docker
      +
Registry
      +
SSH
      +
Cloud Infrastructure
      +
Application
```

A failure in any one of these can stop the deployment.

------------------------------------------------------------------------

## 2. Logs are evidence

Instead of guessing:

``` text
"What might be wrong?"
```

the better question is:

``` text
"What evidence do I have?"
```

For example:

``` text
docker logs demo-app
```

immediately revealed the incorrect JAR filename.

------------------------------------------------------------------------

## 3. Host ports and container ports are different

The project made the distinction between:

``` text
host port
```

and:

``` text
container port
```

explicit.

The application listens on:

``` text
8080
```

inside the container.

The EC2 host exposes:

``` text
8081
```

to the outside world.

The mapping is:

``` text
8081:8080
```

------------------------------------------------------------------------

## 4. Containers do not remove the need for debugging

A container can be:

``` text
created
```

but still:

``` text
exited
```

Therefore:

``` bash
docker ps
```

alone is not always enough.

For failed containers:

``` bash
docker ps -a
docker logs <container>
```

are essential.

------------------------------------------------------------------------

## 5. Infrastructure configuration and application configuration interact

The port collision demonstrated that application deployment cannot be
considered separately from the infrastructure already running on the
server.

Jenkins occupied port `8080`.

The application therefore needed a different host port.

This is one example of why deployment architecture matters.

------------------------------------------------------------------------

# Security Considerations

This project is primarily a learning environment.

Some design choices are intentionally simple so the complete CI/CD flow
can be understood.

For a production environment, additional controls would be appropriate.

Examples include:

-   restricting SSH access
-   using least-privilege IAM
-   protecting Jenkins
-   avoiding unnecessary public ports
-   using HTTPS/TLS
-   using a managed container registry
-   scanning container images
-   rotating credentials
-   using secrets management
-   separating CI and production infrastructure
-   using dedicated deployment identities
-   implementing stronger network segmentation
-   adding monitoring and alerting

The Docker socket approach also requires careful consideration because
access to the host Docker daemon can provide significant control over
the host.

------------------------------------------------------------------------

# Current Implementation

The implementation practiced in this repository is:

``` text
GitHub
   |
   v
Jenkins Multibranch Pipeline
   |
   v
Maven
   |
   v
Docker Build
   |
   v
Docker Hub
   |
   v
SSH
   |
   v
Amazon EC2
   |
   v
Docker Container
   |
   v
Spring Boot Application
```

### Current image

``` text
pierrechukason/demo-app.jma-1.1
```

### Current application container

``` text
demo-app
```

### Current application mapping

``` text
8081:8080
```

### Jenkins

``` text
8080
```

------------------------------------------------------------------------

# Progressive Improvements

The current implementation provides the complete learning path for the
project.

There are also several natural next steps.

These are improvements to the architecture rather than claims about the
current implementation.

## Next Stage --- Docker Compose

Docker Compose could be introduced to manage multiple services and make
local or server-side service configuration easier.

Possible direction:

``` text
Docker Compose
   |
   +--> Jenkins
   |
   +--> Application
   |
   +--> Supporting Services
```

------------------------------------------------------------------------

## Next Stage --- Amazon ECR

Docker Hub can later be replaced or supplemented by Amazon Elastic
Container Registry.

Possible flow:

``` text
GitHub
   |
   v
Jenkins
   |
   v
Docker Build
   |
   v
Amazon ECR
   |
   v
Amazon EC2 / ECS / EKS
```

This would also connect naturally with the next phase of AWS and
Kubernetes learning.

------------------------------------------------------------------------

## Next Stage --- Dynamic Image Versioning

The current project uses a fixed image tag:

``` text
pierrechukason/demo-app.jma-1.1
```

A future implementation could generate tags from:

-   Git commit SHA
-   build number
-   application version
-   release version

For example:

``` text
demo-app:<git-commit>
```

This would improve traceability between a deployed container and the
source code that produced it.

------------------------------------------------------------------------

# Multi-Branch Pipeline

The Jenkins job is configured as a Multibranch Pipeline.

The purpose is to allow Jenkins to discover branches and execute the
pipeline according to the Jenkinsfile associated with each branch.

Conceptually:

``` text
GitHub
│
├── main
│     └── Jenkinsfile
│
├── development
│     └── Jenkinsfile
│
└── feature/*
      └── Jenkinsfile
```

This becomes increasingly useful as development workflows become more
complex.

------------------------------------------------------------------------

# Verification Checklist

After a successful deployment, the following can be checked.

### Source

``` bash
git status
git log
```

### Jenkins

Confirm that:

``` text
init
build jar
build image
deploy
```

complete successfully.

### Docker

``` bash
docker ps
```

Confirm that:

``` text
demo-app
```

is running.

### Container logs

``` bash
docker logs demo-app
```

Confirm that the application starts successfully.

### Port mapping

``` bash
docker port demo-app
```

Expected mapping:

``` text
8080/tcp -> 0.0.0.0:8081
```

### Application

From the EC2 server:

``` bash
curl -v http://localhost:8081
```

From an allowed external network:

``` text
http://<EC2-PUBLIC-IP>:8081/
```

------------------------------------------------------------------------

# Documentation Structure

The repository documentation is intentionally organized around the
engineering work:

``` text
docs/
├── deployment.md
├── troubleshooting.md
├── lessons-learned.md
├── publishing-checklist.md
└── system-design.md
```

### `deployment.md`

How the environment was configured and how deployment works.

### `troubleshooting.md`

Actual failures, diagnostic evidence, root causes, and fixes.

### `lessons-learned.md`

Engineering lessons extracted from the implementation.

### `publishing-checklist.md`

Checklist for preparing the repository and project for public
presentation.

### `system-design.md`

Detailed architecture, component relationships, design decisions,
trade-offs, and future evolution.

------------------------------------------------------------------------

# Project Status

## Completed

-   [x] Java/Maven application
-   [x] Git/GitHub integration
-   [x] Jenkins Multibranch Pipeline
-   [x] Maven build automation
-   [x] Docker image build
-   [x] Docker Hub publishing
-   [x] Jenkins running in Docker
-   [x] Jenkins Docker CLI configuration
-   [x] Docker socket integration
-   [x] Docker permissions configuration
-   [x] SSH deployment to EC2
-   [x] Docker container deployment
-   [x] Application port mapping
-   [x] AWS Security Group configuration
-   [x] End-to-end pipeline practice
-   [x] Troubleshooting and recovery
-   [x] Project documentation

## Future Improvements

-   [ ] Docker Compose
-   [ ] Amazon ECR
-   [ ] Dynamic image tagging
-   [ ] Image scanning
-   [ ] HTTPS/TLS
-   [ ] Stronger secrets management
-   [ ] More granular IAM permissions
-   [ ] Monitoring and alerting
-   [ ] More production-oriented deployment architecture

------------------------------------------------------------------------

# What This Project Demonstrates

This repository is more than a Jenkins configuration.

It demonstrates practical understanding of the path from:

``` text
Source Code
     |
     v
Build
     |
     v
Artifact
     |
     v
Container Image
     |
     v
Registry
     |
     v
Remote Infrastructure
     |
     v
Running Application
```

It also demonstrates the debugging required when those layers do not
initially work together.

The most important outcome of the project was not simply getting a green
Jenkins pipeline.

It was learning how to move from:

``` text
failure
   |
   v
evidence
   |
   v
diagnosis
   |
   v
fix
   |
   v
verification
```

That troubleshooting loop is an important part of practical DevOps work.

------------------------------------------------------------------------

# Related Documentation

-   `docs/system-design.md`
-   `docs/deployment.md`
-   `docs/troubleshooting.md`
-   `docs/lessons-learned.md`
-   `docs/publishing-checklist.md`

------------------------------------------------------------------------

# Author

**Chukwuemeka Peter Eze**

DevOps / Cloud Engineering Learning Portfolio

GitHub:

``` text
https://github.com/Chukwuemeka-Peter-Eze/aws-jenkins-ec2-cicd-pipeline
```

------------------------------------------------------------------------

# Final Note

This project was built as a hands-on learning implementation.

The goal was not to reproduce a production platform in one repository.

The goal was to understand the individual components, connect them into
a working delivery path, encounter real integration problems,
investigate those problems, and document the engineering decisions
behind the final implementation.

The next step is to continue evolving the architecture toward more
production-oriented AWS, container, Kubernetes, security, observability,
and automation patterns.