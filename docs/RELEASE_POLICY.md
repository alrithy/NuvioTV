# Release Policy

## Channels
- Development: task branches / integration artifacts.
- Beta: user testing with documented known issues.
- Stable: only after release hardening and mandatory regression coverage.

## Beta requirements
CI + validator green, fullDebug build, high-risk manual-test status documented, no known data-loss/security blocker, experimental flags disclosed.

## Stable requirements
Applicable TEST_MATRIX cases pass; fresh install + upgrade tested; TV remote sanity; low-memory sanity for resource changes; sensitive-log audit; license/import ledger audit; APK checksums; release artifacts exactly match tagged commit. Experimental features stay OFF unless explicitly promoted.

Do not infer freshness from downstream fork version numbers.
