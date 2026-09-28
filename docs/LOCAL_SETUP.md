# Local / agent setup

Git, Python 3, JDK 17 and the Android SDK required by the checked-in Gradle wrapper/build.
Use the wrapper, not a globally selected Gradle version. Never commit local properties or signing keys.

```bash
python3 -m pip install -r scripts/superfork/requirements.txt
python3 scripts/superfork/preflight.py --fetch
python3 scripts/superfork/validate_project_state.py
python3 -m unittest discover -s scripts/superfork/tests
python3 scripts/superfork/run_full_unit_suite.py
./gradlew :app:assembleFullDebug --stacktrace
```

The suite runner is the complete command; do not append `|| true`, filter tests, or run
the XML classifier against stale outputs. It removes only its generated JUnit directory,
requires a fresh completion receipt and propagates Gradle infrastructure errors.
CI uses no production properties/secrets and an ephemeral debug keystore.
If Android tools are unavailable locally, record NOT RUN and rely on exact-head GitHub
CI before merging. Hardware checks remain MANUAL-PENDING until real device execution.
