# Lessons Learned

The value of this project was not in getting a green Jenkins pipeline on
the first attempt, it was in the debugging that happened between failures.
This document captures the engineering principles that came out of that
process, generalized beyond this one repository so they're useful on the
next pipeline too.

---

## 1. A pipeline is a system, not just a Jenkinsfile

**What happened:** Every failure in this project traced back to the
*seam* between two systems, not to a syntax error in the `Jenkinsfile`
itself, the seam between Jenkins-in-a-container and the host Docker
engine, between Jenkins and Docker Hub, between the application's declared
port and the port Jenkins already occupied.

**Why it matters:** A CI/CD pipeline is really a chain of dependent
systems:

```text
Source Control + Build Tool + Jenkins + Docker + Registry
+ SSH + Cloud Infrastructure + Application
```

A failure in any one link stops the whole chain, and the *Jenkinsfile* is
often the last place to look, not the first, it's usually just the
messenger reporting that something upstream or downstream isn't
cooperating.

**Applying it going forward:** When a pipeline fails, mentally walk the
chain link by link (source → build → image → registry → transport →
target → running process) instead of re-reading the Jenkinsfile from the
top every time.

---

## 2. Logs are evidence, not an afterthought

**What happened:** The application-container-exited issue (see
`troubleshooting.md` #5) was solved in under a minute once `docker logs`
was actually read. Every minute spent before that, guessing at the port
mapping or the deploy script, was wasted.

**Why it matters:** The instinct under pressure is to ask *"what might be
wrong?"* The more productive question is *"what evidence do I already have
access to, that I haven't looked at yet?"* Almost every layer in this stack:
Jenkins console output, `docker logs`, `ss -ltnp`, Security Group rules,
produces evidence for free. Skipping straight to fixing without reading it
means fixing the wrong thing more often than not.

**Applying it going forward:** Treat "check the logs" as step one, not step
five, of any failure, before changing any code or configuration.

---

## 3. Host ports and container ports are different resources

**What happened:** The application was deployed expecting to use the
"obvious" port `8080` inside its container, and that assumption silently
carried over to the host mapping too, colliding with Jenkins.

**Why it matters:** A container's internal port and the host port it's
mapped to are two independent numbers that happen to often match by
convention (`-p 8080:8080`). On a shared host running multiple services,
that convention breaks down fast. Being explicit about *which* port is
which `8081:8080` here, removes an entire category of "works on my
machine" bugs.

**Applying it going forward:** On any host that already runs something
else, list existing listeners (`ss -ltnp`) before assigning a new service's
host port, and document the mapping explicitly rather than assuming
container-port == host-port.

---

## 4. Containers "existing" is not the same as containers "working"

**What happened:** `docker ps -a` showed the application container present
even while it had already exited, `docker ps` alone (without `-a`) would
have made it look like the container simply never got created.

**Why it matters:** A container can be *created* and still be *exited*.
Verifying deployment success requires checking runtime state
(`docker ps`), not just that the `docker run` command returned without a
shell error.

**Applying it going forward:** Every deployment gets the same three-command
check as a minimum bar: `docker ps` (is it running), `docker logs`
(did it start cleanly), `curl` against the exposed port (is it actually
answering). See `deployment.md` §9 for the full verification checklist.

---

## 5. Infrastructure configuration and application configuration interact

**What happened:** The port collision wasn't a bug in the application or
in Jenkins individually, it only existed because *both* were deployed to
the same host. Neither team (in a real org, these might be separate teams)
would have seen the problem testing their piece in isolation.

**Why it matters:** Deployment architecture can't be designed by looking at
one service's Dockerfile in isolation. What else is already running on the
target host is part of the design input, not an afterthought discovered at
deploy time.

**Applying it going forward:** Before deploying a new service to an
existing host, inventory what's already running there (ports, resource
usage, credentials in use) as an explicit step, not something to discover
via a failed `docker run`.

---

## 6. Credential IDs are part of the pipeline's contract

**What happened:** The Docker Hub push failed simply because a credential
ID referenced in code (`docker-hub-repo`) didn't yet exist in Jenkins.

**Why it matters:** A pipeline that references credentials by ID has an
implicit contract with whatever created those credentials. That contract is
invisible in the code itself, nothing in the `Jenkinsfile` guarantees the
ID exists in Jenkins's credential store.

**Applying it going forward:** Document every credential ID a pipeline
depends on in one place (see `deployment.md` §5) and treat renaming a
credential in Jenkins as a breaking change to the pipeline, requiring a
corresponding code update.

---

## 7. Installing a tool is not the same as granting it access

**What happened:** Installing the Docker CLI inside the Jenkins container
solved *one* problem (the command existed) but not the actual problem
(the command couldn't reach the Docker daemon). Two separate fixes were
required, discovered only because the second failure produced a *different*
error message than the first.

**Why it matters:** Tooling and permissions are separate failure domains.
Assuming a fix is complete because the error message changed, rather than
disappeared, is a common way to declare victory one step too early.

**Applying it going forward:** When a fix changes the error rather than
resolving it entirely, treat that as progress, not completion, and keep
diagnosing.

---

## Summary Table

| Lesson | One-line takeaway |
|---|---|
| Pipeline as a system | Failures live at the seams between tools, not just in the Jenkinsfile |
| Logs are evidence | Read logs before guessing |
| Host vs. container ports | Never assume they match on a shared host |
| Existing ≠ working | Always verify runtime state, not just command exit codes |
| Infra + app configuration interact | Inventory the host before deploying to it |
| Credential IDs are contracts | Document and version them like code |
| Tool installed ≠ tool authorized | A changed error message means "closer," not "done" |