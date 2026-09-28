# System Design

This document explains the architecture of `aws-jenkins-ec2-cicd-pipeline`
in more depth than the README: the components involved, why each design
decision was made the way it was, what trade-offs were accepted, and how
the system could evolve. Where the README shows *what* exists, this
document focuses on *why*.

---

## 1. Component Overview

| Component | Responsibility |
|---|---|
| GitHub | Source of truth for application code and pipeline definition |
| Jenkins (containerized) | Orchestrates build, image creation, and deployment |
| Maven | Compiles and packages the Java application into a JAR |
| Docker Engine (host) | Builds and runs all containers, including Jenkins itself |
| Docker Hub | Stores and distributes the built application image |
| SSH | Transport used by Jenkins to trigger deployment actions on EC2 |
| Amazon EC2 | Single host running Jenkins, Docker, and the deployed application |
| AWS Security Group | Perimeter network access control for the EC2 host |

---

## 2. End-to-End Data Flow

```text
Developer
   |
   v
GitHub Repository  --(webhook / poll)-->  Jenkins Multibranch Pipeline
                                                 |
                              +------------------+------------------+
                              |                                     |
                              v                                     v
                        Maven Build                          Docker Build
                              |                                     |
                              v                                     v
                        JAR Artifact  ------------------->   Docker Image
                                                                     |
                                                                     v
                                                               Docker Hub
                                                                     |
                                                                     v
                                                         SSH to EC2 (self)
                                                                     |
                                                                     v
                                                          docker pull + run
                                                                     |
                                                                     v
                                                          demo-app container
                                                                     |
                                                                     v
                                                        Spring Boot Application
```

---

## 3. Jenkins-in-Docker Architecture

Jenkins runs as a container on the EC2 host rather than as a bare-metal
install. To build Docker images from *within* that container, Jenkins needs
a path to the host's Docker engine:

```text
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
│                        │                      │
│                        │ Docker Socket        │
│                        v                      │
│               /var/run/docker.sock            │
│                        │                      │
│                        v                      │
│               Host Docker Engine              │
│                        │                      │
│                        v                      │
│               Application Container           │
│                        │                      │
│                  8081:8080                    │
│                        │                      │
└────────────────────────┼──────────────────────┘
                          v
                   Spring Boot App
```

**Design decision: mount the host Docker socket, rather than running
Docker-in-Docker (DinD).**

- *Why chosen:* Simpler to set up, avoids the storage and networking quirks
  of a nested Docker daemon, and images built this way are immediately
  visible to the host engine without an extra push/pull round trip.
- *Trade-off accepted:* Any process with access to `/var/run/docker.sock`
  has effective root-equivalent control over the host, since it can launch
  privileged containers. This is a reasonable trade-off for a
  single-tenant learning environment, but would need to be reconsidered
  (e.g. dedicated build agents, DinD with proper isolation, or a managed
  build service) before running arbitrary or third-party pipeline code.

---

## 4. Port Architecture

```text
EC2 Host
  |
  |-- 8080 --> Jenkins Container --> Jenkins UI
  |
  |-- 8081 --> demo-app Container --> :8080 (Spring Boot)
```

**Design decision: application listens on container port `8080`, mapped to
host port `8081`.**

- *Why chosen:* Jenkins already occupies host port `8080` on the same
  instance. Rather than changing Jenkins's well-known default port,
  the application's host-side mapping was changed instead, since the
  application's *internal* port is easy to keep at its framework default
  (`8080` for Spring Boot) while remapping only the externally-visible
  side.
- *Trade-off accepted:* Anyone deploying a second application to this same
  host will need to track which host ports are already in use — there's no
  automated port allocation. This doesn't scale past a small, manually
  tracked number of services on one box (see *Future Improvements*).

---

## 5. Registry Choice: Docker Hub

**Design decision: use Docker Hub rather than Amazon ECR.**

- *Why chosen:* Docker Hub requires no additional AWS IAM configuration to
  get a first working pipeline — useful when the learning goal is the CI/CD
  mechanics themselves, not AWS registry permissions.
- *Trade-off accepted:* No native integration with EC2 IAM roles, so
  authentication depends on a Jenkins-stored Docker Hub credential
  (`docker-hub-repo`) rather than an instance role. This is acceptable at
  small scale; ECR with an attached IAM role removes a credential to manage
  and rotate, and is the natural next step for anyone consolidating deeper
  into the AWS ecosystem (see *Future Improvements* and the companion
  `aws-ecr-docker-registry` repository).

---

## 6. Deployment Transport: SSH

**Design decision: deploy via a direct SSH connection from Jenkins to the
target host, rather than a dedicated deployment tool (e.g. AWS CodeDeploy,
Ansible).**

- *Why chosen:* Minimal moving parts, transparent (every deploy command is
  visible in the pipeline), and sufficient for a single deployment target.
- *Trade-off accepted:* No built-in rollback, canary, or blue/green
  capability, and no orchestrated multi-host deployment. Also requires
  managing an SSH keypair as a long-lived credential. This is a reasonable
  starting point; CodeDeploy or a container orchestrator (ECS/EKS) becomes
  worth the added complexity once there's more than one deployment target
  or a need for zero-downtime deploys.

---

## 7. Credentials Model

| Credential ID | Where used | Why this shape |
|---|---|---|
| `docker-hub-repo` | `deploy` stage, image push | Username/password keeps registry auth separate from infra auth |
| `ec2-server-key` | `deploy` stage, SSH transport | Dedicated key scoped only to deployment, not a personal admin key |
| `GitHub-PAT` | Source checkout / webhook config | Avoids storing GitHub password anywhere |

**Design decision: reference all credentials by Jenkins credential ID
rather than embedding any value in pipeline code.**

- *Why chosen:* Keeps secrets out of source control entirely — the
  `Jenkinsfile` is safe to make public even though it defines exactly how
  authentication happens.
- *Trade-off accepted:* Creates an implicit dependency between the pipeline
  code and Jenkins's credential store that isn't visible from the code
  alone (this caused the failure documented in `troubleshooting.md` issue
  #3). Mitigated by documenting every expected credential ID in
  `deployment.md`.

---

## 8. Security Considerations

This project is a learning environment, and several choices were made for
clarity over production hardening. Documented explicitly so the gap is a
conscious one, not an oversight:

- **Docker socket exposure** — as noted in §3, mounting the host socket is
  a significant privilege grant. Acceptable here; would need isolation in
  a multi-tenant or production CI environment.
- **SSH exposed on the host** — currently scoped by Security Group, but a
  long-lived key-based deployment credential is still a standing attack
  surface. AWS Systems Manager Run Command (no open port 22) is a stronger
  alternative worth adopting later.
- **Jenkins and application co-located** — a compromise of one increases
  blast radius on the other, since they share the same host and Docker
  engine.
- **No image scanning** — images are pushed and deployed without a
  vulnerability scan step in the pipeline.
- **No TLS** — the application is currently served over plain HTTP on
  `8081`.
- **Broad Security Group during development** — should be reviewed and
  tightened to only the networks that actually need access before treating
  the environment as anything beyond a personal learning sandbox.

---

## 9. Future Improvements

### Docker Compose
Introduce Compose to manage Jenkins, the application, and any supporting
services (e.g. a reverse proxy) as a single declarative unit rather than
individual `docker run` commands:

```text
Docker Compose
   |
   +--> Jenkins
   |
   +--> Application
   |
   +--> Supporting Services
```

### Amazon ECR
Replace or supplement Docker Hub with a private ECR repository, paired with
an EC2 instance IAM role for pull authentication — removing a stored
credential and integrating more naturally with the rest of the AWS
portfolio (`aws-ecr-docker-registry`):

```text
GitHub -> Jenkins -> Docker Build -> Amazon ECR -> EC2 / ECS / EKS
```

### Dynamic Image Versioning
Replace the fixed tag `pierrechukason/demo-app.jma-1.1` with tags derived
from the Git commit SHA or Jenkins build number (e.g. `demo-app:<git-sha>`)
so every deployed container can be traced back to the exact commit that
produced it, and rollback to a known-good build becomes a simple tag
reference rather than a manual rebuild.

### Additional hardening (longer-term)
- Move deployment transport from SSH to AWS SSM Run Command
- Add an image vulnerability scan stage before push
- Separate Jenkins and the application onto different hosts, or move the
  application to ECS/EKS entirely
- Add TLS termination in front of the application
- Introduce a secrets manager (AWS Secrets Manager / Parameter Store)
  instead of Jenkins-only credential storage
- Add monitoring/alerting on both the Jenkins host and the application
  container

---

## 10. Why This Design Was Appropriate for the Project's Goal

The goal of this repository was to understand how source control, build
tooling, containerization, a registry, an orchestrator, and cloud
infrastructure fit together end to end — and to practice the debugging that
happens when they don't, at first, fit together cleanly. Every simplification
documented above (shared host, Docker Hub over ECR, SSH over a dedicated
deployment tool) was chosen to keep the number of new concepts introduced
at once manageable, while still exercising a genuinely complete,
production-shaped pipeline rather than a toy example. The *Future
Improvements* section is the deliberate path toward closing each gap once
the fundamentals are solid.