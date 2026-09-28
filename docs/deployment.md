# Deployment Guide

This document describes how the `aws-jenkins-ec2-cicd-pipeline` environment
was configured and how a deployment actually flows from a Git commit to a
running container on Amazon EC2. It is written so the environment can be
reproduced from scratch, not just observed after the fact.

---

## 1. Environment Overview

| Component | Role | Notes |
|---|---|---|
| Amazon EC2 | Host for Jenkins **and** the application | Single instance hosts both |
| Jenkins (in Docker) | CI/CD orchestrator | Custom image with Docker CLI baked in |
| Docker Engine (host) | Builds and runs all containers | Shared via `/var/run/docker.sock` |
| Docker Hub | Image registry | Repository: `pierrechukason/demo-app.jma-1.1` |
| GitHub | Source control + Multibranch trigger | Jenkinsfile lives in each branch |
| SSH | Deployment transport | Jenkins → EC2 host, same-box loopback in current setup |

Because Jenkins and the deployed application share one EC2 instance, port
allocation and the Docker socket permission model are the two details that
matter most when reproducing this environment.

---

## 2. Prerequisites

Before starting, have the following ready:

- An AWS account with permission to launch EC2 instances and edit Security
  Groups
- A Docker Hub account and a repository (or the ability to create one)
- A GitHub repository containing `Jenkinsfile`, `script.groovy`, `pom.xml`,
  `Dockerfile`, and application source under `src/`
- An SSH key pair dedicated to Jenkins → EC2 deployment (do not reuse a
  personal admin key)

---

## 3. Provision the EC2 Instance

1. Launch an EC2 instance (this project used a Linux instance sized for
   running Jenkins + a lightweight Spring Boot container comfortably —
   `t3.small` or larger is recommended once Jenkins itself is added to the
   box).
2. Attach a Security Group allowing:

   | Port | Purpose | Source |
   |---|---|---|
   | 22 | SSH | Restricted to admin/CI IP range |
   | 8080 | Jenkins UI | Restricted to trusted IPs |
   | 8081 | Application | Open to whichever audience needs it |

3. Install Docker on the host EC2 instance (not inside any container yet).
   Enable and start the Docker service, and add your admin/deploy user to
   the `docker` group.

---

## 4. Build and Run the Custom Jenkins Image

Jenkins needs the Docker CLI available inside its own container so pipeline
stages can build and push images. The stock `jenkins/jenkins:lts` image does
not include this.

**`Dockerfile` for the Jenkins image:**

```dockerfile
FROM jenkins/jenkins:lts

USER root

RUN apt-get update \
    && apt-get install -y docker.io \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
```

Build it, then run the container with access to the **host's** Docker
socket, so commands issued from inside Jenkins are actually executed by the
host Docker engine:

```bash
docker build -t jenkins-docker:lts .

docker run -d \
  --name jenkins \
  --group-add 109 \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins-docker:lts
```

**Why `--group-add 109`:** the number must match the GID of the `docker`
group on the **host**, found with:

```bash
getent group docker
```

Passing the correct GID lets the `jenkins` user inside the container talk to
`/var/run/docker.sock` without loosening file permissions on the socket
itself (see `troubleshooting.md`, issue #2, for what happens if this step is
skipped).

---

## 5. Configure Jenkins Credentials

Under **Manage Jenkins → Credentials**, create the following, using these
exact IDs (the pipeline references them by ID, so they must match):

| Credential ID | Type | Purpose |
|---|---|---|
| `docker-hub-repo` | Username with password | Authenticates `docker push` to Docker Hub |
| `ec2-server-key` | SSH Username with private key | Authenticates the deploy stage over SSH |
| `GitHub-PAT` | Secret text / username+password | GitHub API access where required (webhooks, private repo checkout) |

Keeping credential IDs stable and documented is what prevents the failure
described in `troubleshooting.md` issue #3.

---

## 6. Configure the Multibranch Pipeline Job

1. In Jenkins, create a **Multibranch Pipeline** job pointing at the GitHub
   repository.
2. Jenkins will scan branches and look for a `Jenkinsfile` in each one.
3. Confirm the **Maven** tool is configured under **Manage Jenkins → Tools**
   with the name `maven-3.9` (matches the `tools { maven 'maven-3.9' }`
   block in the Jenkinsfile).
4. Save and let Jenkins perform its first branch scan/build.

---

## 7. Pipeline Execution Flow

```text
init
  |
  v
build jar        (mvn package)
  |
  v
build image       (docker build + tag)
  |
  v
deploy            (docker push, then SSH to EC2)
```

The `init` stage loads `script.groovy`, which holds the reusable
`buildJar()`, `buildImage()`, and `deployApp()` functions referenced from
the `Jenkinsfile`. Keeping these in a separate file keeps the `Jenkinsfile`
readable and makes the build logic testable independent of pipeline syntax.

---

## 8. What Happens During `deploy`

1. The image is tagged and pushed to Docker Hub:
   `pierrechukason/demo-app.jma-1.1`.
2. Jenkins opens an SSH session to the EC2 host using the `ec2-server-key`
   credential.
3. On the target host, the previous container (if any) is stopped and
   removed.
4. The new image is pulled and started:

   ```bash
   docker run -d \
     --name demo-app \
     -p 8081:8080 \
     pierrechukason/demo-app.jma-1.1
   ```

5. Port `8081` on the host is used deliberately, because Jenkins already
   occupies host port `8080` (see `system-design.md` for the full
   reasoning).

---

## 9. Post-Deployment Verification

Run these checks after every deployment — this is the fastest way to catch
a silently-exited container before treating the deployment as successful:

```bash
docker ps                          # confirm demo-app is Up
docker logs demo-app               # confirm application startup logs
docker port demo-app               # confirm 8080/tcp -> 0.0.0.0:8081
sudo ss -ltnp | grep -E ':8081|:8080'   # confirm listeners on host
curl -v http://localhost:8081      # confirm HTTP response from EC2
```

From an allowed external network:

```text
http://<EC2-PUBLIC-IP>:8081/
```

If any of these checks fail, move to `troubleshooting.md` rather than
re-running the pipeline blindly — most failures in this project were
diagnosed from container state, not from Jenkins console output alone.

---

## 10. Rollback

There is currently no automated rollback stage. The manual rollback
procedure is:

```bash
docker stop demo-app
docker rm demo-app
docker run -d --name demo-app -p 8081:8080 pierrechukason/demo-app.jma-1.1:<previous-tag>
```

This depends on a previous image tag still existing on Docker Hub (or
locally cached on the EC2 host). See `system-design.md` → *Future
Improvements* for the planned move to commit-SHA-based tags, which will
make rollback to a specific known-good build straightforward.

---

## 11. Known Environment Constraints

- Jenkins and the application currently share a single EC2 instance —
  resource contention (CPU/memory) is possible under load.
- The Docker socket mount gives the Jenkins container effective root-level
  control over the host's Docker engine. This is acceptable for a learning
  environment but would need reconsideration (e.g. a dedicated build agent)
  before production use.
- Deployment is push-based over SSH with no built-in health check gate — a
  broken image will still replace a working container unless verified
  manually afterward.