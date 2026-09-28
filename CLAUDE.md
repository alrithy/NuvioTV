# Claude Code entrypoint

This repository uses a shared agent workflow.

Read and follow `AGENTS.md` as the authoritative instruction file.

If the user says only "اشتغل على نوفيو", "work on Nuvio", or "continue Nuvio":
- do not ask for a task by default;
- inspect Git/repository state;
- read `docs/HANDOFF.md`;
- continue the next incomplete roadmap gate exactly as defined in `AGENTS.md`.

Do not rely on prior conversation history.
