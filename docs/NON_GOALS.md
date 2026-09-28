# Non-goals & Scope Boundaries

Unless a new explicit decision changes these rules, the Superfork is NOT trying to:

- rewrite Nuvio from scratch;
- merge any entire fork branch wholesale;
- duplicate functionality already owned by current official Nuvio;
- remove official layouts/features merely because a source fork removed them;
- make experimental AI or MAT behavior mandatory;
- ship reverted Smart Vibrance as stable;
- create a permanently feature-deleted Lite edition;
- re-port obsolete custom-server infrastructure already present upstream;
- mix tvOS/Tizen/WebOS platform ports into Android TV core;
- import arbitrary release APKs/binaries from forks instead of auditable source;
- change application ID, signing identity, backend identity or production credentials during feature-port gates;
- add analytics/telemetry or new Android permissions without explicit review/decision;
- silently change source pins;
- silently drop difficult MASTER_FEATURES entries;
- claim manual/hardware validation that was not actually run;
- land old PR #1/#2 design work directly into the Superfork without a G12 audit/port decision.

Old design/prototype branches are reference material only until intentionally harvested under the current architecture.
