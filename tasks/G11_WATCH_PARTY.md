# Task Packet — G11 Watch Party
Feature IDs: 237–249
Branch: `feat/watch-party`
Depends on: G10
Primary source: AntoninoScardina/NuvioTV pinned SHA.

## Scope
Room code, TV↔TV/phone, play/pause/seek sync, periodic state, soft drift correction, hard seek and encrypted WebRTC data channel.

## Mandatory hardening
Explicit consent before sharing resolved URL/required headers; share only required headers; never persist sensitive values; redact logs; clean room/WebView/session lifecycle.

## Limits
Local torrent/P2P and IP-locked sources may be unsupported across devices/networks. Surface unsupported state safely instead of bypassing restrictions.

## License
Audit VDO.Ninja/third-party notices.

## Tests
Drift thresholds, command sync, lifecycle cleanup, redaction, consent, unsupported-source handling.
