Review the current changes on the public mirror.

This is the PUBLIC mirror of the Octopus Android SDK. Direct code changes should NOT happen here.

1. Get the diff: `git diff` (or `git diff --cached`).

2. Check with **hard thresholds**:

   **ERROR** (review FAILS):
   - Any source code changes (src/) — code changes must go through octopus-sdk-android-sources
   - Secrets, credentials, or internal URLs in the diff
   - Changes to publishing configuration without explicit approval

   **WARNING**:
   - README changes that don't match the current SDK version
   - Missing release tag for a version bump

   **INFO**:
   - Documentation improvements

3. **Verdict**: FAIL / PASS WITH WARNINGS / PASS
