# Feature Flag Policy

## Modes
- OFF: feature must not affect runtime behavior.
- ON: feature is explicitly enabled.
- AUTO: runtime policy chooses based on capability/resource/context.

## Default rules
- Official behavior stays the default/fallback until an imported path is validated.
- Experimental AI and MAT default OFF.
- High-risk playback strategies enter behind flags.
- AUTO must be deterministic and explainable in diagnostics when it selects a strategy.

## Lifecycle
1. Introduce flag with official-safe default.
2. Validate imported behavior.
3. Promote to AUTO/default only through a documented decision.
4. Retire a flag only after compatibility/migration is proven.

## Storage
Profile-specific behavior should be profile-scoped. Device capability/resource settings may be device-local. Do not mix these scopes casually.

## Testing
Each flag requires default-state tests and, for core paths, fallback tests. OFF must behave like the official path where that is the stated fallback contract.
