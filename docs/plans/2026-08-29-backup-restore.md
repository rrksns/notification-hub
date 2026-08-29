# Backup And Restore Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add repeatable Compose backup and guarded restore commands for MySQL, MongoDB, Redis, and Kafka metadata.

**Architecture:** `backup.sh` creates one timestamped artifact directory using the existing Compose service containers. `restore.sh` validates that directory, prints commands by default, and performs writes only with `--confirm`; Redis restore is explicitly separated because it requires a container restart.

**Tech Stack:** Bash, Docker Compose CLI, MySQL `mysqldump`, MongoDB `mongodump`/`mongorestore`, Redis `redis-cli`, Kafka topic CLI.

---

### Task 1: Backup command

**Files:** Create `scripts/backup/backup.sh`.

1. Validate `docker compose` availability and create a timestamped output directory.
2. Dump MySQL, MongoDB, and Kafka metadata into the directory.
3. Trigger Redis `BGSAVE`, wait for completion, and copy `dump.rdb`.
4. Write a manifest with creation time and artifact names.
5. Verify with `bash -n` and `backup.sh --dry-run`.

### Task 2: Guarded restore command

**Files:** Create `scripts/backup/restore.sh`.

1. Require a backup directory and validate required artifacts.
2. Print restore commands without modifying services by default.
3. Require `--confirm` before MySQL, MongoDB, Kafka, or Redis writes.
4. Restore Redis only after stopping the Redis service, then start it again.
5. Verify with `bash -n`, missing-argument checks, and default dry-run.

### Task 3: Operations documentation

**Files:** Modify `README.md`, `manual_test.md`, `checklist.md`, `context-notes.md`, and the commercialization priority list.

1. Document backup schedule, RPO/RTO, storage requirements, and commands.
2. Record that real external backup storage and a separate restore environment remain operator prerequisites.
3. Mark implementation and local verification complete while keeping the production rehearsal item explicit.

### Task 4: Verification and commit

1. Run both shell syntax checks and all dry-run checks.
2. Run `git diff --check`.
3. Commit the logical change and push the feature branch.
