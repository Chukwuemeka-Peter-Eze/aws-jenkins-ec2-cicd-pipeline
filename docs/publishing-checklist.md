<!--
# Publishing Checklist

## 1. Security — Do This First

A public repository is a public repository forever, even after a later
commit "removes" a secret. Check this before anything else.

- [ ] No AWS access keys, secret keys, or session tokens committed anywhere
      in history (`git log -p` or a tool like `gitleaks`/`truffleHog`)
- [ ] No Docker Hub password or personal access token committed
- [ ] No SSH private keys committed (`.pem`, `id_rsa`, `ec2-server-key`, etc.)
- [ ] No `.env` files with real values committed — only `.env.example` with
      placeholder values, if used
- [ ] EC2 public IP / hostname, if shown in documentation, is either a
      placeholder (`<EC2-PUBLIC-IP>`) or an IP you're comfortable being
      public
- [ ] `.gitignore` explicitly excludes `target/`, `*.pem`, `.env`, and any
      local IDE/config directories
- [ ] If a secret was ever committed and later removed, treat it as
      compromised — rotate it, don't rely on history rewriting alone

---

## 2. Repository Metadata

- [ ] Repository name matches the project's actual scope
      (`aws-jenkins-ec2-cicd-pipeline`)
- [ ] Repository description is accurate and specific — states what's
      actually built (build tool, registry, deployment target), not a
      generic "CI/CD pipeline" blurb
- [ ] Topics/tags added: `jenkins`, `aws`, `ec2`, `docker`, `docker-hub`,
      `cicd`, `devops`, `maven`, `java` (adjust to match actual stack)
- [ ] License file added if the repo is meant to be reused by others
      (MIT is a common default for portfolio projects)

---

## 3. README Quality

- [ ] README opens with a clear one-paragraph summary of what the project
      does, before diving into architecture
- [ ] Technology stack table is accurate and complete
- [ ] Architecture diagrams render correctly on GitHub (fenced code blocks
      display fine; verify no formatting breaks in the rendered view)
- [ ] All internal links (to `docs/*.md`) are correct relative paths and
      actually resolve on GitHub
- [ ] Port mapping (`8081:8080`) and the reason for it (Jenkins already
      using `8080`) is explained, not just stated
- [ ] Credentials table lists credential *IDs* only — never values
- [ ] "Current Implementation" section matches what's actually in the repo
      right now, not an aspirational future state
- [ ] "Future Improvements" section is clearly separated from what's
      actually implemented, so a reader can't mistake a roadmap item for a
      shipped feature

---

## 4. Documentation Completeness

- [ ] `docs/deployment.md` — reproducible, step-by-step, includes
      verification commands
- [ ] `docs/troubleshooting.md` — real issues only, each with symptom, root
      cause, fix, and prevention
- [ ] `docs/lessons-learned.md` — generalized principles, not just a
      restatement of the troubleshooting log
- [ ] `docs/system-design.md` — architecture, trade-offs, and reasoning
      behind design decisions (not just a diagram)
- [ ] `docs/publishing-checklist.md` — this file, kept up to date as the
      project evolves

---

## 5. Code and Configuration Cleanliness

- [ ] `Jenkinsfile` and `script.groovy` are free of hardcoded credentials
      or IPs — everything sensitive is referenced via Jenkins credential ID
- [ ] `Dockerfile` JAR filename matches the current `pom.xml` version (see
      `troubleshooting.md` issue #5 — this drifted once already)
- [ ] `pom.xml` version and image tag are consistent with what's described
      in the README
- [ ] No commented-out dead code left in the pipeline files without
      explanation
- [ ] `target/` build output is excluded from version control

---

## 6. Commit History

- [ ] No secrets present anywhere in commit history (recheck after any
      force-push or history rewrite)
- [ ] Commit messages are reasonably descriptive — a reviewer skimming
      `git log` should be able to follow the project's evolution
- [ ] If history is noisy (many "fix typo" / "wip" commits), consider
      whether a clean squash-merge onto `main` improves first impressions
      without losing the learning narrative you want to preserve

---

## 7. Verification Before Sharing the Link

- [ ] Clone the repository fresh into a new directory and confirm the
      README alone is enough to understand the project without prior
      context
- [ ] Click through every link in the README and each `docs/*.md` file
- [ ] Confirm the GitHub URL in the README's **Author** section is correct
- [ ] If screenshots or diagrams are added later, confirm they render on
      GitHub's default (light and dark) themes

---

## 8. Optional Polish (Not Blocking)

- [ ] Add a badge row (build status, license, last commit) at the top of
      the README
- [ ] Add a short table of contents to the README if it grows much longer
- [ ] Cross-link this repo's README to the other repos in the same
      portfolio (`aws-ecr-docker-registry`, `aws-eks-cluster`,
      `aws-cli-automation`) so a visitor can navigate the full body of work
-->