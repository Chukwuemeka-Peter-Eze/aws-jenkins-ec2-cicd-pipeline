# Troubleshooting Log

This document records the real failures encountered while building this
pipeline, the evidence used to diagnose each one, and the fix applied. It is
written as a reference for reproducing the same environment without hitting
the same walls blind, and as a demonstration of a repeatable diagnostic
method, not just a list of fixes.

---

## Diagnostic Method Used Throughout

```text
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

The core principle: **don't guess, gather evidence.** Every issue below was
resolved by running a diagnostic command, not by speculating about the
cause.

---

## Issue 1: Jenkins Could Not Run Docker Commands

**Symptom:** The `build image` stage failed immediately, unable to find or
execute the `docker` command from inside the Jenkins container.

**Root cause:** The stock `jenkins/jenkins:lts` image does not ship with the
Docker CLI. Jenkins itself was running inside a container, so it had no
Docker binary to call in the first place.

**Diagnosis:**
```bash
docker exec -it jenkins which docker
# no such file, confirms CLI is missing
```

**Fix:** Built a custom Jenkins image that installs `docker.io` on top of
the base image (see `deployment.md` §4).

**Architecture before → after:**
```text
Before:                      After:
Jenkins Container             Jenkins Container
      X                             |
   Docker                           v
                              Docker CLI
                                    |
                                    v
                         Host Docker Socket
                                    |
                                    v
                             Docker Engine
```

**Prevention:** When Jenkins runs inside a container, always confirm
up front whether it needs to *build* Docker images (needs CLI + socket
access) or merely *deploy* already-built images (may only need SSH).

---

## Issue 2: Docker Socket Permission Problem

**Symptom:** Even after installing the Docker CLI, Docker commands issued
from inside the Jenkins container failed with a permission error when
talking to `/var/run/docker.sock`.

**Root cause:** Installing the CLI does not grant permission to use it.
The `jenkins` user inside the container was not a member of the group that
owns the host's Docker socket.

**Diagnosis:**
```bash
ls -l /var/run/docker.sock
# shows the socket's owning group and permission bits
getent group docker
# shows the GID of the docker group on the HOST
```

**Fix:** Started the Jenkins container with `--group-add <host-docker-gid>`
so the Jenkins process's effective group membership matches the group that
owns the socket, without loosening the socket's own permissions (e.g.
avoiding `chmod 666` on the socket, which would be a much broader
exposure).

**Lesson reinforced:**
> Installing a CLI does not automatically provide access to the service
> that CLI controls.

---

## Issue 3: Docker Hub Credential Problem

**Symptom:** The `deploy` stage failed at the `docker push` step with an
authentication error.

**Root cause:** The pipeline referenced a Jenkins credential ID
(`docker-hub-repo`) that did not yet exist in Jenkins's credential store,
it had been assumed to be present rather than explicitly created.

**Diagnosis:** Jenkins console output showed a credential lookup failure
referencing the missing ID directly, which made this one of the faster
issues to pinpoint once the console log was actually read line by line
rather than skimmed.

**Fix:** Created the credential in **Manage Jenkins → Credentials** with
the exact ID `docker-hub-repo`, type "Username with password", using a
Docker Hub access token rather than the account password.

**Prevention:** Keep a single source of truth (see `deployment.md` §5) for
every credential ID the pipeline expects, and treat a credential ID as part
of the pipeline's "API contract" — renaming it in Jenkins without updating
the Jenkinsfile (or vice versa) breaks the pipeline the same way a typo
would.

---

## Issue 4: Host Port Collision

**Symptom:** The application container failed to start (or immediately
exited) when deployed with `-p 8080:8080`.

**Root cause:** Jenkins itself was already bound to host port `8080`
(`-p 8080:8080` in the Jenkins `docker run` command). Docker cannot bind two
processes to the same host port.

**Diagnosis:**
```bash
sudo ss -ltnp | grep :8080
# shows Jenkins already listening on 8080
```

**Fix:** Remapped the application to host port `8081` while keeping the
container-internal port at `8080`:

```bash
docker run -d --name demo-app -p 8081:8080 pierrechukason/demo-app.jma-1.1
```

**Resulting mapping:**
```text
Jenkins:      EC2:8080  -> Jenkins container:8080
Application:  EC2:8081  -> demo-app container:8080
```

**Prevention:** Before assigning a host port to any new service on a shared
box, check what is already listening with `ss -ltnp` rather than assuming a
port is free because it's the application's "default."

---

## Issue 5: Application Container Exited Immediately

**Symptom:** After resolving the port collision, `docker ps` showed no
running `demo-app` container. `docker ps -a` showed it in an `Exited`
state.

**Root cause:** The Dockerfile's `ENTRYPOINT` referenced a hardcoded JAR
filename (`java-maven-app-1.0-SNAPSHOT.jar`) that no longer matched what
Maven was actually producing (`java-maven-app-1.1.0-SNAPSHOT.jar`), after
the project version was bumped in `pom.xml`.

**Diagnosis:**
```bash
docker ps -a
# demo-app shown as Exited (1)

docker logs demo-app
# Error: Unable to access jarfile java-maven-app-1.0-SNAPSHOT.jar
```

**Fix:** Updated the Dockerfile `ENTRYPOINT` to match the actual artifact
name:

```dockerfile
ENTRYPOINT ["java", "-jar", "java-maven-app-1.1.0-SNAPSHOT.jar"]
```

**Prevention:** Whenever `pom.xml`'s `<version>` changes, treat the
Dockerfile's hardcoded JAR filename as a dependent artifact that must be
updated in the same change or better, parameterize it via a build arg so
it can never drift silently (see `system-design.md` → *Future
Improvements*).

---

## Quick Reference: Useful Diagnostic Commands

```bash
# Container state
docker ps
docker ps -a
docker logs <container>
docker inspect <container>

# Images
docker images
docker pull <image>

# Ports / processes
sudo ss -ltnp | grep -E ':8081|:8080'

# Application reachability
curl -v http://localhost:8081
```

---

## General Principle

Across all five issues, the pattern was the same: **the fix was never
guessed, it was read off a log, a port listing, or a container inspect
output.** Restarting a container or re-running the pipeline without first
gathering evidence only re-produces the same failure with less information
than the first time.