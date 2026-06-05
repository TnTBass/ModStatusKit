# Version Mismatch Severity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional server-declared public version mismatch severity so consuming mods can render normal version mismatches orange by default or red when the server explicitly marks version mismatch as breaking.

**Architecture:** Keep `VersionStatus` as the connection/version state and add a separate `VersionMismatchSeverity` policy with `WARN` and `BREAKING`. Extend the status payload backward-compatibly: legacy plain version payloads decode as `WARN`, while new `MSK2` payloads can include base version, optional build metadata, and mismatch severity. Only public/base version mismatch consults severity; build mismatch remains diagnostic and can be rendered blue or teal by consuming UI code.

**Tech Stack:** Plain Java, dependency-free payload helpers, hand-written test harness in `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`, docs/reference examples in Markdown and Java.

---

## File Structure

- Create `src/main/java/cloud/explosive/modstatuskit/VersionMismatchSeverity.java`
  - Defines `WARN` and `BREAKING`.
  - Parses optional payload tokens, defaulting missing or unknown values to `WARN`.
- Create `src/main/java/cloud/explosive/modstatuskit/ModStatusServerStatus.java`
  - Immutable decoded server status: normalized server version/build plus `VersionMismatchSeverity`.
  - Bridges old one-string server version calls and new structured payload data.
- Modify `src/main/java/cloud/explosive/modstatuskit/StatusTone.java`
  - Add `RED`.
- Modify `src/main/java/cloud/explosive/modstatuskit/VersionStatus.java`
  - Keep existing `tone()` behavior unchanged so old code still maps `DIFFERENT` to orange.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusSnapshot.java`
  - Store `VersionMismatchSeverity`.
  - Add `versionMismatchSeverity()` accessor.
  - Preserve existing static factories and old `withServerVersion` behavior as `WARN`.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusKit.java`
  - Add `connected(ModStatusConfig, ModStatusServerStatus)`.
  - Keep `connected(ModStatusConfig, String)` by delegating through default `WARN`.
  - Return `RED` tone only when base versions differ and server status severity is `BREAKING`.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusDisplay.java`
  - No new field is required if `tone` carries red.
  - Existing constructors remain valid.
- Modify `src/main/java/cloud/explosive/modstatuskit/ModStatusVersionPayload.java`
  - Keep all existing legacy helpers.
  - Add structured `MSK2` encode/decode helpers for severity.
  - Keep `sendServerVersionIfSupported` legacy-compatible by continuing to send the existing version/build payload unless a new overload is used.
- Modify `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`
  - Add tests for severity defaults, red tone behavior, structured payload round trips, and legacy compatibility.
- Modify `README.md`
  - Document the full palette: gray, green, teal/blue, orange, red.
  - Explain red is opt-in server-declared public version mismatch severity.
  - Explicitly state build mismatch never becomes red in this first pass.
- Modify `examples/fabric-embedded-reference/README.md`
  - Mirror the palette and policy guidance.
- Modify `examples/fabric-embedded-reference/ExampleModStatus.java`
  - Pass structured `ModStatusServerStatus` values from the reference client callback into `ModStatusKit.connected(...)`.
- Modify `examples/fabric-embedded-reference/ExampleModStatusClient.java`
  - Decode new structured status payloads through `decodeServerStatus(...)` while keeping legacy payloads compatible through the helper.
- Modify `examples/fabric-embedded-reference/ExampleModStatusUiSnippet.java`
  - Keep the teal build mismatch derivation, but add `RED` mapping once `StatusTone.RED` exists.
- Modify `examples/fabric-embedded-reference/ExampleModStatusNetworking.java`
  - Add a commented consuming-mod integration example showing how a server would send `BREAKING` if it intentionally treats public version mismatch as incompatible.
- Modify `VERSION`
  - Bump one patch version during implementation closeout.

## Existing Helpers And Assumptions

The implementation worker should verify these live repo facts before editing:

- `ModStatusVersion.of(String)` already parses SemVer-style `+build` metadata.
- `ModStatusVersion.of(String version, String build)` already normalizes explicit base version/build pairs.
- `ModStatusVersion.toPayloadString()` already returns `version` when build metadata is absent and `version + "+" + build` when build metadata is present.
- `ModStatusKitTest` already has a private `assertThrows(Class<? extends Throwable>, Runnable, String)` helper.
- `examples/fabric-embedded-reference/ExampleModStatusClient.java` exists and is intentionally reference-only consuming-mod integration code.
- Existing uncommitted docs/reference changes for blue/teal build mismatch guidance should be preserved and reconciled into the final docs instead of reverted.

## Payload Format

Legacy payloads remain unchanged:

```text
1.2.3
```

```text
1.2.3+abc1234
```

New structured payloads use a dependency-free line format:

```text
MSK2
version=1.2.3
build=abc1234
versionMismatchSeverity=BREAKING
```

Rules:

- First line must be exactly `MSK2` for structured decoding.
- `version` is required and uses the public/base version.
- `build` is optional.
- `versionMismatchSeverity` is optional and defaults to `WARN`.
- Unknown severity values decode as `WARN`.
- Encoders must reject version/build/severity fields containing `\r` or `\n`.
- Decoders must ignore unknown keys.
- Legacy payloads must continue to decode through `decodeServerVersion(...)` and `decodeServerVersionInfo(...)`.

## Task 1: Add Severity Model And Snapshot/Display Behavior

**Files:**
- Create: `src/main/java/cloud/explosive/modstatuskit/VersionMismatchSeverity.java`
- Create: `src/main/java/cloud/explosive/modstatuskit/ModStatusServerStatus.java`
- Modify: `src/main/java/cloud/explosive/modstatuskit/StatusTone.java`
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusSnapshot.java`
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusKit.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write failing tests for version mismatch severity**

Add this call to `main` after `testBuildMetadataStatusAndDisplay();`:

```java
testVersionMismatchSeverityStatusAndDisplay();
```

Add this test method:

```java
private static void testVersionMismatchSeverityStatusAndDisplay() {
    ModStatusConfig config = ModStatusConfig.builder()
            .modId("examplemod")
            .displayName("Example Mod")
            .clientVersion("1.2.3")
            .payloadChannel("examplemod", "server_version")
            .build();

    ModStatusServerStatus warnServer = ModStatusServerStatus.of("1.2.4");
    ModStatusSnapshot warnSnapshot = ModStatusKit.connected(config, warnServer);
    ModStatusDisplay warnDisplay = ModStatusKit.display(config, warnSnapshot);
    assertEquals(VersionStatus.DIFFERENT, warnSnapshot.status(), "warn mismatch status");
    assertEquals(VersionMismatchSeverity.WARN, warnSnapshot.versionMismatchSeverity(), "warn default severity");
    assertEquals(StatusTone.ORANGE, warnDisplay.tone(), "warn mismatch stays orange");

    ModStatusServerStatus breakingServer = ModStatusServerStatus.of(
            "1.2.4",
            null,
            VersionMismatchSeverity.BREAKING
    );
    ModStatusSnapshot breakingSnapshot = ModStatusKit.connected(config, breakingServer);
    ModStatusDisplay breakingDisplay = ModStatusKit.display(config, breakingSnapshot);
    assertEquals(VersionStatus.DIFFERENT, breakingSnapshot.status(), "breaking mismatch status");
    assertEquals(VersionMismatchSeverity.BREAKING, breakingSnapshot.versionMismatchSeverity(), "breaking severity");
    assertEquals(StatusTone.RED, breakingDisplay.tone(), "breaking mismatch renders red");

    ModStatusServerStatus matchedBreakingServer = ModStatusServerStatus.of(
            "1.2.3",
            null,
            VersionMismatchSeverity.BREAKING
    );
    ModStatusDisplay matchedBreakingDisplay = ModStatusKit.display(
            config,
            ModStatusKit.connected(config, matchedBreakingServer)
    );
    assertEquals(StatusTone.GREEN, matchedBreakingDisplay.tone(), "matched version ignores breaking severity");

    ModStatusConfig buildConfig = ModStatusConfig.builder()
            .modId("examplemod")
            .displayName("Example Mod")
            .clientVersion("1.2.3")
            .clientBuild("client123")
            .payloadChannel("examplemod", "server_version")
            .build();
    ModStatusServerStatus buildMismatchServer = ModStatusServerStatus.of(
            "1.2.3",
            "server456",
            VersionMismatchSeverity.BREAKING
    );
    ModStatusDisplay buildMismatchDisplay = ModStatusKit.display(
            buildConfig,
            ModStatusKit.connected(buildConfig, buildMismatchServer)
    );
    assertEquals(StatusTone.GREEN, buildMismatchDisplay.tone(), "build mismatch never becomes red");
    assertEquals("server456", buildMismatchDisplay.serverBuild(), "server build still exposed");

    assertEquals(StatusTone.ORANGE, VersionStatus.DIFFERENT.tone(), "enum tone remains passive orange");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: compile failure mentioning missing `ModStatusServerStatus`, `VersionMismatchSeverity`, or `StatusTone.RED`.

- [ ] **Step 3: Add `VersionMismatchSeverity`**

Create `src/main/java/cloud/explosive/modstatuskit/VersionMismatchSeverity.java`:

```java
package cloud.explosive.modstatuskit;

/**
 * Server-declared severity for public/base version mismatches.
 */
public enum VersionMismatchSeverity {
    WARN,
    BREAKING;

    static VersionMismatchSeverity fromPayloadValue(String value) {
        String normalized = ModStatusStrings.optionalText(value);
        if (normalized == null) {
            return WARN;
        }
        for (VersionMismatchSeverity severity : values()) {
            if (severity.name().equalsIgnoreCase(normalized)) {
                return severity;
            }
        }
        return WARN;
    }
}
```

- [ ] **Step 4: Add `ModStatusServerStatus`**

Create `src/main/java/cloud/explosive/modstatuskit/ModStatusServerStatus.java`:

```java
package cloud.explosive.modstatuskit;

import java.util.Objects;

/**
 * Server-published status data decoded from the consuming mod's status payload.
 */
public final class ModStatusServerStatus {
    private final ModStatusVersion serverVersion;
    private final VersionMismatchSeverity versionMismatchSeverity;

    private ModStatusServerStatus(ModStatusVersion serverVersion, VersionMismatchSeverity versionMismatchSeverity) {
        this.serverVersion = Objects.requireNonNull(serverVersion, "serverVersion");
        this.versionMismatchSeverity = versionMismatchSeverity == null
                ? VersionMismatchSeverity.WARN
                : versionMismatchSeverity;
    }

    public static ModStatusServerStatus of(String serverVersion) {
        return of(serverVersion, null, VersionMismatchSeverity.WARN);
    }

    public static ModStatusServerStatus of(
            String serverVersion,
            String serverBuild,
            VersionMismatchSeverity versionMismatchSeverity
    ) {
        return new ModStatusServerStatus(
                ModStatusVersion.of(serverVersion, serverBuild),
                versionMismatchSeverity
        );
    }

    public static ModStatusServerStatus of(ModStatusVersion serverVersion, VersionMismatchSeverity versionMismatchSeverity) {
        return new ModStatusServerStatus(serverVersion, versionMismatchSeverity);
    }

    public String serverVersion() {
        return serverVersion.version();
    }

    public String serverBuild() {
        return serverVersion.build();
    }

    public ModStatusVersion serverVersionInfo() {
        return serverVersion;
    }

    public VersionMismatchSeverity versionMismatchSeverity() {
        return versionMismatchSeverity;
    }
}
```

- [ ] **Step 5: Add `RED` tone and severity-aware snapshot/display flow**

Update `src/main/java/cloud/explosive/modstatuskit/StatusTone.java`:

```java
package cloud.explosive.modstatuskit;

/**
 * UI tone a consuming mod can map to its own colors or widgets.
 */
public enum StatusTone {
    GREEN,
    ORANGE,
    RED,
    GRAY
}
```

Update `src/main/java/cloud/explosive/modstatuskit/ModStatusSnapshot.java` so it has a severity field and accessor:

```java
private final VersionMismatchSeverity versionMismatchSeverity;
```

The string constructor should set `WARN`:

```java
private ModStatusSnapshot(String serverVersion, VersionStatus status) {
    String normalized = normalize(serverVersion);
    this.serverVersion = normalized == null ? null : ModStatusVersion.of(normalized);
    this.status = Objects.requireNonNull(status, "status");
    this.versionMismatchSeverity = VersionMismatchSeverity.WARN;
}
```

Replace the existing `ModStatusVersion` constructor with:

```java
private ModStatusSnapshot(
        ModStatusVersion serverVersion,
        VersionStatus status,
        VersionMismatchSeverity versionMismatchSeverity
) {
    this.serverVersion = Objects.requireNonNull(serverVersion, "serverVersion");
    this.status = Objects.requireNonNull(status, "status");
    this.versionMismatchSeverity = versionMismatchSeverity == null
            ? VersionMismatchSeverity.WARN
            : versionMismatchSeverity;
}
```

Keep the old factory and delegate to `WARN`:

```java
static ModStatusSnapshot withServerVersion(ModStatusVersion serverVersion, VersionStatus status) {
    return withServerVersion(serverVersion, status, VersionMismatchSeverity.WARN);
}
```

Add the new factory:

```java
static ModStatusSnapshot withServerVersion(
        ModStatusVersion serverVersion,
        VersionStatus status,
        VersionMismatchSeverity versionMismatchSeverity
) {
    if (status != VersionStatus.MATCHED && status != VersionStatus.DIFFERENT) {
        throw new IllegalArgumentException("status must be MATCHED or DIFFERENT when serverVersion is present");
    }
    return new ModStatusSnapshot(serverVersion, status, versionMismatchSeverity);
}
```

Add:

```java
public VersionMismatchSeverity versionMismatchSeverity() {
    return versionMismatchSeverity;
}
```

Update `src/main/java/cloud/explosive/modstatuskit/ModStatusKit.java` by adding:

```java
public static ModStatusSnapshot connected(ModStatusConfig config, ModStatusServerStatus serverStatus) {
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(serverStatus, "serverStatus");
    VersionStatus status = config.clientVersionInfo().version().equals(serverStatus.serverVersionInfo().version())
            ? VersionStatus.MATCHED
            : VersionStatus.DIFFERENT;
    return ModStatusSnapshot.withServerVersion(
            serverStatus.serverVersionInfo(),
            status,
            serverStatus.versionMismatchSeverity()
    );
}
```

Change the existing `connected(ModStatusConfig, String)` to:

```java
public static ModStatusSnapshot connected(ModStatusConfig config, String serverVersion) {
    return connected(config, ModStatusServerStatus.of(serverVersion));
}
```

In `display(...)`, calculate tone with a helper:

```java
StatusTone tone = toneFor(status, snapshot.versionMismatchSeverity());
```

Pass `tone` into the `ModStatusDisplay` constructor instead of `status.tone()`.

Add:

```java
private static StatusTone toneFor(VersionStatus status, VersionMismatchSeverity severity) {
    if (status == VersionStatus.DIFFERENT && severity == VersionMismatchSeverity.BREAKING) {
        return StatusTone.RED;
    }
    return status.tone();
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: `ModStatusKitTest passed`.

## Task 2: Add Structured Payload Helpers

**Files:**
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusVersionPayload.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write failing tests for structured payload compatibility**

Add this call to `main` after `testVersionPayloadHelpers();`:

```java
testStructuredStatusPayloadHelpers();
```

Add this test method:

```java
private static void testStructuredStatusPayloadHelpers() {
    byte[] warnPayload = ModStatusVersionPayload.encodeServerStatus(
            "1.2.3",
            "server123",
            VersionMismatchSeverity.WARN
    );
    ModStatusServerStatus warnStatus = ModStatusVersionPayload.decodeServerStatus(warnPayload);
    assertEquals("1.2.3", warnStatus.serverVersion(), "structured warn base version");
    assertEquals("server123", warnStatus.serverBuild(), "structured warn build");
    assertEquals(VersionMismatchSeverity.WARN, warnStatus.versionMismatchSeverity(), "structured warn severity");
    assertEquals("1.2.3+server123", ModStatusVersionPayload.decodeServerVersion(warnPayload), "structured legacy version decode");

    byte[] breakingPayload = ModStatusVersionPayload.encodeServerStatus(
            "1.2.4",
            null,
            VersionMismatchSeverity.BREAKING
    );
    ModStatusServerStatus breakingStatus = ModStatusVersionPayload.decodeServerStatus(breakingPayload);
    assertEquals("1.2.4", breakingStatus.serverVersion(), "structured breaking base version");
    assertEquals(null, breakingStatus.serverBuild(), "structured breaking build absent");
    assertEquals(VersionMismatchSeverity.BREAKING, breakingStatus.versionMismatchSeverity(), "structured breaking severity");

    ModStatusServerStatus legacyPlain = ModStatusVersionPayload.decodeServerStatus(
            ModStatusVersionPayload.encodeServerVersion("1.2.5")
    );
    assertEquals("1.2.5", legacyPlain.serverVersion(), "legacy plain server version");
    assertEquals(null, legacyPlain.serverBuild(), "legacy plain build absent");
    assertEquals(VersionMismatchSeverity.WARN, legacyPlain.versionMismatchSeverity(), "legacy plain defaults warn");

    ModStatusServerStatus legacyBuild = ModStatusVersionPayload.decodeServerStatus(
            ModStatusVersionPayload.encodeServerVersion("1.2.5+abc1234")
    );
    assertEquals("1.2.5", legacyBuild.serverVersion(), "legacy build base version");
    assertEquals("abc1234", legacyBuild.serverBuild(), "legacy build metadata");
    assertEquals(VersionMismatchSeverity.WARN, legacyBuild.versionMismatchSeverity(), "legacy build defaults warn");

    String unknownSeverityPayload = "MSK2\nversion=2.0.0\nversionMismatchSeverity=LOUD\n";
    ModStatusServerStatus unknownSeverity = ModStatusVersionPayload.decodeServerStatus(
            unknownSeverityPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    );
    assertEquals(VersionMismatchSeverity.WARN, unknownSeverity.versionMismatchSeverity(), "unknown severity defaults warn");

    assertThrows(
            IllegalArgumentException.class,
            () -> ModStatusVersionPayload.encodeServerStatus("1.2.3\nbad", null, VersionMismatchSeverity.WARN),
            "structured version rejects newline"
    );
    assertThrows(
            IllegalArgumentException.class,
            () -> ModStatusVersionPayload.encodeServerStatus("1.2.3", "bad\nbuild", VersionMismatchSeverity.WARN),
            "structured build rejects newline"
    );
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: compile failure mentioning missing `encodeServerStatus` and `decodeServerStatus`.

- [ ] **Step 3: Implement structured payload helpers**

In `ModStatusVersionPayload`, add constants:

```java
private static final String STRUCTURED_PREFIX = "MSK2";
private static final String KEY_VERSION = "version";
private static final String KEY_BUILD = "build";
private static final String KEY_VERSION_MISMATCH_SEVERITY = "versionMismatchSeverity";
```

Add:

```java
public static byte[] encodeServerStatus(
        String serverVersion,
        String serverBuild,
        VersionMismatchSeverity versionMismatchSeverity
) {
    ModStatusVersion version = ModStatusVersion.of(serverVersion, serverBuild);
    VersionMismatchSeverity severity = versionMismatchSeverity == null
            ? VersionMismatchSeverity.WARN
            : versionMismatchSeverity;
    StringBuilder payload = new StringBuilder();
    payload.append(STRUCTURED_PREFIX).append('\n');
    appendField(payload, KEY_VERSION, version.version());
    if (version.build() != null) {
        appendField(payload, KEY_BUILD, version.build());
    }
    appendField(payload, KEY_VERSION_MISMATCH_SEVERITY, severity.name());
    return payload.toString().getBytes(StandardCharsets.UTF_8);
}

public static ModStatusServerStatus decodeServerStatus(byte[] payload) {
    String text = decodePayloadText(payload);
    if (!text.startsWith(STRUCTURED_PREFIX + "\n")) {
        return ModStatusServerStatus.of(ModStatusVersion.of(text), VersionMismatchSeverity.WARN);
    }

    String version = null;
    String build = null;
    VersionMismatchSeverity severity = VersionMismatchSeverity.WARN;
    String[] lines = text.split("\\n");
    for (int index = 1; index < lines.length; index++) {
        String line = lines[index];
        int separator = line.indexOf('=');
        if (separator <= 0) {
            continue;
        }
        String key = line.substring(0, separator);
        String value = line.substring(separator + 1);
        if (KEY_VERSION.equals(key)) {
            version = value;
        } else if (KEY_BUILD.equals(key)) {
            build = value;
        } else if (KEY_VERSION_MISMATCH_SEVERITY.equals(key)) {
            severity = VersionMismatchSeverity.fromPayloadValue(value);
        }
    }
    if (version == null) {
        throw new IllegalArgumentException("structured payload missing version");
    }
    return ModStatusServerStatus.of(version, build, severity);
}
```

Change `decodeServerVersion(byte[] payload)` to decode structured payloads to the legacy version string:

```java
public static String decodeServerVersion(byte[] payload) {
    String text = decodePayloadText(payload);
    if (text.startsWith(STRUCTURED_PREFIX + "\n")) {
        return decodeServerStatus(payload).serverVersionInfo().toPayloadString();
    }
    return ModStatusStrings.requireText(text, "serverVersion");
}
```

Add helpers:

```java
private static String decodePayloadText(byte[] payload) {
    Objects.requireNonNull(payload, "payload");
    return ModStatusStrings.requireText(new String(payload, StandardCharsets.UTF_8), "serverVersion");
}

private static void appendField(StringBuilder payload, String key, String value) {
    String normalized = ModStatusStrings.requireText(value, key);
    if (normalized.indexOf('\n') >= 0 || normalized.indexOf('\r') >= 0) {
        throw new IllegalArgumentException(key + " must not contain line breaks");
    }
    payload.append(key).append('=').append(normalized).append('\n');
}
```

- [ ] **Step 4: Add send helper overload for structured status**

Add:

```java
public static boolean sendServerStatusIfSupported(
        ModStatusConfig config,
        VersionMismatchSeverity versionMismatchSeverity,
        PayloadSupport support,
        PayloadSender sender
) {
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(support, "support");
    Objects.requireNonNull(sender, "sender");

    String channel = config.payloadChannel();
    if (!support.canSend(channel)) {
        return false;
    }
    ModStatusVersion version = config.clientVersionInfo();
    sender.send(channel, encodeServerStatus(version.version(), version.build(), versionMismatchSeverity));
    return true;
}
```

Keep `sendServerVersionIfSupported(...)` unchanged so existing consuming mods continue to send legacy payloads unless they opt into structured status.

- [ ] **Step 5: Extend send helper tests**

Extend `testVersionPayloadSendIfSupported()` with:

```java
ModStatusConfig breakingConfig = ModStatusConfig.builder()
        .modId("examplemod")
        .displayName("Example Mod")
        .clientVersion("0.4.0")
        .clientBuild("server456")
        .payloadChannel("examplemod", "server_version")
        .build();
sender = new RecordingPayloadSender();
sent = ModStatusVersionPayload.sendServerStatusIfSupported(
        breakingConfig,
        VersionMismatchSeverity.BREAKING,
        channel -> true,
        sender
);
assertEquals(true, sent, "send structured status supported result");
assertEquals("examplemod:server_version", sender.channel, "sent structured status channel");
ModStatusServerStatus sentStatus = ModStatusVersionPayload.decodeServerStatus(sender.payload);
assertEquals("0.4.0", sentStatus.serverVersion(), "sent structured status version");
assertEquals("server456", sentStatus.serverBuild(), "sent structured status build");
assertEquals(VersionMismatchSeverity.BREAKING, sentStatus.versionMismatchSeverity(), "sent structured status severity");
```

- [ ] **Step 6: Run test to verify it passes**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: `ModStatusKitTest passed`.

## Task 3: Wire Structured Decode Into Reference Integration

**Files:**
- Modify: `examples/fabric-embedded-reference/ExampleModStatusClient.java`
- Modify: `examples/fabric-embedded-reference/ExampleModStatusNetworking.java`
- Modify: `examples/fabric-embedded-reference/ExampleModStatusUiSnippet.java`
- Modify: `examples/fabric-embedded-reference/README.md`

- [ ] **Step 1: Update client payload handling example**

In `ExampleModStatusClient.java`, change the client receiver path from decoding a raw server version string and calling `ExampleModStatus.onServerVersion(serverVersion)` to decoding `ModStatusServerStatus` and calling a new overload:

```java
ModStatusServerStatus serverStatus = ModStatusVersionPayload.decodeServerStatus(payloadBytes);
ExampleModStatus.onServerStatus(serverStatus);
```

If the existing snippet uses Fabric callback-specific payload APIs, keep the surrounding Fabric code unchanged and only replace the ModStatusKit decode/callback lines.

- [ ] **Step 2: Update `ExampleModStatus.java` callback**

Add this import:

```java
import cloud.explosive.modstatuskit.ModStatusServerStatus;
```

Add:

```java
public static void onServerStatus(ModStatusServerStatus serverStatus) {
    CLIENT_STATE.connected(serverStatus);
    waitingForServerStatus = false;
}
```

Keep the existing `onServerVersion(String serverVersion)` method for legacy examples, but delegate:

```java
public static void onServerVersion(String serverVersion) {
    onServerStatus(ModStatusServerStatus.of(serverVersion));
}
```

This requires Task 4 to add `ModStatusClientState.connected(ModStatusServerStatus)`.

- [ ] **Step 3: Update server send example**

In `ExampleModStatusNetworking.java`, keep the default send path passive:

```java
ModStatusVersionPayload.sendServerStatusIfSupported(
        ExampleModStatus.CONFIG,
        VersionMismatchSeverity.WARN,
        channel -> canSendPlayerPayload(player, channel),
        (channel, payload) -> sendPlayerPayload(player, channel, payload)
);
```

Add a comment directly above it:

```java
// CHANGE: use BREAKING only if your server-side mod intentionally treats
// public version mismatch as an incompatibility. Build mismatch remains
// diagnostic and should not use BREAKING.
```

- [ ] **Step 4: Update UI snippet color mapping**

After `StatusTone.RED` exists, update `colorFor(StatusTone tone)`:

```java
private static int colorFor(StatusTone tone) {
    return switch (tone) {
        case GREEN -> 0x55FF55;
        case ORANGE -> 0xFFAA00;
        case RED -> 0xFF5555;
        case GRAY -> 0xAAAAAA;
    };
}
```

Keep the existing `statusColorFor(display)` teal override guarded by `display.tone() == StatusTone.GREEN`.

- [ ] **Step 5: Update embedded reference README**

Add a concise palette list:

```markdown
Recommended colors:

- Green: public/base versions match and build metadata is absent or equal.
- Blue/teal: public/base versions match, but both sides report different builds.
- Orange: public/base versions differ and the server did not mark mismatch as breaking.
- Red: public/base versions differ and the server explicitly sent `VersionMismatchSeverity.BREAKING`.
- Gray: disconnected, unknown, or server not detected.

Use `BREAKING` only for a consuming-mod compatibility policy. It does not kick, block, or disconnect by itself; it only gives the UI a red status tone.
```

- [ ] **Step 6: Run docs/example checks**

Run:

```powershell
rg -n "VersionMismatchSeverity|BREAKING|RED|blue|teal" README.md examples/fabric-embedded-reference
```

Expected: matches in root README, embedded reference README, and example Java snippets.

## Task 4: Add Client State Convenience Overload

**Files:**
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusClientState.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write failing test for client state structured status**

Extend `testClientStateTransitions()` after the existing version mismatch assertion:

```java
state.connected(ModStatusServerStatus.of("1.2.5", null, VersionMismatchSeverity.BREAKING));
assertEquals(VersionStatus.DIFFERENT, state.snapshot().status(), "breaking connected state");
assertEquals(VersionMismatchSeverity.BREAKING, state.snapshot().versionMismatchSeverity(), "breaking client state severity");
assertEquals(StatusTone.RED, state.display().tone(), "breaking client state display tone");
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: compile failure mentioning missing `connected(ModStatusServerStatus)`.

- [ ] **Step 3: Add overload**

In `ModStatusClientState`, add:

```java
public void connected(ModStatusServerStatus serverStatus) {
    snapshot = ModStatusKit.connected(config, serverStatus);
}
```

Keep the existing `connected(String serverVersion)` method and delegate:

```java
public void connected(String serverVersion) {
    connected(ModStatusServerStatus.of(serverVersion));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: `ModStatusKitTest passed`.

## Task 5: Update Root Documentation And Version

**Files:**
- Modify: `README.md`
- Modify: `VERSION`

- [ ] **Step 1: Update root README palette and payload guidance**

Update the status color guidance so it says:

```markdown
Recommended colors:

- Green: public/base versions match and build metadata is absent or equal.
- Blue/teal: public/base versions match, but both sides report different builds.
- Orange: public/base versions differ and the server did not mark mismatch as breaking.
- Red: public/base versions differ and the server explicitly sent `VersionMismatchSeverity.BREAKING`.
- Gray: disconnected, unknown, or server not detected.
```

Add a short payload paragraph:

```markdown
Structured status payloads are optional. Legacy payloads that contain only `1.2.3` or `1.2.3+abc1234` still decode as passive `WARN` mismatches. Use structured status only when the consuming server wants to declare public version mismatch severity.
```

- [ ] **Step 2: Document red boundary**

Add this wording near the passive-status guidance:

```markdown
`BREAKING` does not enforce anything by itself. It only lets the consuming UI render a public version mismatch as red. If a consuming mod wants to kick, disconnect, block gameplay, or disable a feature, that policy still belongs to the consuming mod.
```

- [ ] **Step 3: Bump version**

Inspect `VERSION`, then increment the patch version by one. If `VERSION` is currently `0.1.4`, change it to:

```text
0.1.5
```

- [ ] **Step 4: Run final local verification**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: `ModStatusKitTest passed`.

Run:

```powershell
git diff --check
```

Expected: exit code 0. CRLF warnings are acceptable if they are the only output.

Run:

```powershell
rg -n "T[B]D|T[O]DO|F[I]XME" README.md examples/fabric-embedded-reference src docs/superpowers/plans/2026-06-04-version-mismatch-severity.md
```

Expected: no matches.

Run:

```powershell
rg -n "VersionMismatchSeverity|BREAKING|StatusTone.RED|blue|teal|orange|gray|red" README.md examples/fabric-embedded-reference src
```

Expected: matches show the severity model, red status tone, teal build mismatch guidance, and docs/reference coverage.

## Task 6: Review And Commit

**Files:**
- Review scope: all files changed by Tasks 1-5.

- [ ] **Step 1: Check worktree scope**

Run:

```powershell
git status --short
```

Expected: only files from this plan are modified or added.

- [ ] **Step 2: Send implementation through Revue**

Create a Revue `implementation-review` with explicit files and requirements:

- Add `VersionMismatchSeverity.WARN` and `BREAKING`.
- Keep legacy payloads compatible and default to `WARN`.
- Only public/base version mismatch can become red.
- Build mismatch remains diagnostic and teal/blue in consuming UI only.
- `BREAKING` changes display tone only; it does not kick, block, disconnect, or enforce.
- Core remains dependency-free with no Fabric/Loom dependencies.
- Docs/reference explain the palette and policy boundary.

- [ ] **Step 3: Action Revue findings**

For each finding:

- Verify it against the codebase.
- Fix valid findings with focused tests/docs updates.
- Re-run `.\scripts\test-java-core.ps1`, `git diff --check`, and the placeholder scan after fixes.
- Run a targeted Revue follow-up if the fix materially changes the reviewed behavior.

- [ ] **Step 4: Commit**

Run:

```powershell
git add README.md VERSION src/main/java/cloud/explosive/modstatuskit src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java examples/fabric-embedded-reference docs/superpowers/plans/2026-06-04-version-mismatch-severity.md
git commit -m "Add version mismatch severity status"
```

Expected: commit succeeds.

## Self-Review

Spec coverage:

- Server-declared red status: Task 1 and Task 2 implement `VersionMismatchSeverity.BREAKING`, `StatusTone.RED`, structured payloads, and red display tone.
- Warn/orange default: Task 1 and Task 2 keep `VersionStatus.DIFFERENT.tone()` orange and make missing/unknown payload severity decode as `WARN`.
- Build mismatch remains teal/blue only: Task 1 tests build mismatch does not become red; Task 3 keeps the reference UI teal override only when core tone is green.
- Backward compatibility: Task 2 keeps existing payload methods and adds structured helpers without changing legacy send behavior.
- No enforcement: Task 5 documents that `BREAKING` is display tone only.
- Dependency-free core: all planned code uses only Java standard library and existing project helpers.

Placeholder scan:

- This plan contains no placeholder task names, no incomplete implementation steps, and no deferred design decisions.

Type consistency:

- `VersionMismatchSeverity`, `ModStatusServerStatus`, `StatusTone.RED`, `encodeServerStatus`, `decodeServerStatus`, `sendServerStatusIfSupported`, and `versionMismatchSeverity()` are used consistently across tasks.
