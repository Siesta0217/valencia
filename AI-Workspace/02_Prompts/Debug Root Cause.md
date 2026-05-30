---
type: prompt
tool: claude/codex
category: debugging
created: 2026-05-30
---

# Debug Root Cause

## Use When

Use this when there is a crash log, compile error, runtime bug, or weird behavior.

## Prompt

```text
Please debug this as a root-cause investigation.

Use the project note at AI-Workspace/01_Projects/nofall-mod.md for repo context.

Process:
1. Identify the failing symptom.
2. Find the smallest relevant code path.
3. Explain the root cause before editing.
4. Make the smallest safe fix.
5. Run the relevant verification command.
6. Create or update an Obsidian debug log under AI-Workspace/06_Debug-Logs.

Symptom / log:
```

## Notes

Do not skip the explanation step when the failure is non-obvious.

