# AWS Jenkins CI/CD Pipeline — Screenshots & Evidence

## 1. Purpose

This document provides an index and organization framework for the screenshots captured during the AWS Jenkins CI/CD pipeline project.

The purpose of the screenshots is to provide visual evidence that the documented CI/CD workflow was actually configured, executed, and verified.

The project progresses through three stages:

```text
Part I
Jenkins → SSH → EC2 → Docker

Part II
Jenkins → Docker → ECR → EC2 → Docker Compose

Part III
Jenkins → Versioning → ECR → EC2 → Docker Compose
```

The AWS project material identifies Jenkins SSH Agent, EC2 SSH credentials, Docker, Docker Compose, ECR, multi-branch pipelines, and dynamic versioning as the major components across these stages.

---

# 2. Evidence Strategy

Screenshots should answer one simple question:

> **What does this screenshot prove?**

Avoid adding screenshots merely because they were captured during the project.

A strong evidence set should demonstrate:

```text
Configuration
     ↓
Execution
     ↓
Artifact
     ↓
Deployment
     ↓
Verification
```

---

# 3. Recommended Screenshot Directory

Organize the evidence directory approximately as follows:

```text id="nq48jr"
screenshots/
│
├── 01-jenkins/
│   ├── 01-jenkins-dashboard.png
│   ├── 02-plugins.png
│   ├── 03-credentials.png
│   └── 04-multibranch-pipeline.png
│
├── 02-part-i/
│   ├── 01-pipeline.png
│   ├── 02-console-output.png
│   ├── 03-ec2.png
│   ├── 04-docker.png
│   └── 05-application.png
│
├── 03-part-ii/
│   ├── 01-ecr.png
│   ├── 02-image.png
│   ├── 03-pipeline.png
│   ├── 04-docker-compose.png
│   └── 05-application.png
│
├── 04-part-iii/
│   ├── 01-version.png
│   ├── 02-ecr-version.png
│   ├── 03-pipeline.png
│   ├── 04-ec2.png
│   └── 05-application.png
│
└── 05-verification/
    ├── 01-final-pipeline.png
    ├── 02-final-image.png
    ├── 03-running-container.png
    └── 04-final-application.png
```

Use the structure that matches your actual screenshots. Do not create placeholder images simply to fill the directory structure.

---

# 4. Jenkins Configuration Evidence

## Screenshot 01 — Jenkins Dashboard

**Suggested filename:**

```text id="q3x4ob"
01-jenkins-dashboard.png
```

### Purpose

Demonstrates that Jenkins is installed and operational.

### Evidence

Should show:

* Jenkins dashboard
* Relevant jobs/pipelines
* Jenkins environment

### Do not expose

* Passwords
* API tokens
* Secret credentials
* Sensitive system information

---

# 5. Jenkins Plugin Evidence

## Screenshot 02 — SSH Agent Plugin

**Suggested filename:**

```text id="c8ukyt"
02-ssh-agent-plugin.png
```

### Purpose

Demonstrates installation/configuration of the Jenkins SSH Agent capability.

The project material specifically identifies installation of the Jenkins SSH Agent plugin as part of Part I.

### Evidence

The screenshot should clearly show the relevant plugin or configuration state.

---

# 6. Jenkins Credentials Evidence

## Screenshot 03 — Jenkins Credentials

**Suggested filename:**

```text id="b9v7pi"
03-jenkins-credentials.png
```

### Purpose

Demonstrates that credentials were configured in Jenkins.

### Important

The screenshot must **not reveal the credential value**.

It is sufficient to show:

* Credential entry
* Credential type
* Credential identifier where safe
* Jenkins credential management interface

Never expose:

* Private keys
* AWS secret keys
* Passwords
* Tokens

---

# 7. Multi-Branch Pipeline Evidence

## Screenshot 04 — Multi-Branch Pipeline

**Suggested filename:**

```text id="q4d7r2"
04-multibranch-pipeline.png
```

### Purpose

Demonstrates the multi-branch Jenkins pipeline configuration.

The project material identifies multi-branch pipeline execution as part of Part I.

### Evidence

Show:

* Pipeline name
* Branch discovery
* Discovered branches where applicable
* Build status where useful

---

# 8. Part I Evidence — Jenkins to EC2

Part I focuses on Jenkins automating deployment to EC2.

The project material identifies SSH credentials, remote Docker commands, Docker repository authentication, Security Group configuration, multi-branch pipelines, and EC2 deployment.

---

## Screenshot — Part I Pipeline

**Suggested filename:**

```text id="9eg9de"
01-part-i-pipeline.png
```

### Purpose

Shows the Jenkins pipeline execution.

### Evidence

Capture:

* Pipeline stages
* Successful execution
* Build status

---

## Screenshot — Part I Console Output

**Suggested filename:**

```text id="2xk0qw"
02-part-i-console-output.png
```

### Purpose

Demonstrates the commands executed by Jenkins.

### Evidence

Show relevant deployment stages without exposing credentials.

---

## Screenshot — EC2 Instance

**Suggested filename:**

```text id="c17g2f"
03-part-i-ec2.png
```

### Purpose

Demonstrates the EC2 deployment environment.

### Evidence

Show:

* Instance state
* Relevant instance information
* Deployment environment

Avoid exposing unnecessary sensitive information.

---

## Screenshot — Docker Container

**Suggested filename:**

```text id="7y2o2m"
04-part-i-docker.png
```

### Purpose

Demonstrates that the application container is running on EC2.

Useful evidence may include:

```bash id="a9f5q1"
docker ps
```

The output should clearly identify the relevant container.

---

## Screenshot — Application

**Suggested filename:**

```text id="q9j7y6"
05-part-i-application.png
```

### Purpose

Demonstrates successful application deployment.

The browser should display the deployed application.

---

# 9. Part II Evidence — Docker Compose + ECR

Part II introduces Docker Compose and Amazon ECR.

The project material identifies installation of Docker Compose, creation of `docker-compose.yaml`, Jenkins execution of Compose commands, pipeline execution, EC2 deployment, and extraction of deployment logic into a shell script.

---

## Screenshot — ECR Repository

**Suggested filename:**

```text id="y3c5ob"
01-part-ii-ecr.png
```

### Purpose

Demonstrates that the ECR repository exists.

### Evidence

Show:

* Repository name
* Image information
* Relevant repository state

---

## Screenshot — ECR Image

**Suggested filename:**

```text id="j0f7m2"
02-part-ii-ecr-image.png
```

### Purpose

Demonstrates that Jenkins successfully published a Docker image to ECR.

### Evidence

Show:

* Image repository
* Image tag
* Push result where applicable

---

## Screenshot — Part II Pipeline

**Suggested filename:**

```text id="u3h8w4"
03-part-ii-pipeline.png
```

### Purpose

Demonstrates successful Jenkins execution of the ECR deployment workflow.

### Evidence

Show the relevant pipeline stages.

---

## Screenshot — Docker Compose

**Suggested filename:**

```text id="5h2k7p"
04-part-ii-docker-compose.png
```

### Purpose

Demonstrates that Docker Compose is being used on the EC2 deployment host.

### Evidence

Where appropriate, show:

```bash id="q6q0f1"
docker compose ps
```

or equivalent evidence from the actual implementation.

---

## Screenshot — Part II Application

**Suggested filename:**

```text id="p8f3n1"
05-part-ii-application.png
```

### Purpose

Demonstrates successful application deployment through the Jenkins → ECR → EC2 → Docker Compose workflow.

---

# 10. Part III Evidence — Dynamic Versioning

Part III introduces dynamic versioning into the Jenkins CI/CD workflow.

---

## Screenshot — Jenkins Version

**Suggested filename:**

```text id="h5d2q9"
01-part-iii-version.png
```

### Purpose

Demonstrates the generated application/image version.

### Evidence

The screenshot should make the generated version identifiable.

For example:

```text id="0x3e8b"
Build
  ↓
Version
  ↓
Docker Tag
```

Use the actual versioning mechanism implemented in the project.

---

## Screenshot — Versioned ECR Image

**Suggested filename:**

```text id="p3q7n4"
02-part-iii-ecr-version.png
```

### Purpose

Demonstrates that the versioned image was published to ECR.

### Evidence

The image tag should correspond to the version produced by Jenkins.

---

## Screenshot — Final Pipeline

**Suggested filename:**

```text id="m7r2k8"
03-part-iii-pipeline.png
```

### Purpose

Demonstrates successful execution of the complete CI/CD pipeline.

### Evidence

Where applicable, show:

```text id="q9c3z1"
Build
  ↓
Version
  ↓
Docker Build
  ↓
ECR Push
  ↓
Deployment
  ↓
Verification
```

---

## Screenshot — EC2 Deployment

**Suggested filename:**

```text id="w8x5j3"
04-part-iii-ec2.png
```

### Purpose

Demonstrates that the versioned image was deployed to EC2.

---

## Screenshot — Final Application

**Suggested filename:**

```text id="z6p4m1"
05-part-iii-application.png
```

### Purpose

Demonstrates that the final version of the application is running successfully.

---

# 11. Final Verification Evidence

The final evidence set should connect the entire workflow.

---

## Screenshot — Final Jenkins Pipeline

```text id="e4k8r2"
01-final-pipeline.png
```

**Proves:**

The CI/CD pipeline completed successfully.

---

## Screenshot — Final ECR Image

```text id="n5c9v7"
02-final-image.png
```

**Proves:**

The expected Docker image exists in ECR.

---

## Screenshot — Running Container

```text id="r3j6w8"
03-running-container.png
```

**Proves:**

The expected container is running on EC2.

---

## Screenshot — Final Application

```text id="t8m2q4"
04-final-application.png
```

**Proves:**

The deployment is accessible and functioning.

---

# 12. Evidence Mapping

Use the following table to connect project requirements to evidence.

| Requirement                | Evidence               |
| -------------------------- | ---------------------- |
| Jenkins installed          | Jenkins dashboard      |
| SSH Agent configured       | Plugin screenshot      |
| EC2 credentials configured | Credentials screenshot |
| Multi-branch pipeline      | Pipeline configuration |
| Jenkins deployment         | Pipeline execution     |
| EC2 deployment             | EC2 screenshot         |
| Docker deployment          | Running container      |
| Docker Compose             | Compose evidence       |
| ECR repository             | ECR screenshot         |
| Image published            | ECR image              |
| Dynamic versioning         | Version screenshot     |
| Versioned image            | ECR version            |
| Final pipeline             | Jenkins execution      |
| Application deployment     | Browser screenshot     |

---

# 13. Screenshot Naming Convention

Use descriptive filenames.

Prefer:

```text id="d2f6a9"
part-iii-ecr-version.png
```

over:

```text id="c8z1p0"
Screenshot_2026-08-08_14-31-02.png
```

A good filename should tell you what the screenshot proves without opening it.

---

# 14. Screenshot Quality Standards

Every screenshot should be:

* Clear
* Readable
* Relevant
* Properly cropped
* Free from unnecessary desktop content
* Free from credentials
* Free from secrets
* Large enough to read important information

Avoid screenshots that contain excessive empty space.

---

# 15. Sensitive Information Review

Before adding any screenshot to GitHub, inspect it carefully.

Remove or redact:

```text id="b3x6r7"
AWS Access Keys
AWS Secret Keys
SSH Private Keys
Passwords
API Tokens
Jenkins Secrets
Registry Credentials
Environment Variables
Session Tokens
```

Also review browser screenshots for sensitive information in:

* URLs
* Browser tabs
* Console output
* Developer tools
* AWS account identifiers
* Internal hostnames

---

# 16. Evidence Integrity

Screenshots should represent the actual implementation.

Do not use:

* Internet screenshots as project evidence
* Screenshots from another environment
* Screenshots from another project
* Edited screenshots that change technical information
* Evidence that cannot be connected to the documented workflow

The objective is not to create the appearance of implementation.

The objective is to document implementation truthfully.

---

# 17. Evidence Sequence

The strongest visual story follows this order:

```text id="b6k0r2"
1. Jenkins Configuration
        ↓
2. Pipeline Configuration
        ↓
3. Pipeline Execution
        ↓
4. Docker Image
        ↓
5. ECR
        ↓
6. EC2
        ↓
7. Docker Compose
        ↓
8. Running Container
        ↓
9. Application
```

This allows a reviewer to follow the deployment lifecycle.

---

# 18. Recommended Minimum Evidence Set

If there are many screenshots from the actual project, do not necessarily publish all of them.

A concise evidence set can include:

### Jenkins

* [ ] Jenkins dashboard
* [ ] SSH Agent/plugin
* [ ] Credential configuration
* [ ] Multi-branch pipeline

### Part I

* [ ] Successful pipeline
* [ ] EC2 deployment
* [ ] Running Docker container
* [ ] Application

### Part II

* [ ] ECR repository
* [ ] ECR image
* [ ] Docker Compose deployment
* [ ] Application

### Part III

* [ ] Dynamic version
* [ ] Versioned ECR image
* [ ] Final pipeline
* [ ] Final application

This provides strong coverage without overwhelming the repository.

---

# 19. Screenshot-to-Documentation Links

Where appropriate, reference screenshots from the relevant documentation.

For example:

```markdown
![Jenkins Pipeline](../screenshots/02-part-i/01-pipeline.png)
```

Use relative paths so the documentation works when viewed directly from GitHub.

---

# 20. GitHub Rendering Check

After pushing the screenshots to GitHub:

* [ ] Images load
* [ ] Relative paths work
* [ ] Images are readable
* [ ] No broken links
* [ ] No accidental secrets are visible
* [ ] Filenames are correct
* [ ] Case sensitivity is correct

Remember that GitHub paths are case-sensitive in many environments.

---

# 21. Evidence Review Before Publication

Perform a final review:

```text id="t2q5x8"
Screenshot
    │
    ▼
What does it prove?
    │
    ▼
Is that claim documented?
    │
    ▼
Does the implementation actually support it?
    │
    ▼
Any sensitive information?
    │
    ▼
Publish
```

If a screenshot cannot answer what it proves, it probably does not belong in the final evidence set.

---

# 22. Final Evidence Checklist

```text id="h8n4q6"
JENKINS
[ ] Dashboard
[ ] SSH Agent
[ ] Credentials
[ ] Multi-branch pipeline

PART I
[ ] Pipeline execution
[ ] Console output
[ ] EC2
[ ] Docker container
[ ] Application

PART II
[ ] ECR repository
[ ] ECR image
[ ] Pipeline
[ ] Docker Compose
[ ] Application

PART III
[ ] Dynamic version
[ ] Versioned ECR image
[ ] Final pipeline
[ ] EC2 deployment
[ ] Final application

SECURITY
[ ] No private keys
[ ] No passwords
[ ] No AWS credentials
[ ] No tokens
[ ] No registry secrets

GITHUB
[ ] Images render
[ ] Paths work
[ ] Filenames are descriptive
[ ] Evidence matches documentation
```

---

# 23. Final Evidence Principle

The screenshots should tell the story of the project's transformation:

```text id="q2s8y4"
Manual Deployment
       ↓
Jenkins Automation
       ↓
Docker Image
       ↓
Amazon ECR
       ↓
EC2 Deployment
       ↓
Docker Compose
       ↓
Dynamic Versioning
       ↓
Running Application
```