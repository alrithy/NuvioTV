# Task Packet — G13 Experimental AI & MAT
Feature IDs: 45, 250–261
Branch: `experimental/media`
Depends on: G12
Sources: Fornace AI + ysosrs MAT.

## Objective
Integrate experimental capabilities without changing stable default runtime.

## AI
Provider APK architecture, BYOK, Keystore/profile credentials, hash + signer verification, provider identity/contract, optional ASR/subtitle/voice translation.

## MAT
Experimental Kodi-style MAT/IEC61937 path behind explicit experimental flag and hardware validation.

## Invariant
Everything in G13 defaults OFF. Disabled paths must remain inert.

## Tests
Provider identity/integrity, credential storage/redaction, disabled-path inertness, failure isolation. MAT hardware remains MANUAL-PENDING until actually tested.
