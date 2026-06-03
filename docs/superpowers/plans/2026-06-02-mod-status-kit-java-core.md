# ModStatusKit Java Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first tiny, dependency-free Java core for ModStatusKit with pure status comparison, immutable display/config models, and executable tests.

**Architecture:** The v1 core is plain Java under `dev.jasmine.modstatuskit` with no Fabric, Minecraft, ModMenu, Gradle, or external test dependency. Consuming mods provide config and lifecycle inputs, then call simple static API methods to get immutable snapshots and display data. Tests compile and run with `javac`/`java` through a small PowerShell script.

**Tech Stack:** Java 17+, PowerShell, `javac`, `java`, Git.

---

## Scope

This plan implements only the generic core scaffold described in `docs/specs/2026-06-02-mod-status-kit-design.md`.

It does not implement Fabric networking, ModMenu rendering, Gradle publishing, shading automation, consuming-mod integrations, release tagging, pushing, or publishing.

## File Structure

- Create `src/main/java/dev/jasmine/modstatuskit/StatusTone.java`
  - Defines display tone values: `GREEN`, `ORANGE`, `GRAY`.
- Create `src/main/java/dev/jasmine/modstatuskit/VersionStatus.java`
  - Defines status values: `MATCHED`, `DIFFERENT`, `DISCONNECTED`, `SERVER_NOT_DETECTED`, `UNKNOWN`.
- Create `src/main/java/dev/jasmine/modstatuskit/ModStatusMessages.java`
  - Immutable message set with defaults and override factory methods.
- Create `src/main/java/dev/jasmine/modstatuskit/ModStatusConfig.java`
  - Immutable consuming-mod configuration plus builder validation.
- Create `src/main/java/dev/jasmine/modstatuskit/ModStatusSnapshot.java`
  - Immutable lifecycle/status snapshot containing status and optional server version.
- Create `src/main/java/dev/jasmine/modstatuskit/ModStatusDisplay.java`
  - Immutable display model for consuming UI code.
- Create `src/main/java/dev/jasmine/modstatuskit/ModStatusKit.java`
  - Small static facade for creating snapshots and display models.
- Create `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`
  - Dependency-free test runner with assertions in `main`.
- Create `scripts/test-java-core.ps1`
  - Compiles main/test Java sources into `build/test-classes` and runs the test runner.
- Modify `README.md`
  - Add starter API usage and test command after implementation exists.

## Task 1: Core Status Enums

**Files:**
- Create: `src/main/java/dev/jasmine/modstatuskit/StatusTone.java`
- Create: `src/main/java/dev/jasmine/modstatuskit/VersionStatus.java`

- [ ] **Step 1: Create the package directory**

Run:

```powershell
New-Item -ItemType Directory -Path src\main\java\dev\jasmine\modstatuskit -Force
```

Expected: directory exists at `src/main/java/dev/jasmine/modstatuskit`.

- [ ] **Step 2: Create `StatusTone.java`**

Write:

```java
package dev.jasmine.modstatuskit;

/**
 * UI tone a consuming mod can map to its own colors or widgets.
 */
public enum StatusTone {
    GREEN,
    ORANGE,
    GRAY
}
```

- [ ] **Step 3: Create `VersionStatus.java`**

Write:

```java
package dev.jasmine.modstatuskit;

/**
 * Informational client/server version status.
 */
public enum VersionStatus {
    MATCHED,
    DIFFERENT,
    DISCONNECTED,
    SERVER_NOT_DETECTED,
    UNKNOWN;

    public StatusTone tone() {
        return switch (this) {
            case MATCHED -> StatusTone.GREEN;
            case DIFFERENT -> StatusTone.ORANGE;
            case DISCONNECTED, SERVER_NOT_DETECTED, UNKNOWN -> StatusTone.GRAY;
        };
    }
}
```

- [ ] **Step 4: Compile the enum files**

Run:

```powershell
javac -d build\plan-check src\main\java\dev\jasmine\modstatuskit\StatusTone.java src\main\java\dev\jasmine\modstatuskit\VersionStatus.java
```

Expected: exit code `0`.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/main/java/dev/jasmine/modstatuskit/StatusTone.java src/main/java/dev/jasmine/modstatuskit/VersionStatus.java
git commit -m "Add ModStatusKit status enums"
```

## Task 2: Message and Config Models

**Files:**
- Create: `src/main/java/dev/jasmine/modstatuskit/ModStatusMessages.java`
- Create: `src/main/java/dev/jasmine/modstatuskit/ModStatusConfig.java`
- Test: `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`
- Create: `scripts/test-java-core.ps1`

- [ ] **Step 1: Create the test and script directories**

Run:

```powershell
New-Item -ItemType Directory -Path src\test\java\dev\jasmine\modstatuskit -Force
New-Item -ItemType Directory -Path scripts -Force
```

Expected: both directories exist.

- [ ] **Step 2: Create the first failing tests**

Write `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`:

```java
package dev.jasmine.modstatuskit;

public final class ModStatusKitTest {
    public static void main(String[] args) {
        testDefaultMessages();
        testConfigBuilder();
        System.out.println("ModStatusKitTest passed");
    }

    private static void testDefaultMessages() {
        ModStatusMessages messages = ModStatusMessages.defaults();

        assertEquals("Matched", messages.labelFor(VersionStatus.MATCHED), "matched label");
        assertEquals("Different versions", messages.labelFor(VersionStatus.DIFFERENT), "different label");
        assertEquals("Disconnected", messages.labelFor(VersionStatus.DISCONNECTED), "disconnected label");
        assertEquals("Server not detected", messages.labelFor(VersionStatus.SERVER_NOT_DETECTED), "server not detected label");
        assertEquals("Unknown", messages.labelFor(VersionStatus.UNKNOWN), "unknown label");
        assertEquals("Different versions may affect optional features.", messages.helpFor(VersionStatus.DIFFERENT), "different help");
    }

    private static void testConfigBuilder() {
        ModStatusConfig config = ModStatusConfig.builder()
                .modId("examplemod")
                .displayName("Example Mod")
                .clientVersion("1.2.3")
                .payloadChannel("examplemod", "server_version")
                .updateUrl("https://example.invalid/examplemod")
                .messages(ModStatusMessages.defaults())
                .build();

        assertEquals("examplemod", config.modId(), "mod id");
        assertEquals("Example Mod", config.displayName(), "display name");
        assertEquals("1.2.3", config.clientVersion(), "client version");
        assertEquals("examplemod", config.payloadNamespace(), "payload namespace");
        assertEquals("server_version", config.payloadPath(), "payload path");
        assertEquals("examplemod:server_version", config.payloadChannel(), "payload channel");
        assertEquals("https://example.invalid/examplemod", config.updateUrl(), "update url");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected [" + expected + "] but got [" + actual + "]");
        }
    }
}
```

- [ ] **Step 3: Create the test script**

Write `scripts/test-java-core.ps1`:

```powershell
$ErrorActionPreference = "Stop"

$buildDir = "build\test-classes"
if (Test-Path $buildDir) {
    Remove-Item -Recurse -Force $buildDir
}
New-Item -ItemType Directory -Path $buildDir | Out-Null

$mainSources = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$testSources = Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

javac -d $buildDir @mainSources @testSources
java -cp $buildDir dev.jasmine.modstatuskit.ModStatusKitTest
```

- [ ] **Step 4: Run tests to verify they fail**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: compile failure mentioning missing symbols such as `ModStatusMessages` and `ModStatusConfig`.

- [ ] **Step 5: Create `ModStatusMessages.java`**

Write:

```java
package dev.jasmine.modstatuskit;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Labels and short help text supplied by a consuming mod.
 */
public final class ModStatusMessages {
    private final EnumMap<VersionStatus, String> labels;
    private final EnumMap<VersionStatus, String> helpText;

    private ModStatusMessages(EnumMap<VersionStatus, String> labels, EnumMap<VersionStatus, String> helpText) {
        this.labels = labels;
        this.helpText = helpText;
    }

    public static ModStatusMessages defaults() {
        return builder()
                .label(VersionStatus.MATCHED, "Matched")
                .label(VersionStatus.DIFFERENT, "Different versions")
                .label(VersionStatus.DISCONNECTED, "Disconnected")
                .label(VersionStatus.SERVER_NOT_DETECTED, "Server not detected")
                .label(VersionStatus.UNKNOWN, "Unknown")
                .help(VersionStatus.MATCHED, "Client and server versions match.")
                .help(VersionStatus.DIFFERENT, "Different versions may affect optional features.")
                .help(VersionStatus.DISCONNECTED, "Not connected to a server or world.")
                .help(VersionStatus.SERVER_NOT_DETECTED, "No matching server-side mod was detected.")
                .help(VersionStatus.UNKNOWN, "Server version has not been received yet.")
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String labelFor(VersionStatus status) {
        return labels.get(Objects.requireNonNull(status, "status"));
    }

    public String helpFor(VersionStatus status) {
        return helpText.get(Objects.requireNonNull(status, "status"));
    }

    public static final class Builder {
        private final EnumMap<VersionStatus, String> labels = new EnumMap<>(VersionStatus.class);
        private final EnumMap<VersionStatus, String> helpText = new EnumMap<>(VersionStatus.class);

        public Builder label(VersionStatus status, String label) {
            labels.put(Objects.requireNonNull(status, "status"), requireText(label, "label"));
            return this;
        }

        public Builder help(VersionStatus status, String help) {
            helpText.put(Objects.requireNonNull(status, "status"), requireText(help, "help"));
            return this;
        }

        public ModStatusMessages build() {
            for (VersionStatus status : VersionStatus.values()) {
                labels.putIfAbsent(status, defaultLabel(status));
                helpText.putIfAbsent(status, "");
            }
            return new ModStatusMessages(copyOf(labels), copyOf(helpText));
        }

        private static EnumMap<VersionStatus, String> copyOf(Map<VersionStatus, String> source) {
            return new EnumMap<>(source);
        }

        private static String defaultLabel(VersionStatus status) {
            return switch (status) {
                case MATCHED -> "Matched";
                case DIFFERENT -> "Different versions";
                case DISCONNECTED -> "Disconnected";
                case SERVER_NOT_DETECTED -> "Server not detected";
                case UNKNOWN -> "Unknown";
            };
        }

        private static String requireText(String value, String name) {
            String trimmed = Objects.requireNonNull(value, name).trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return trimmed;
        }
    }
}
```

- [ ] **Step 6: Create `ModStatusConfig.java`**

Write:

```java
package dev.jasmine.modstatuskit;

import java.util.Objects;

/**
 * Static configuration supplied by the consuming mod.
 */
public final class ModStatusConfig {
    private final String modId;
    private final String displayName;
    private final String clientVersion;
    private final String updateUrl;
    private final String payloadNamespace;
    private final String payloadPath;
    private final ModStatusMessages messages;

    private ModStatusConfig(Builder builder) {
        this.modId = requireText(builder.modId, "modId");
        this.displayName = requireText(builder.displayName, "displayName");
        this.clientVersion = requireText(builder.clientVersion, "clientVersion");
        this.updateUrl = optionalText(builder.updateUrl);
        this.payloadNamespace = requireText(builder.payloadNamespace, "payloadNamespace");
        this.payloadPath = requireText(builder.payloadPath, "payloadPath");
        this.messages = builder.messages == null ? ModStatusMessages.defaults() : builder.messages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }

    public String clientVersion() {
        return clientVersion;
    }

    public String updateUrl() {
        return updateUrl;
    }

    public String payloadNamespace() {
        return payloadNamespace;
    }

    public String payloadPath() {
        return payloadPath;
    }

    public String payloadChannel() {
        return payloadNamespace + ":" + payloadPath;
    }

    public ModStatusMessages messages() {
        return messages;
    }

    public static final class Builder {
        private String modId;
        private String displayName;
        private String clientVersion;
        private String updateUrl;
        private String payloadNamespace;
        private String payloadPath;
        private ModStatusMessages messages;

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder clientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public Builder updateUrl(String updateUrl) {
            this.updateUrl = updateUrl;
            return this;
        }

        public Builder payloadChannel(String namespace, String path) {
            this.payloadNamespace = namespace;
            this.payloadPath = path;
            return this;
        }

        public Builder messages(ModStatusMessages messages) {
            this.messages = Objects.requireNonNull(messages, "messages");
            return this;
        }

        public ModStatusConfig build() {
            return new ModStatusConfig(this);
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected:

```text
ModStatusKitTest passed
```

- [ ] **Step 8: Commit**

Run:

```powershell
git add scripts/test-java-core.ps1 src/main/java/dev/jasmine/modstatuskit/ModStatusMessages.java src/main/java/dev/jasmine/modstatuskit/ModStatusConfig.java src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java
git commit -m "Add ModStatusKit config and message models"
```

## Task 3: Snapshot and Display Models

**Files:**
- Create: `src/main/java/dev/jasmine/modstatuskit/ModStatusSnapshot.java`
- Create: `src/main/java/dev/jasmine/modstatuskit/ModStatusDisplay.java`
- Modify: `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Add failing snapshot/display tests**

Modify `ModStatusKitTest.java` so `main` calls the new test:

```java
public static void main(String[] args) {
    testDefaultMessages();
    testConfigBuilder();
    testSnapshotAndDisplayModels();
    System.out.println("ModStatusKitTest passed");
}
```

Add:

```java
private static void testSnapshotAndDisplayModels() {
    ModStatusSnapshot snapshot = ModStatusSnapshot.withServerVersion("1.2.3", VersionStatus.MATCHED);
    assertEquals("1.2.3", snapshot.serverVersion(), "snapshot server version");
    assertEquals(VersionStatus.MATCHED, snapshot.status(), "snapshot status");

    ModStatusDisplay display = new ModStatusDisplay(
            "Example Mod",
            "1.2.3",
            "1.2.3",
            "Matched",
            "Client and server versions match.",
            StatusTone.GREEN,
            "https://example.invalid/examplemod"
    );

    assertEquals("Example Mod", display.displayName(), "display display name");
    assertEquals("1.2.3", display.clientVersion(), "display client version");
    assertEquals("1.2.3", display.serverVersion(), "display server version");
    assertEquals("Matched", display.statusLabel(), "display status label");
    assertEquals("Client and server versions match.", display.helpText(), "display help text");
    assertEquals(StatusTone.GREEN, display.tone(), "display tone");
    assertEquals("https://example.invalid/examplemod", display.updateUrl(), "display update url");
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: compile failure mentioning missing `ModStatusSnapshot` and `ModStatusDisplay`.

- [ ] **Step 3: Create `ModStatusSnapshot.java`**

Write:

```java
package dev.jasmine.modstatuskit;

import java.util.Objects;

/**
 * Current server-side status known by the consuming mod.
 */
public final class ModStatusSnapshot {
    private final String serverVersion;
    private final VersionStatus status;

    private ModStatusSnapshot(String serverVersion, VersionStatus status) {
        this.serverVersion = normalize(serverVersion);
        this.status = Objects.requireNonNull(status, "status");
    }

    public static ModStatusSnapshot disconnected() {
        return new ModStatusSnapshot(null, VersionStatus.DISCONNECTED);
    }

    public static ModStatusSnapshot unknown() {
        return new ModStatusSnapshot(null, VersionStatus.UNKNOWN);
    }

    public static ModStatusSnapshot serverNotDetected() {
        return new ModStatusSnapshot(null, VersionStatus.SERVER_NOT_DETECTED);
    }

    public static ModStatusSnapshot withServerVersion(String serverVersion, VersionStatus status) {
        if (status != VersionStatus.MATCHED && status != VersionStatus.DIFFERENT) {
            throw new IllegalArgumentException("status must be MATCHED or DIFFERENT when serverVersion is present");
        }
        String normalized = requireText(serverVersion, "serverVersion");
        return new ModStatusSnapshot(normalized, status);
    }

    public String serverVersion() {
        return serverVersion;
    }

    public VersionStatus status() {
        return status;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(Objects.requireNonNull(value, name));
        if (normalized == null) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
```

- [ ] **Step 4: Create `ModStatusDisplay.java`**

Write:

```java
package dev.jasmine.modstatuskit;

import java.util.Objects;

/**
 * UI-ready status data for the consuming mod to render.
 */
public final class ModStatusDisplay {
    private final String displayName;
    private final String clientVersion;
    private final String serverVersion;
    private final String statusLabel;
    private final String helpText;
    private final StatusTone tone;
    private final String updateUrl;

    public ModStatusDisplay(
            String displayName,
            String clientVersion,
            String serverVersion,
            String statusLabel,
            String helpText,
            StatusTone tone,
            String updateUrl
    ) {
        this.displayName = requireText(displayName, "displayName");
        this.clientVersion = requireText(clientVersion, "clientVersion");
        this.serverVersion = requireText(serverVersion, "serverVersion");
        this.statusLabel = requireText(statusLabel, "statusLabel");
        this.helpText = helpText == null ? "" : helpText.trim();
        this.tone = Objects.requireNonNull(tone, "tone");
        this.updateUrl = optionalText(updateUrl);
    }

    public String displayName() {
        return displayName;
    }

    public String clientVersion() {
        return clientVersion;
    }

    public String serverVersion() {
        return serverVersion;
    }

    public String statusLabel() {
        return statusLabel;
    }

    public String helpText() {
        return helpText;
    }

    public StatusTone tone() {
        return tone;
    }

    public String updateUrl() {
        return updateUrl;
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected:

```text
ModStatusKitTest passed
```

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/main/java/dev/jasmine/modstatuskit/ModStatusSnapshot.java src/main/java/dev/jasmine/modstatuskit/ModStatusDisplay.java src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java
git commit -m "Add ModStatusKit snapshot and display models"
```

## Task 4: Static API Facade

**Files:**
- Create: `src/main/java/dev/jasmine/modstatuskit/ModStatusKit.java`
- Modify: `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Add failing API tests**

Modify `main`:

```java
public static void main(String[] args) {
    testDefaultMessages();
    testConfigBuilder();
    testSnapshotAndDisplayModels();
    testStatusApi();
    testCustomMessages();
    System.out.println("ModStatusKitTest passed");
}
```

Add helper:

```java
private static ModStatusConfig exampleConfig() {
    return ModStatusConfig.builder()
            .modId("examplemod")
            .displayName("Example Mod")
            .clientVersion("1.2.3")
            .payloadChannel("examplemod", "server_version")
            .updateUrl("https://example.invalid/examplemod")
            .messages(ModStatusMessages.defaults())
            .build();
}
```

Add tests:

```java
private static void testStatusApi() {
    ModStatusConfig config = exampleConfig();

    assertDisplay(config, ModStatusKit.disconnected(), VersionStatus.DISCONNECTED, "Unknown", StatusTone.GRAY);
    assertDisplay(config, ModStatusKit.unknown(), VersionStatus.UNKNOWN, "Unknown", StatusTone.GRAY);
    assertDisplay(config, ModStatusKit.serverNotDetected(), VersionStatus.SERVER_NOT_DETECTED, "Unknown", StatusTone.GRAY);
    assertDisplay(config, ModStatusKit.connected(config, "1.2.3"), VersionStatus.MATCHED, "1.2.3", StatusTone.GREEN);
    assertDisplay(config, ModStatusKit.connected(config, "1.2.4"), VersionStatus.DIFFERENT, "1.2.4", StatusTone.ORANGE);
}

private static void testCustomMessages() {
    ModStatusMessages messages = ModStatusMessages.builder()
            .label(VersionStatus.DIFFERENT, "Different versions")
            .help(VersionStatus.DIFFERENT, "Different versions may miss or hide new features. Gameplay remains compatible.")
            .build();
    ModStatusConfig config = ModStatusConfig.builder()
            .modId("carrybabyanimals")
            .displayName("Carry Baby Animals")
            .clientVersion("0.1.3")
            .payloadChannel("carrybabyanimals", "server_version")
            .messages(messages)
            .build();

    ModStatusDisplay display = ModStatusKit.display(config, ModStatusKit.connected(config, "0.1.4"));

    assertEquals("Different versions", display.statusLabel(), "custom different label");
    assertEquals("Different versions may miss or hide new features. Gameplay remains compatible.", display.helpText(), "custom different help");
}

private static void assertDisplay(
        ModStatusConfig config,
        ModStatusSnapshot snapshot,
        VersionStatus expectedStatus,
        String expectedServerVersion,
        StatusTone expectedTone
) {
    ModStatusDisplay display = ModStatusKit.display(config, snapshot);
    assertEquals(expectedStatus.tone(), display.tone(), "status tone from enum");
    assertEquals(expectedTone, display.tone(), "display tone");
    assertEquals(expectedServerVersion, display.serverVersion(), "display server version");
    assertEquals(config.messages().labelFor(expectedStatus), display.statusLabel(), "display status label");
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected: compile failure mentioning missing `ModStatusKit`.

- [ ] **Step 3: Create `ModStatusKit.java`**

Write:

```java
package dev.jasmine.modstatuskit;

import java.util.Objects;

/**
 * Tiny public facade for consuming mods.
 */
public final class ModStatusKit {
    private static final String UNKNOWN_SERVER_VERSION = "Unknown";

    private ModStatusKit() {
    }

    public static ModStatusSnapshot disconnected() {
        return ModStatusSnapshot.disconnected();
    }

    public static ModStatusSnapshot unknown() {
        return ModStatusSnapshot.unknown();
    }

    public static ModStatusSnapshot serverNotDetected() {
        return ModStatusSnapshot.serverNotDetected();
    }

    public static ModStatusSnapshot connected(ModStatusConfig config, String serverVersion) {
        Objects.requireNonNull(config, "config");
        String normalizedServerVersion = requireText(serverVersion, "serverVersion");
        VersionStatus status = config.clientVersion().equals(normalizedServerVersion)
                ? VersionStatus.MATCHED
                : VersionStatus.DIFFERENT;
        return ModStatusSnapshot.withServerVersion(normalizedServerVersion, status);
    }

    public static ModStatusDisplay display(ModStatusConfig config, ModStatusSnapshot snapshot) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(snapshot, "snapshot");

        String serverVersion = snapshot.serverVersion() == null ? UNKNOWN_SERVER_VERSION : snapshot.serverVersion();
        VersionStatus status = snapshot.status();
        ModStatusMessages messages = config.messages();

        return new ModStatusDisplay(
                config.displayName(),
                config.clientVersion(),
                serverVersion,
                messages.labelFor(status),
                messages.helpFor(status),
                status.tone(),
                config.updateUrl()
        );
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\scripts\test-java-core.ps1
```

Expected:

```text
ModStatusKitTest passed
```

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/main/java/dev/jasmine/modstatuskit/ModStatusKit.java src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java
git commit -m "Add ModStatusKit core API"
```

## Task 5: Validation and README Usage

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add README usage docs**

Append this section to `README.md`:

````markdown
## Java Core API

The v1 core is plain Java. A consuming mod creates a config, updates status from its own lifecycle/networking code, and asks ModStatusKit for display data.

```java
ModStatusConfig config = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("1.2.3")
    .payloadChannel("examplemod", "server_version")
    .messages(ModStatusMessages.defaults())
    .build();

ModStatusSnapshot snapshot = ModStatusKit.connected(config, "1.2.3");
ModStatusDisplay display = ModStatusKit.display(config, snapshot);
```

For custom mismatch wording:

```java
ModStatusMessages messages = ModStatusMessages.builder()
    .label(VersionStatus.DIFFERENT, "Different versions")
    .help(VersionStatus.DIFFERENT, "Different versions may miss or hide new features. Gameplay remains compatible.")
    .build();
```

The consuming mod owns Fabric lifecycle hooks, capability-gated networking, and ModMenu rendering.

## Testing

Run the dependency-free Java core tests:

```powershell
.\scripts\test-java-core.ps1
```
````

- [ ] **Step 2: Run full verification**

Run:

```powershell
.\scripts\test-java-core.ps1
git diff --check
$redFlags = @(
    [string]::new([char[]](84, 66, 68)),
    [string]::new([char[]](84, 79, 68, 79)),
    [string]::new([char[]](70, 73, 88, 77, 69))
)
foreach ($redFlag in $redFlags) {
    rg -n $redFlag README.md docs/specs docs/superpowers src scripts
}
```

Expected:

```text
ModStatusKitTest passed
```

`git diff --check` exits `0`.

Each `rg` command exits `1` with no matches.

- [ ] **Step 3: Commit**

Run:

```powershell
git add README.md
git commit -m "Document ModStatusKit Java core usage"
```

## Task 6: Implementation Review Gate

**Files:**
- Review scope after implementation:
  - `README.md`
  - `scripts/test-java-core.ps1`
  - `src/main/java/dev/jasmine/modstatuskit/*.java`
  - `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`

- [ ] **Step 1: Check final repo state**

Run:

```powershell
git status --short
git log --oneline -5
.\scripts\test-java-core.ps1
```

Expected:

```text
ModStatusKitTest passed
```

`git status --short` should be clean before opening the review, unless the implementation-review request intentionally reviews uncommitted changes.

- [ ] **Step 2: Route through Revue**

Use `superpowers-review-gates` with `review_mode=implementation-review`.

Recommended review unit:

```json
{
  "scope_basis": "explicit-files",
  "recommended": true,
  "user_choice": "codex-decides",
  "reason": "Focused implementation review for the tiny Java core scaffold only."
}
```

Requirements for the review request:

```text
- Core remains plain Java with no Fabric, Minecraft, ModMenu, Gradle, or external test dependencies.
- API supports simple consuming-mod calls: config, lifecycle snapshot, display model.
- Equal known versions are MATCHED/GREEN.
- Unequal known versions are DIFFERENT/ORANGE.
- DISCONNECTED, SERVER_NOT_DETECTED, and UNKNOWN are GRAY.
- Server state is clearable by consuming-mod lifecycle calls.
- Custom messages override defaults without changing comparison logic.
- No consuming mod IDs are hardcoded in core logic.
```

- [ ] **Step 3: Action review findings as appropriate**

If Revue returns findings, use `superpowers:receiving-code-review`, evaluate each finding, make only appropriate fixes, run `.\scripts\test-java-core.ps1`, and mark fixed findings with `addressed_in_commit:<commit>`.

## Self-Review Checklist

- Spec coverage:
  - Java-only tiny core: Tasks 1-4.
  - Simple, repeatable API calls: Task 4 and Task 5.
  - Embedded/relocated design remains docs-only: README and spec remain the source of truth; no packaging work is added.
  - Status states and tones: Task 1 and Task 4 tests.
  - Custom messages: Task 2 and Task 4 tests.
  - Lifecycle clear/refresh model: Task 4 exposes `disconnected()`, `unknown()`, `serverNotDetected()`, and `connected(...)` calls for consuming mods.
  - Capability-gated networking and ModMenu boundaries: kept out of implementation; documented in README/spec.
- Red-flag scan target: no unfinished-work markers.
- Type consistency:
  - `VersionStatus.tone()` returns `StatusTone`.
  - `ModStatusConfig.messages()` returns `ModStatusMessages`.
  - `ModStatusKit.display(...)` returns `ModStatusDisplay`.
  - `ModStatusSnapshot.serverVersion()` is nullable; `ModStatusDisplay.serverVersion()` is UI text and never blank.
