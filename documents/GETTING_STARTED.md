# Getting Started — Suchika

| | |
|---|---|
| **Type** | Reference |
| **Audience** | New developers, AI agents |
| **Status** | Active |
| **Last updated** | 2026-07-06 |

## Where Setup Actually Lives

This project's real getting-started guide is **[../CONTRIBUTING.md](../CONTRIBUTING.md)** — prerequisites, one-time database bootstrap, dev-alias quick start, and troubleshooting. Start there.

For project orientation instead of setup mechanics, see:
- **[CONTEXT_PRIMER.md](CONTEXT_PRIMER.md)** — compact project snapshot, service map, current version (read this first if you're an agent)
- **[../CLAUDE.md](../CLAUDE.md)** — commands, architecture, and key rules for working in this codebase
- **[../README.md](../README.md)** — project overview and repository structure

## Cleanup Note (2026-07-06)

This file previously contained the `devops` agent definition plus the `/integration-test` and `/logs` slash-command bodies, appended in full. That was accidental: a keyword-classification bug in `scripts/documentWriter.py` matched words like "setup" and "install" inside those `.claude/` files and misfiled them here as "getting started" content. It carried no unique information — the canonical, current copies live in `.claude/agents/devops.md` and `.claude/commands/` — so it has been removed rather than kept as a duplicate. See `documents/SCRIPTS.md` for the existing write-up on the script issue itself (not modified as part of this pass).
