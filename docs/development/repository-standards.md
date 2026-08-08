# Repository Standards

This document defines lightweight but professional repository standards for HomeOps AI. The goal is to keep collaboration clear, safe, and easy to maintain for a small project while still reflecting modern engineering practices.

## Branch naming

Use short, descriptive branch names in lowercase kebab-case using the format:

- `<type>/<issue-number>-<short-description>`

Allowed types:

- `feature`
- `fix`
- `docs`
- `chore`

Examples:

- `feature/6-repository-standards`
- `fix/12-maintenance-reminder-bug`
- `docs/8-definition-of-done`
- `chore/10-update-dependencies`

This keeps branches easy to scan and makes the purpose of each change obvious.

## Commit message conventions

Use concise, descriptive commit messages in a conventional style.

Recommended format:

- `feat: add vehicle reminder workflow`
- `fix: correct maintenance schedule validation`
- `docs: add repository standards`
- `chore: update development notes`

Commit subjects should be short, specific, and written in the imperative mood.

## Pull request expectations

Every meaningful change should be delivered through a pull request before merging to main.

Each pull request should include:

- a short summary of the change,
- the reason for the change,
- any validation performed,
- and any relevant risks or follow-up items.

This keeps review lightweight but still structured enough to support quality and accountability.

## Direct commits to main

Direct commits to `main` are prohibited. All changes must go through a branch and a pull request.

This rule protects the main branch from unreviewed changes and helps preserve a reliable delivery history.

## Merge strategy

Prefer squash merging for most pull requests. Use a standard merge commit only when preserving the branch context is clearly valuable.

Squash merging keeps the history clean and easy to read, which is especially useful for a small project with one primary developer.

## Release and tagging expectations

Use tags for meaningful releases or milestones, such as:

- `v0.1.0`
- `v0.2.0`

Tags should be created from `main` only after the relevant work is complete and validated.

This provides a clear record of what was shipped without overcomplicating the release process.

## Documentation-only changes

Documentation-only changes should still use a branch and pull request, but the process can be lighter.

Recommended approach:

- a pull request is still required,
- review can be lighter,
- and tests are not required unless the documentation affects executable behavior or tooling.

This keeps documentation changes traceable without introducing unnecessary ceremony.

## AI-assisted changes

AI agents and AI-assisted changes are allowed, but human responsibility remains absolute.

AI agents must not commit, push, merge, or alter protected branches without human review and approval.

All AI-assisted work should still be reviewed carefully, especially when it affects:

- security,
- authentication or authorization,
- sensitive data,
- document handling,
- or production-critical behavior.

This ensures that AI accelerates delivery without reducing accountability or safety.

## Why these standards exist

These standards are intentionally lightweight but professional. They help the project stay organized, reduce risk, and preserve a clean history without creating unnecessary process overhead for a small team.
