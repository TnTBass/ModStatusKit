# Optional Fabric Client Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add dependency-free helper APIs that consuming Fabric mods can call from their own lifecycle and networking callbacks.

**Architecture:** Keep ModStatusKit core free of Fabric, Minecraft, ModMenu, and UI dependencies. Add small Java helpers for current client status state, server-version payload encoding/decoding, and capability-gated send delegation so implementing mods provide Fabric callbacks and policy while ModStatusKit handles repeatable status mechanics.

**Tech Stack:** Plain Java compiled by `javac`; dependency-free tests run with `.\scripts\test-java-core.ps1`.

---

### Task 1: Client Status State Holder

**Files:**
- Create: `src/main/java/cloud/explosive/modstatuskit/ModStatusClientState.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write the failing test**

Add `testClientStateTransitions()` to `ModStatusKitTest`, call it from `main`, and assert:

```java
private static void testClientStateTransitions() {
    ModStatusConfig config = exampleConfig();
    ModStatusClientState state = ModStatusClientState.create(config);

    assertEquals(VersionStatus.DISCONNECTED, state.snapshot().status(), "initial client state");
    assertEquals("Unknown", state.display().serverVersion(), "initial display server version");

    state.unknown();
    assertEquals(VersionStatus.UNKNOWN, state.snapshot().status(), "unknown client state");

    assertEquals(true, state.markServerNotDetectedIfUnknown(), "mark server not detected from unknown");
    assertEquals(VersionStatus.SERVER_NOT_DETECTED, state.snapshot().status(), "server not detected state");
    assertEquals(false, state.markServerNotDetectedIfUnknown(), "server not detected unchanged");

    state.connected("1.2.3");
    assertEquals(VersionStatus.MATCHED, state.snapshot().status(), "matched connected state");
    assertEquals("1.2.3", state.display().serverVersion(), "matched display server version");

    state.connected("1.2.4");
    assertEquals(VersionStatus.DIFFERENT, state.snapshot().status(), "different connected state");

    state.disconnected();
    assertEquals(VersionStatus.DISCONNECTED, state.snapshot().status(), "disconnected client state");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\test-java-core.ps1`

Expected: compile failure because `ModStatusClientState` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `ModStatusClientState` with:

```java
public final class ModStatusClientState {
    public static ModStatusClientState create(ModStatusConfig config);
    public ModStatusConfig config();
    public ModStatusSnapshot snapshot();
    public ModStatusDisplay display();
    public void disconnected();
    public void unknown();
    public void serverNotDetected();
    public void connected(String serverVersion);
    public boolean markServerNotDetectedIfUnknown();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`.

### Task 2: Version Payload Helpers

**Files:**
- Create: `src/main/java/cloud/explosive/modstatuskit/ModStatusVersionPayload.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write the failing test**

Add `testVersionPayloadHelpers()` to `ModStatusKitTest`, call it from `main`, and assert:

```java
private static void testVersionPayloadHelpers() {
    byte[] payload = ModStatusVersionPayload.encodeServerVersion(" 1.2.3 ");
    assertEquals("1.2.3", ModStatusVersionPayload.decodeServerVersion(payload), "decoded server version");

    assertThrows(IllegalArgumentException.class, () -> ModStatusVersionPayload.encodeServerVersion(""), "blank encode");
    assertThrows(NullPointerException.class, () -> ModStatusVersionPayload.encodeServerVersion(null), "null encode");
    assertThrows(IllegalArgumentException.class, () -> ModStatusVersionPayload.decodeServerVersion(new byte[0]), "empty decode");
    assertThrows(NullPointerException.class, () -> ModStatusVersionPayload.decodeServerVersion(null), "null decode");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\test-java-core.ps1`

Expected: compile failure because `ModStatusVersionPayload` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `ModStatusVersionPayload` with UTF-8 `encodeServerVersion(String)` and `decodeServerVersion(byte[])`, using the same nonblank text validation as the core config.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`.

### Task 3: Capability-Gated Send Delegation

**Files:**
- Modify: `src/main/java/cloud/explosive/modstatuskit/ModStatusVersionPayload.java`
- Modify: `src/test/java/cloud/explosive/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Write the failing test**

Add `testVersionPayloadSendIfSupported()` to `ModStatusKitTest`, call it from `main`, and assert:

```java
private static void testVersionPayloadSendIfSupported() {
    ModStatusConfig config = exampleConfig();
    RecordingPayloadSender sender = new RecordingPayloadSender();

    boolean sent = ModStatusVersionPayload.sendServerVersionIfSupported(config, channel -> true, sender);
    assertEquals(true, sent, "send supported result");
    assertEquals("examplemod:server_version", sender.channel, "sent channel");
    assertEquals("1.2.3", ModStatusVersionPayload.decodeServerVersion(sender.payload), "sent payload");

    sender = new RecordingPayloadSender();
    sent = ModStatusVersionPayload.sendServerVersionIfSupported(config, channel -> false, sender);
    assertEquals(false, sent, "send unsupported result");
    assertEquals(null, sender.channel, "unsupported channel");
    assertEquals(null, sender.payload, "unsupported payload");
}

private static final class RecordingPayloadSender implements ModStatusVersionPayload.PayloadSender {
    private String channel;
    private byte[] payload;

    @Override
    public void send(String channel, byte[] payload) {
        this.channel = channel;
        this.payload = payload;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\test-java-core.ps1`

Expected: compile failure because `sendServerVersionIfSupported`, `PayloadSupport`, or `PayloadSender` does not exist.

- [ ] **Step 3: Write minimal implementation**

Add nested interfaces and method:

```java
@FunctionalInterface
public interface PayloadSupport {
    boolean canSend(String channel);
}

@FunctionalInterface
public interface PayloadSender {
    void send(String channel, byte[] payload);
}

public static boolean sendServerVersionIfSupported(
        ModStatusConfig config,
        PayloadSupport support,
        PayloadSender sender
);
```

The method calls `support.canSend(config.payloadChannel())`, returns `false` without sending when unsupported, and otherwise sends the encoded `config.clientVersion()` payload to `sender`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`.

### Task 4: Documentation Alignment

**Files:**
- Modify: `README.md`
- Modify: `docs/specs/2026-06-02-mod-status-kit-design.md`

- [ ] **Step 1: Update README**

Describe the optional integration helpers as dependency-free Java helpers that consuming Fabric mods call from their own Fabric callbacks. State that UI remains raw `ModStatusDisplay`; no UI helper layer is added.

- [ ] **Step 2: Update design spec**

Capture the settled design: raw display model for UI, optional client state/payload helpers, capability-gated send delegation, optional consuming-mod enforcement policy with clear disconnect reason, and no ModMenu/UI helper package in this phase.

- [ ] **Step 3: Run verification**

Run:

```powershell
.\scripts\test-java-core.ps1
git diff --check
run the repository red-flag scan requested for this session
git status --short
```

Expected: tests pass, diff check exits 0, red-flag scan has no matches, and status shows only intended files before commit.
