---
type: prompt
tool: codex
category: coding
created: 2026-05-30
---

# Codex Repo Fix

## Use When

Use this before asking Codex to edit this repo.

## Prompt

```text
Please read AI-Workspace/01_Projects/nofall-mod.md first.

Then fix the issue I describe below. Keep the patch scoped, preserve existing style, run the most relevant Gradle verification command, and summarize:
- files changed
- behavior changed
- verification result
- any remaining risk

Issue:
```

## Notes

Best for bug fixes, focused features, and refactors with a clear target.

