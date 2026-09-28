# Security & Secrets Policy

Never log or persist provider tokens, AI API keys, authorization/cookie headers, or signed stream URLs containing credentials.

## Storage
Use Android Keystore-backed/profile-scoped storage where applicable. No plaintext secrets in source, DataStore, crash reports or analytics.

## Header forwarding
Authorization/Cookie-style headers may only go to intended hosts/trusted subdomains. Subtitle helpers must not forward stream credentials to unrelated hosts.

## QR/local web flows
Use short-lived session tokens, validate requests/origins, and terminate local sessions after completion.

## Watch Party
Require explicit consent before sharing resolved URL/headers. Redact logs, do not persist, clean room/session state, retain encrypted transport.

## AI
BYOK only where documented. Provider APKs require integrity and signer verification. AI remains optional/off by default.

## Diagnostics
New diagnostics fields must be reviewed for credential leakage.

## Dependencies
Do not import arbitrary binaries from fork releases. Prefer pinned source-level imports and preserve third-party licenses/notices.

## CI / release evidence
Do not pass production secrets to PR test jobs. Keep raw logs/JUnit payloads/local properties
and signing material out of artifacts. Publish redacted diagnostics or identifier-only
results. Review URLs for userinfo, tokens, query signatures and fragments; never rely only
on a variable name being masked. Redirects must re-evaluate Authorization/Cookie forwarding
against the intended host boundary. QR tokens need expiration and strict origin checks.
