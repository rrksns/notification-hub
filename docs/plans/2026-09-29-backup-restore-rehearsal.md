# Backup Restore Rehearsal Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Measure a repeatable backup and restore rehearsal in a disposable Compose environment and retain only non-secret operational evidence.

**Architecture:** Start the existing Compose data services with a test-only Alertmanager Secret file, seed representative data, and create a backup using the repository script. A `NOTIFICATION_HUB_CONTAINER_PREFIX` scopes the disposable environment away from existing Compose containers. After source teardown, start a clean project with the same prefix, restore the artifacts, and compare the seeded MySQL, MongoDB, Redis, and Kafka metadata values. The rehearsal records elapsed backup and restore durations against the 24-hour RPO and 60-minute RTO targets.

**Tech Stack:** Docker Compose, Bash, MySQL CLI, Mongo shell, Redis CLI, Kafka CLI.

---

### Task 1: Create isolated rehearsal prerequisites

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`

1. Confirm the main worktree is clean and create `feat/backup-restore-rehearsal` in `.worktrees/`.
2. Create a temporary non-repository SMTP Secret file and start only the Compose data services and Kafka topic initializer.
3. Verify each source service reports healthy before inserting fixture data.

### Task 2: Exercise backup and restore

**Files:**
- Test: `scripts/backup/backup.sh`
- Test: `scripts/backup/restore.sh`

1. Insert a uniquely named fixture into MySQL, MongoDB, Redis, and Kafka metadata.
2. Run `scripts/backup/backup.sh --output <temporary-directory>` and measure the elapsed time.
3. Stop and remove the source Compose data containers and volumes.
4. Start a clean Compose data environment, run `scripts/backup/restore.sh --input <backup-directory> --confirm`, and measure the elapsed time.
5. Assert every fixture is restored and Kafka topics match the saved metadata.

### Task 3: Record evidence and integrate

**Files:**
- Modify: `manual_test.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`
- Modify: `docs/plans/2026-09-13-refactoring-priority-review.md`
- Modify: `docs/plans/2026-08-19-commercialization-priority-list.md`

1. Record only timestamps, durations, fixture identifiers, and pass or fail results. Do not store credentials or backup contents in the repository.
2. Mark the disposable separate-environment rehearsal complete, while retaining external backup storage and production restore approval as outstanding requirements.
3. Run script syntax checks, backup and restore dry-runs, `git diff --check`, and confirm no temporary artifacts are tracked.
4. Commit, push `feat/backup-restore-rehearsal`, merge into `main`, and push `main`.

## Execution Status

- Container prefix support and its static release-gate check are complete.
- The initial source backup and restore attempt was blocked by shared OrbStack VM resource contention. The disposable Kubernetes Deployments were scaled to zero, OrbStack restarted, and the original replica counts were restored after the rehearsal.
- The real source backup completed in 6 seconds. A clean-volume restore completed in 17 seconds, with MySQL, MongoDB, Redis, and Kafka topic fixture verification successful.
- The disposable-environment RPO was below one minute and RTO was 17 seconds. External backup storage and production restore approval remain outside this rehearsal.
