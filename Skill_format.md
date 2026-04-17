# OpenCode Skill Format Reference

## Folder Structure

```
skill-name/
├── SKILL.md          ← required
├── references/       ← optional, large docs loaded on demand
├── scripts/          ← optional, executable helpers
└── assets/           ← optional, templates/data files
```

## SKILL.md Structure

```markdown
---
name: skill-name
description: <trigger description, max 1024 chars>
license: Apache-2.0        # optional
metadata:                  # optional
  author: your-team
  version: "1.0"
---

# Body — free-form markdown
```

## name rules
- 1–64 characters
- Lowercase alphanumeric + single hyphens
- Must match directory name exactly
- Regex: `^[a-z0-9]+(-[a-z0-9]+)*$`
- No leading/trailing hyphens, no `--`

## description rules
- 1–1024 characters
- Primary trigger mechanism — agent decides to load skill based on this alone
- Must convey: WHAT the skill does + WHEN to use it
- Should be slightly "pushy" to avoid undertriggering

## Progressive disclosure loading
1. **Metadata only** (name + description) → always in context
2. **SKILL.md body** → loaded when skill triggers
3. **references/** files → loaded on demand when SKILL.md points to them

Implication: keep SKILL.md body focused (<500 lines). Offload bulk content to references/.

## Discovery locations
- Project-local: `.opencode/skills/<skill-name>/SKILL.md`
- Global: `~/.config/opencode/skills/<skill-name>/SKILL.md`
- Also supported: `~/.claude/skills/` and `~/.agents/skills/`

## Permissions (opencode.json)
```json
{
  "permission": {
    "skill": {
      "*": "allow"
    }
  }
}
```
