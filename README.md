# AWS Jenkins CI/CD Pipeline

A hands-on CI/CD project that automates the journey from source code to a running Dockerized application on Amazon EC2.

This repository started as a manual EC2 deployment exercise and evolved into a Jenkins-driven delivery workflow. The project is intentionally built in stages so that each step exposes a different part of the delivery chain: source control, build automation, containerization, image publishing, remote deployment, networking, and operational troubleshooting.

The current implementation uses **Jenkins, Maven, Docker, Docker Hub, SSH, and Amazon EC2**. The repository also documents the planned progression toward **Docker Compose, Amazon ECR, and dynamic image versioning**.

---

## What This Project Demonstrates

The core workflow currently implemented is:

```text
Developer
    |
    | Git push
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
                           | SSH
                           v
                     Amazon EC2
                           |
                           v
                    Docker Container
                           |
                           v
                  Application :8081
```

The application is packaged as:

```text
java-maven-app-1.1.0-SNAPSHOT.jar
```

The application listens on port `8080` inside the container and is published on port `8081` on the EC2 host:

```text
EC2 :8081  ->  Container :8080
```

Application endpoint:

```text
http://<EC2-PUBLIC-IP>:8081/
```

---

# Project Goals

The project is designed to build practical understanding of how a CI/CD system moves an application through several engineering boundaries:

1. Source control
2. Automated build
3. Artifact creation
4. Container image creation
5. Image publishing
6. Remote deployment
7. Runtime verification
8. Infrastructure and application troubleshooting

The objective is not simply to make Jenkins execute commands. The objective is to understand what each component contributes to the delivery process and what evidence is required when something fails.

---

# Technology Stack

| Technology | Role |
|---|---|
| Git / GitHub | Source control and pipeline trigger |
| Jenkins | CI/CD automation |
| Jenkins Multibranch Pipeline | Branch-aware pipeline execution |
| Maven | Java build and packaging |
| Spring Boot | Application framework |
| Amazon Corretto 17 | Java runtime inside the application container |
| Docker | Application containerization |
| Docker Hub | Container image registry in the current implementation |
| SSH | Jenkins-to-EC2 remote access |
| Amazon EC2 | Application deployment environment |
| AWS Security Groups | Network access control |
| Docker Compose | Planned/progressive deployment improvement |
| Amazon ECR | Planned/progressive registry improvement |

---

# Repository Structure

```text
.
├── Dockerfile
├── Jenkinsfile
├── script.groovy
├── pom.xml
├── README.md
├── docs/
│   ├── deployment.md
│   ├── lessons-learned.md
│   ├── publishing-checklist.md
│   ├── screenshots.md
│   └── troubleshooting.md
└── src/
```

The `docs/` directory contains supporting operational documentation rather than duplicating the README.

---

# CI/CD Pipeline

The current Jenkins pipeline is deliberately separated into a small number of clear stages.

```text
Source Checkout
      |
      v
Initialization
      |
      v
Build JAR
      |
      v
Build Docker Image
      |
      v
Push Image
      |
      v
SSH to EC2
      |
      v
Pull Image
      |
      v
Replace Running Container
      |
      v
Expose Application on :8081
```

## Pipeline responsibilities

### 1. Initialization

Jenkins loads the shared Groovy deployment/build functions from:

```text
script.groovy
```

The Jenkinsfile remains responsible for pipeline structure while `script.groovy` contains reusable implementation logic.

### 2. Build

Maven packages the Spring Boot application:

```bash
mvn package
```

The resulting artifact is:

```text
target/java-maven-app-1.1.0-SNAPSHOT.jar
```

### 3. Docker Build

The application is packaged into a Docker image using:

```text
amazoncorretto:17-alpine-jdk
```

The Dockerfile copies the Maven-generated JAR into the image and starts it with Java.

### 4. Image Publishing

The current implementation publishes the image to Docker Hub:

```text
pierrechukason/demo-app.jma-1.1
```

Jenkins authenticates using a Jenkins-managed Docker Hub credential rather than storing registry credentials directly in the repository.

### 5. Deployment

Jenkins uses the configured EC2 SSH credential to connect to the deployment host.

The deployment process:

```text
Pull latest image
      |
      v
Remove previous container
      |
      v
Start new container
```

The application container is published as:

```text
8081:8080
```

This means:

```text
EC2 host port 8081
        |
        v
Docker container port 8080
```

---

# Why Port 8081?

Jenkins and the application are deployed on the same EC2 instance.

Jenkins itself uses host port `8080`, so the application cannot also bind to host port `8080`.

Instead, the application uses:

```text
Host:8081 -> Container:8080
```

This is a useful example of the difference between an application's internal listening port and the port exposed by the deployment host.

The Spring Boot application continues to listen on its normal container port:

```text
8080
```

Only the EC2-facing port changes:

```text
8081
```

---

# Dockerfile

The application Dockerfile is intentionally small:

```dockerfile
FROM amazoncorretto:17-alpine-jdk

EXPOSE 8080

COPY ./target/java-maven-app-*.jar /usr/app/
WORKDIR /usr/app

ENTRYPOINT ["java", "-jar", "java-maven-app-1.1.0-SNAPSHOT.jar"]
```

The important relationship is between Maven's output and the Docker entrypoint.

Maven produces:

```text
java-maven-app-1.1.0-SNAPSHOT.jar
```

and the Docker image starts exactly that artifact.

A previous deployment failure demonstrated why this matters: the image attempted to start `java-maven-app-1.0-SNAPSHOT.jar` while Maven was producing version `1.1.0`. The container therefore exited immediately with:

```text
Error: Unable to access jarfile java-maven-app-1.0-SNAPSHOT.jar
```

The failure was not a port or Security Group problem. The application process never started.

---

# Jenkins and Docker

Jenkins itself runs inside Docker on the EC2 host.

The Jenkins container has access to the host Docker daemon through:

```text
/var/run/docker.sock
```

The Jenkins container was also configured with access to the host Docker group so that Jenkins can execute Docker commands.

This creates the following relationship:

```text
EC2 Host
│
├── Docker Engine
│
├── Jenkins Container
│     │
│     └── Docker CLI
│             |
│             └── /var/run/docker.sock
│
└── Application Container
```

This setup is useful for learning because it makes the relationship between Jenkins, Docker CLI, the Docker daemon, and containers visible.

It also introduces an important operational consideration: access to the Docker socket is highly privileged and should be treated accordingly in production environments.

---

# Jenkins Credentials

The pipeline uses Jenkins-managed credentials for infrastructure and registry access.

Current credential roles include:

| Credential ID | Purpose |
|---|---|
| `GitHub-PAT` | GitHub repository access |
| `docker-hub-repo` | Docker Hub authentication |
| `ec2-server-key` | SSH access to EC2 |

Credentials are referenced by ID rather than embedded directly into pipeline source code.

Example:

```groovy
withCredentials([
    usernamePassword(
        credentialsId: 'docker-hub-repo',
        passwordVariable: 'PASS',
        usernameVariable: 'USER'
    )
])
```

The repository should never contain the actual values of these credentials.

---

# Amazon EC2 Deployment

The EC2 instance acts as the deployment host.

The current deployment model is:

```text
Jenkins
   |
   | SSH
   v
Amazon EC2
   |
   | Docker Pull
   v
Docker Hub
   |
   v
Application Image
   |
   v
demo-app container
```

The application container is named:

```text
demo-app
```

The expected runtime mapping is:

```text
0.0.0.0:8081 -> 8080/tcp
```

---

# AWS Security Group

The EC2 Security Group must allow inbound traffic to the application host port.

For the current deployment, that means TCP port:

```text
8081
```

Opening a port in the Security Group does not itself make an application available.

The complete chain must exist:

```text
Internet
   |
   v
AWS Security Group :8081
   |
   v
EC2 host :8081
   |
   v
Docker port mapping
   |
   v
Container :8080
   |
   v
Spring Boot application
```

This distinction became important during troubleshooting because a permitted Security Group port does not compensate for a stopped container or an application process that failed to start.

---

# Verification

A deployment is not considered complete merely because Jenkins reports that the Docker command executed.

Verification should happen at several layers.

## 1. Jenkins

Confirm that the pipeline completed successfully.

## 2. Docker image

Confirm that the expected image exists locally or in Docker Hub.

```bash
docker images
```

## 3. Container

Confirm that the application is running:

```bash
docker ps
```

The expected port mapping should resemble:

```text
0.0.0.0:8081->8080/tcp
```

## 4. Container logs

Inspect application startup:

```bash
docker logs demo-app
```

A container that immediately exits should be investigated before testing the public endpoint.

## 5. Port listening

On EC2:

```bash
sudo ss -ltnp | grep 8081
```

## 6. Local application test

From the EC2 instance:

```bash
curl -v http://localhost:8081
```

This separates application/container problems from AWS networking problems.

## 7. External application test

From a browser:

```text
http://<EC2-PUBLIC-IP>:8081/
```

If the local `curl` works but the browser cannot connect, investigate the Security Group, network path, or public addressing.

---

# Troubleshooting Method

The project deliberately uses a layered troubleshooting approach.

When the application is unavailable, do not immediately change AWS networking.

Work from the inside out:

```text
1. Is the container running?
          |
          v
2. Does the application process start?
          |
          v
3. Is the container port correct?
          |
          v
4. Is the host port published?
          |
          v
5. Is EC2 listening on that port?
          |
          v
6. Does localhost work?
          |
          v
7. Does the Security Group allow the port?
          |
          v
8. Does the public endpoint work?
```

Useful commands include:

```bash
docker ps -a
docker logs demo-app
docker inspect demo-app
sudo ss -ltnp
curl -v http://localhost:8081
```

This approach prevents infrastructure changes from masking application-level failures.

---

# Example Failure: JAR Filename Mismatch

One of the practical failures encountered during development was a mismatch between the Maven artifact version and the Docker entrypoint.

Maven produced:

```text
java-maven-app-1.1.0-SNAPSHOT.jar
```

while the Dockerfile attempted to start:

```text
java-maven-app-1.0-SNAPSHOT.jar
```

The result was:

```text
Error: Unable to access jarfile java-maven-app-1.0-SNAPSHOT.jar
```

Docker itself was functioning. The image was created. The container could be started.

The failure occurred at the application startup layer.

The fix was to make the Docker entrypoint match the actual artifact produced by Maven.

This illustrates an important CI/CD principle:

> A successful image build does not necessarily mean a successful application deployment.

The artifact must also be compatible with the runtime command used by the container.

---

# Example Failure: Docker Port Collision

Another failure occurred when the deployment attempted:

```bash
docker run -d --name demo-app -p 8080:8080 ...
```

The EC2 host was already using port `8080` for Jenkins.

Docker therefore reported that the port was already allocated.

The solution was to separate the host-facing ports:

```text
Jenkins     -> EC2 :8080
Application -> EC2 :8081
```

while keeping the application listening on:

```text
Container :8080
```

This distinction between host and container ports is fundamental when multiple services share one host.

---

# Project Evolution

This repository is organized as a progression rather than a single final architecture.

## Part I — Jenkins + SSH + EC2 + Docker

The first stage establishes the basic automated deployment path:

```text
GitHub
  |
  v
Jenkins
  |
  | Build
  v
Docker Image
  |
  | Push
  v
Docker Hub
  |
  | SSH
  v
EC2
  |
  v
Docker Container
```

This is the current foundation of the project.

---

## Part II — Docker Compose + ECR

The next stage introduces:

- Docker Compose
- Amazon ECR
- More structured deployment configuration
- Separation of image storage from the current Docker Hub workflow
- Deployment logic extracted into reusable scripts

Conceptually:

```text
GitHub
  |
  v
Jenkins
  |
  +--> Build
  |
  +--> Test
  |
  +--> Docker Build
  |
  +--> Push to ECR
          |
          v
        EC2
          |
          v
   Docker Compose
          |
          v
     Application
```

This stage should only be described as implemented once the repository actually contains and uses the corresponding Compose and ECR configuration.

---

## Part III — Dynamic Image Versioning

The final progression introduces build-specific image versions.

Instead of depending on one mutable tag, the pipeline can associate an image with a Jenkins build or application version.

For example:

```text
Build 101
    |
    v
demo-app:101
```

or:

```text
demo-app:<application-version>
```

This makes deployments easier to identify and creates a foundation for controlled rollbacks.

---

# Why Dynamic Versioning Matters

A static image tag makes it difficult to answer a simple operational question:

> Which exact build is running in production?

Versioned images make that relationship more explicit:

```text
Source Commit
      |
      v
Jenkins Build
      |
      v
Application Version
      |
      v
Docker Image Tag
      |
      v
Deployment
```

That relationship becomes increasingly important as deployment frequency increases.

---

# Multi-Branch Pipeline

The repository uses a Jenkins Multibranch Pipeline.

Instead of configuring each branch as a completely separate Jenkins job, Jenkins can discover branches containing a pipeline definition.

Conceptually:

```text
GitHub Repository
       |
       +── main
       |     └── Jenkinsfile
       |
       +── feature/*
       |     └── Jenkinsfile
       |
       └── other branches
             └── Jenkinsfile
```

This provides a foundation for branch-based CI workflows and makes the Jenkins configuration closer to the repository structure.

---

# Security Considerations

CI/CD systems connect source code, credentials, build infrastructure, registries, and deployment environments.

Important practices demonstrated or reinforced by this project include:

- Store secrets in Jenkins Credentials rather than source code.
- Do not commit passwords, access tokens, or private keys.
- Use dedicated credentials for different systems.
- Keep EC2 Security Group rules as restrictive as practical.
- Avoid unnecessary administrative permissions.
- Treat access to the Docker socket as privileged.
- Use appropriate AWS IAM permissions when AWS services are introduced.
- Do not expose registry credentials in build logs.
- Separate application configuration from secret values.
- Verify exactly which credentials each pipeline stage requires.

---

# Engineering Lessons

## 1. A pipeline is a chain of dependencies

A successful deployment depends on several independent components working together:

```text
Git
 ↓
Jenkins
 ↓
Maven
 ↓
JAR
 ↓
Docker
 ↓
Registry
 ↓
SSH
 ↓
EC2
 ↓
Container
 ↓
Application
```

A failure anywhere in the chain can appear as an application availability problem.

---

## 2. Read the failure at the layer where it occurs

For example:

```text
"Unable to access jarfile"
```

points toward the application/container image.

It does not immediately point toward AWS networking.

Likewise:

```text
"port is already allocated"
```

points toward host-level port usage.

The error message determines the next layer to inspect.

---

## 3. Build success and runtime success are different

Docker can successfully build an image that later fails immediately when the container starts.

Therefore:

```text
docker build succeeded
```

does not prove:

```text
application is running
```

Runtime verification is part of deployment.

---

## 4. Host ports and container ports solve different problems

This project uses:

```text
8081:8080
```

which means:

```text
HOST:CONTAINER
```

The application still runs on port `8080` inside the container.

Port `8081` exists so the EC2 host can expose the application without conflicting with Jenkins on host port `8080`.

---

## 5. Automation should reduce manual repetition, not hide system behavior

The purpose of Jenkins is not simply to eliminate typing.

A useful pipeline makes the delivery process repeatable while still making each stage understandable:

```text
Build
→ Package
→ Containerize
→ Publish
→ Deploy
→ Verify
```

That understanding becomes important when the automation fails.

---

# Current Project Status

## Current implementation

The repository currently demonstrates:

- GitHub source control
- Jenkins Multibranch Pipeline
- Maven application packaging
- Spring Boot application
- Docker image creation
- Docker Hub image publishing
- Jenkins-managed credentials
- SSH-based EC2 deployment
- Docker-based application deployment
- EC2 host port `8081` mapped to container port `8080`
- Layered deployment troubleshooting

## Progressive improvements documented in this repository

The project progression also introduces:

- Docker Compose
- Amazon ECR
- Dynamic image versioning
- More structured deployment configuration

These should be considered separate implementation stages and should only be marked as completed when their corresponding configuration is actually active in the repository.

---

# Current Deployment Contract

For the current application deployment, the important values are:

```text
Application artifact:
java-maven-app-1.1.0-SNAPSHOT.jar

Docker image:
pierrechukason/demo-app.jma-1.1

Application container:
demo-app

Container port:
8080

EC2 host port:
8081

Jenkins host port:
8080
```

The resulting traffic path is:

```text
Browser
   |
   | :8081
   v
EC2
   |
   | Docker port mapping
   v
demo-app :8080
   |
   v
Spring Boot
```

---

# Verification Checklist

Before considering a deployment complete:

- [ ] Git commit pushed
- [ ] Jenkins discovered the change
- [ ] Source checkout succeeded
- [ ] Maven build succeeded
- [ ] Expected JAR was created
- [ ] Docker image built successfully
- [ ] Image was pushed to the registry
- [ ] Jenkins successfully connected to EC2
- [ ] EC2 pulled the expected image
- [ ] Previous application container was removed/replaced
- [ ] New `demo-app` container is running
- [ ] Port mapping shows `8081->8080`
- [ ] Application logs show successful startup
- [ ] `curl http://localhost:8081` succeeds
- [ ] EC2 Security Group permits TCP `8081`
- [ ] Public endpoint is reachable

---

# Related Documentation

Detailed operational information is kept in the `docs/` directory.

Recommended documentation responsibilities:

| Document | Purpose |
|---|---|
| `deployment.md` | How the application is deployed and verified |
| `troubleshooting.md` | Failure symptoms, evidence, diagnosis, and fixes |
| `lessons-learned.md` | Engineering lessons and decisions from the project |
| `screenshots.md` | Evidence and explanation of important screenshots |
| `publishing-checklist.md` | Repository/portfolio publishing checklist |

The README explains the system as a whole. The documents above should contain details that would otherwise make the README unnecessarily long.

---

# What This Project Taught Me

The most useful part of this project has not been learning individual commands.

It has been learning to trace a deployment across boundaries.

A request that looks like:

```text
"The application is not opening"
```

can actually originate from:

```text
Source code
    ↓
Build artifact
    ↓
Dockerfile
    ↓
Docker image
    ↓
Container startup
    ↓
Port mapping
    ↓
EC2
    ↓
Security Group
    ↓
Public network
```

The practical skill is being able to identify which layer is actually failing before changing configuration elsewhere.

That is the foundation this project is intended to demonstrate.

---

# Project Outcome

This repository represents a progression from manually deploying a containerized application on EC2 toward a repeatable CI/CD workflow.

The current working foundation is:

```text
GitHub
   ↓
Jenkins Multibranch Pipeline
   ↓
Maven
   ↓
Docker
   ↓
Docker Hub
   ↓
SSH
   ↓
Amazon EC2
   ↓
Docker
   ↓
Spring Boot Application
```

The next stages extend the same delivery chain with:

```text
Docker Compose
+
Amazon ECR
+
Dynamic Image Versioning
```

The overall objective is to build a deployment process that is repeatable, observable, and easier to troubleshoot as the system becomes more sophisticated.