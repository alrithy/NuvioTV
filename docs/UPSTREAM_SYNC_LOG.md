# Upstream Sync Log

This file records every official Nuvio upstream movement after the Superfork project was created. Never rewrite old entries.

## 2026-09-28 — Pre-G0 baseline refresh
- Purpose: start feature implementation from the true latest official `dev` available immediately before G0.
- Previous official anchor: `c257a2365ee3386b582dc2974ec235cfe0381f33`
- New official baseline: `fd7973d91dd75d790c5f9b3d68dae652655e92c4`
- Upstream distance: 2 commits.
- Upstream merge: PR #3720 — `fix(home): keep a row's window on its focused card after a refresh`.
- Feature commit in merge: `514771f21221169ed209ca584fb850bee34d9bcf`.
- Changed production file: `app/src/main/java/com/nuvio/tv/ui/screens/home/ModernHomeContent.kt`.
- Superfork integration merge commit: `3cf04ccdcc20515acb093c28ad9b7c3943a39057`.
- Conflicts: none.
- Feature-port code present before sync: none.
- Decision: adopt the newer official state before G0 implementation so fork feature work does not start two commits behind upstream.
