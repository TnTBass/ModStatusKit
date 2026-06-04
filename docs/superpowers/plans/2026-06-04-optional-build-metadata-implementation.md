# Optional Build Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement optional client/server build metadata so compatible versions can still match while build identifiers are available for diagnostics.

**Architecture:** Add a small immutable `ModStatusVersion` value object that normalizes `version+build` and explicit build fields. Thread it through config, snapshots, display, and payload helpers while keeping existing one-string APIs valid.

**Tech Stack:** Plain Java, hand-written test harness in `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`, verified with `.\scripts\test-java-core.ps1`.

---

## Existing Helpers

`src/main/java/cloud/explosive/modstatuskit/ModStatusStrings.java` already provides `requireText(String, String)` and `optionalText(String)`. The implementation should reuse those helpers; no new string validation helper is required.

## File Structure

- Create `src/main/java/cloud/explosive/modstatuskit/ModStatusVersion.java`
  - Parses and stores base version plus optional build.
  - Parses only SemVer-style `+build`; unsupported formats remain base version text.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusConfig.java`
  - Store normalized client version metadata.
  - Add optional `clientBuild(String)` builder method.
  - Keep `clientVersion()` returning base display version.
  - Add `clientBuild()` and `clientVersionInfo()` accessors.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusSnapshot.java`
  - Store normalized server version metadata.
  - Keep `serverVersion()` returning base display version.
  - Add `serverBuild()` and `serverVersionInfo()` accessors.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusDisplay.java`
  - Add nullable `clientBuild` and `serverBuild` fields/accessors.
  - Keep existing constructor by delegating to a new constructor with build fields.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusKit.java`
  - Compare base versions for normal status.
  - Display base version and expose optional build details.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusVersionPayload.java`
  - Keep old encode/decode server version helpers.
  - Add explicit build-aware encode/decode helpers.
  - Keep `sendServerVersionIfSupported` compatible by sending inline `version+build` when build is present.
- Modify `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`
  - Add red/green tests for parsing, explicit build override, status behavior, display build fields, payload compatibility, and unsupported formats.
- Modify `README.md`
  - Add author-facing guidance for optional build metadata and Gradle/CI stamping.
- Modify `VERSION`
  - Bump from `0.1.2` to `0.1.3` for the implementation commit.

## Task 1: Add Version Metadata Model

**Files:**
- Create: `src/main/java/cloud/explosive/modstatuskit/ModStatusVersion.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write failing tests for version normalization**

Add `testVersionMetadataParsing();` to `main`.

Add this test method:

```java
private static void testVersionMetadataParsing() {
    ModStatusVersion plain = ModStatusVersion.of("0.4.0");
    assertEquals("0.4.0", plain.version(), "plain base version");
    assertEquals(null, plain.build(), "plain build absent");
    assertEquals("0.4.0", plain.toPayloadString(), "plain payload string");

    ModStatusVersion inline = ModStatusVersion.of("0.4.0+abc1234");
    assertEquals("0.4.0", inline.version(), "inline base version");
    assertEquals("abc1234", inline.build(), "inline build");
    assertEquals("0.4.0+abc1234", inline.toPayloadString(), "inline payload string");

    ModStatusVersion explicit = ModStatusVersion.of("0.4.0+ignored", "def5678");
    assertEquals("0.4.0", explicit.version(), "explicit base version");
    assertEquals("def5678", explicit.build(), "explicit build overrides inline");
    assertEquals("0.4.0+def5678", explicit.toPayloadString(), "explicit payload string");

    ModStatusVersion unsupported = ModStatusVersion.of("0.4.0-abc1234");
    assertEquals("0.4.0-abc1234", unsupported.version(), "unsupported format remains version");
    assertEquals(null, unsupported.build(), "unsupported build absent");

    assertThrows(IllegalArgumentException.class, () -> ModStatusVersion.of(""), "blank version metadata");
    assertThrows(NullPointerException.class, () -> ModStatusVersion.of(null), "null version metadata");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\test-java-core.ps1`

Expected: compile failure mentioning missing `ModStatusVersion`.

- [ ] **Step 3: Implement `ModStatusVersion`**

Create:

```java
package cloud.explosive.modstatuskit;

import java.util.Objects;

/**
 * Normalized public version plus optional diagnostic build metadata.
 */
public final class ModStatusVersion {
    private final String version;
    private final String build;

    private ModStatusVersion(String version, String build) {
        this.version = ModStatusStrings.requireText(version, "version");
        this.build = ModStatusStrings.optionalText(build);
    }

    public static ModStatusVersion of(String version) {
        return of(version, null);
    }

    public static ModStatusVersion of(String version, String build) {
        String normalizedVersion = ModStatusStrings.requireText(version, "version");
        String explicitBuild = ModStatusStrings.optionalText(build);
        int buildSeparator = normalizedVersion.indexOf('+');
        if (buildSeparator >= 0) {
            String baseVersion = normalizedVersion.substring(0, buildSeparator);
            String inlineBuild = normalizedVersion.substring(buildSeparator + 1);
            return new ModStatusVersion(baseVersion, explicitBuild == null ? inlineBuild : explicitBuild);
        }
        return new ModStatusVersion(normalizedVersion, explicitBuild);
    }

    public String version() {
        return version;
    }

    public String build() {
        return build;
    }

    public String toPayloadString() {
        return build == null ? version : version + "+" + build;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ModStatusVersion)) {
            return false;
        }
        ModStatusVersion that = (ModStatusVersion) other;
        return version.equals(that.version) && Objects.equals(build, that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, build);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`.

## Task 2: Thread Build Metadata Through Config, Snapshot, Display, and Status

**Files:**
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusConfig.java`
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusSnapshot.java`
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusDisplay.java`
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusKit.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write failing tests for build-aware status/display**

Add `testBuildMetadataStatusAndDisplay();` to `main`.

Add:

```java
private static void testBuildMetadataStatusAndDisplay() {
    ModStatusConfig inlineConfig = ModStatusConfig.builder()
            .modId("examplemod")
            .displayName("Example Mod")
            .clientVersion("0.4.0+client123")
            .payloadChannel("examplemod", "server_version")
            .build();

    ModStatusSnapshot snapshot = ModStatusKit.connected(inlineConfig, "0.4.0+server456");
    ModStatusDisplay display = ModStatusKit.display(inlineConfig, snapshot);

    assertEquals(VersionStatus.MATCHED, snapshot.status(), "same base version matches");
    assertEquals("0.4.0", inlineConfig.clientVersion(), "config exposes base client version");
    assertEquals("client123", inlineConfig.clientBuild(), "config exposes client build");
    assertEquals("0.4.0", snapshot.serverVersion(), "snapshot exposes base server version");
    assertEquals("server456", snapshot.serverBuild(), "snapshot exposes server build");
    assertEquals("0.4.0", display.clientVersion(), "display client base version");
    assertEquals("client123", display.clientBuild(), "display client build");
    assertEquals("0.4.0", display.serverVersion(), "display server base version");
    assertEquals("server456", display.serverBuild(), "display server build");
    assertEquals(StatusTone.GREEN, display.tone(), "build mismatch is diagnostic only");

    ModStatusConfig explicitConfig = ModStatusConfig.builder()
            .modId("examplemod")
            .displayName("Example Mod")
            .clientVersion("0.4.0+ignored")
            .clientBuild("explicit789")
            .payloadChannel("examplemod", "server_version")
            .build();
    assertEquals("explicit789", explicitConfig.clientBuild(), "explicit client build wins");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\test-java-core.ps1`

Expected: compile failure mentioning missing `clientBuild` and/or `serverBuild`.

- [ ] **Step 3: Implement build-aware fields and comparison**

Update the production files so:

- `ModStatusConfig` stores `private final ModStatusVersion clientVersion;`
- `Builder` has `private String clientBuild;` and `clientBuild(String clientBuild)`.
- `clientVersion()` returns `clientVersion.version()`.
- `clientBuild()` returns `clientVersion.build()`.
- `clientVersionInfo()` returns the `ModStatusVersion`.
- `ModStatusSnapshot` stores `ModStatusVersion serverVersion`.
- `serverVersion()` returns null for unknown states, otherwise `serverVersion.version()`.
- `serverBuild()` returns null for unknown states, otherwise `serverVersion.build()`.
- `serverVersionInfo()` returns nullable `ModStatusVersion`.
- `ModStatusKit.connected(...)` parses server metadata and compares `config.clientVersionInfo().version()` to `serverVersionInfo.version()`.
- `ModStatusDisplay` has client/server build fields and accessors, while the existing constructor delegates with null build fields.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`.

## Task 3: Add Build-Aware Payload Helpers

**Files:**
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusVersionPayload.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write failing tests for payload compatibility**

Extend `testVersionPayloadHelpers()` with:

```java
ModStatusVersion decodedPlain = ModStatusVersionPayload.decodeServerVersionInfo(
        ModStatusVersionPayload.encodeServerVersion("1.2.3")
);
assertEquals("1.2.3", decodedPlain.version(), "decoded plain payload base version");
assertEquals(null, decodedPlain.build(), "decoded plain payload build absent");

ModStatusVersion decodedInline = ModStatusVersionPayload.decodeServerVersionInfo(
        ModStatusVersionPayload.encodeServerVersion("0.4.0+server456")
);
assertEquals("0.4.0", decodedInline.version(), "decoded inline payload base version");
assertEquals("server456", decodedInline.build(), "decoded inline payload build");

byte[] explicitPayload = ModStatusVersionPayload.encodeServerVersion("0.4.0", "server456");
ModStatusVersion decodedExplicit = ModStatusVersionPayload.decodeServerVersionInfo(explicitPayload);
assertEquals("0.4.0", decodedExplicit.version(), "decoded explicit payload base version");
assertEquals("server456", decodedExplicit.build(), "decoded explicit payload build");
```

Extend `testVersionPayloadSendIfSupported()` with a build-aware config:

```java
ModStatusConfig buildConfig = ModStatusConfig.builder()
        .modId("examplemod")
        .displayName("Example Mod")
        .clientVersion("0.4.0")
        .clientBuild("server456")
        .payloadChannel("examplemod", "server_version")
        .build();
sender = new RecordingPayloadSender();
sent = ModStatusVersionPayload.sendServerVersionIfSupported(buildConfig, channel -> true, sender);
assertEquals(true, sent, "send build metadata supported result");
assertEquals("examplemod:server_version", sender.channel, "sent build metadata channel");
assertEquals("0.4.0+server456", ModStatusVersionPayload.decodeServerVersion(sender.payload), "sent build metadata payload");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\test-java-core.ps1`

Expected: compile failure mentioning missing `decodeServerVersionInfo` and `encodeServerVersion(String, String)`.

- [ ] **Step 3: Implement payload helpers**

Add:

```java
public static byte[] encodeServerVersion(String serverVersion, String serverBuild) {
    return ModStatusVersion.of(serverVersion, serverBuild).toPayloadString().getBytes(StandardCharsets.UTF_8);
}

public static ModStatusVersion decodeServerVersionInfo(byte[] payload) {
    return ModStatusVersion.of(decodeServerVersion(payload));
}
```

Update `sendServerVersionIfSupported` to send:

```java
sender.send(channel, encodeServerVersion(config.clientVersionInfo().version(), config.clientVersionInfo().build()));
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`.

## Task 4: Document Author Build Stamping and Bump Version

**Files:**
- Modify: `README.md`
- Modify: `VERSION`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Add README guidance**

Add author-facing guidance explaining:

- build metadata is optional
- `0.4.0+abc1234` is parsed
- `.clientVersion("0.4.0").clientBuild(BuildInfo.GIT_COMMIT)` is equivalent
- unsupported formats are treated as ordinary version text
- Gradle/CI must stamp the build value; ModStatusKit does not discover Git at runtime

- [ ] **Step 2: Bump version**

Change `VERSION`:

```text
0.1.3
```

- [ ] **Step 3: Run full verification**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`.

Run: `git diff --check`

Expected: no output and exit code 0.

Run: `rg -n "TBD|TODO|FIXME" README.md src docs/superpowers/specs/2026-06-04-optional-build-metadata-design.md`

Expected: no matches unless existing intentional text is found and reviewed.

- [ ] **Step 4: Commit**

Run:

```powershell
git add VERSION README.md src/main/java/cloud/explosive/modstatuskit src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java docs/superpowers/plans/2026-06-04-optional-build-metadata-implementation.md
git commit -m "Add optional build metadata support"
```

Expected: commit succeeds.

## Self-Review

- Spec coverage: The plan covers inline `+build`, explicit build metadata, explicit override, unsupported formats, status comparison by base version, diagnostic display fields, payload compatibility, docs, tests, and version bump.
- Placeholder scan: No TBD/TODO/FIXME placeholders are present.
- Type consistency: The same value object and method names are used throughout: `ModStatusVersion`, `clientBuild`, `serverBuild`, `clientVersionInfo`, `serverVersionInfo`, `encodeServerVersion(String, String)`, and `decodeServerVersionInfo`.
