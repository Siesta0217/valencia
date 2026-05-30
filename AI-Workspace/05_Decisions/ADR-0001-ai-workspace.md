---
type: decision
status: accepted
created: 2026-05-30
---

# ADR-0001: Use an Obsidian Vault as AI Working Memory

## Context

AI coding sessions are useful but temporary. Project commands, conventions, prompts, and debug lessons get rediscovered too often.

## Decision

Create `AI-Workspace` as a local Obsidian vault inside the repo workspace. Keep notes as plain Markdown so Codex, Claude, and other tools can read them directly.

## Consequences

- Agents can be pointed at stable project context before editing.
- Debug and prompt knowledge survives across sessions.
- The vault should stay lightweight and avoid plugin-dependent content where possible.
- Sensitive notes should not be committed or shared without review.

