# ModStatusKit

ModStatusKit is a tiny reusable Java library for Fabric Minecraft mods that want to show client/server version status in their own UI.

It is intended to be embedded and relocated into each consuming mod jar.

## Intended Use

A consuming mod provides ModStatusKit with its own identity, versions, payload channel, display copy, and policy. ModStatusKit provides a raw status/display model plus dependency-free helpers that the consuming mod can call from its own Fabric lifecycle and networking callbacks.

The consuming mod is responsible for:

- registering Fabric callbacks and receivers in its own entrypoints
- rendering `ModStatusDisplay` in its own UI, if it wants a UI
- choosing any mismatch-enforcement policy and clear disconnect reason text
- shading and relocating ModStatusKit into the shipped mod jar

ModStatusKit does not depend on Minecraft, Fabric API, or ModMenu.

## Create A Config

Create one `ModStatusConfig` from the consuming mod's own metadata:

```java
ModStatusConfig config = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("1.2.3")
    .updateUrl("https://modrinth.com/mod/examplemod")
    .payloadChannel("examplemod", "server_version")
    .messages(ModStatusMessages.defaults())
    .build();
```

The config fields are:

- mod id
- display name
- current client version
- optional diagnostic build metadata
- optional update URL
- payload channel namespace and path
- custom status/help messages

Use a channel namespace owned by the consuming mod, normally the mod id. Use a path that is unique within that mod, such as `server_version` or `mod_status/server_version`.

## Build Metadata Stamping

Mod writers can add optional build metadata when several jars share the same public version. ModStatusKit owns the parsing, display data, status comparison, and payload behavior for that metadata. The consuming mod owns stamping the build value into its jar during Gradle or CI.

Keep the public mod version stable, such as `0.1.3`, and pass the build value separately with `.clientBuild(...)`. Do not put the build metadata in the jar filename unless the consuming mod intentionally wants public files named that way.

ModStatusKit treats build metadata as diagnostic detail by default: `0.4.0+abc1234` and `0.4.0+def5678` still match on the base version `0.4.0`, while the build values remain available for support screens or logs.

Supported forms:

```java
ModStatusConfig inline = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("0.4.0+abc1234")
    .payloadChannel("examplemod", "server_version")
    .build();

ModStatusConfig explicit = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("0.4.0")
    .clientBuild(BuildInfo.GIT_COMMIT)
    .payloadChannel("examplemod", "server_version")
    .build();
```

The explicit `clientBuild(...)` value wins if both forms are present. Only SemVer-style `+build` metadata is parsed from version strings; unsupported forms such as `0.4.0-abc1234` remain ordinary version text.

ModStatusKit does not discover Git information at runtime. Stamp the build value during Gradle or CI, then pass the generated constant or property to `clientBuild(...)`. Common sources include `-PbuildNumber`, CI values such as `GITHUB_SHA` or `GITHUB_RUN_NUMBER`, `git rev-parse --short HEAD`, a generated `BuildInfo` Java class, a generated properties resource, or expanded metadata in `fabric.mod.json`.

Copy/paste Gradle example for a generated Java constant (Groovy DSL, `build.gradle`):

```groovy
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

// CHANGE: adapt the package and generated source path for your mod.
def generatedBuildInfoDir = layout.buildDirectory.dir("generated/sources/buildInfo/java")
def buildInfoPackage = "com.example.yourmod"

static String javaStringLiteral(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}

abstract class GitCommitValueSource implements ValueSource<String, ValueSourceParameters.None> {
    @Override
    String obtain() {
        try {
            def process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .redirectErrorStream(true)
                .start()
            def output = process.inputStream.getText("UTF-8").trim()
            return process.waitFor() == 0 && !output.isEmpty() ? output : null
        } catch (ignored) {
            return null
        }
    }
}

def buildMetadata = providers.gradleProperty("buildNumber")
    .orElse(providers.environmentVariable("GITHUB_SHA").map { it.take(7) })
    .orElse(providers.environmentVariable("GITHUB_RUN_NUMBER"))
    .orElse(providers.of(GitCommitValueSource) {})
    .orElse("dev")

tasks.register("generateBuildInfo") {
    def outputDir = generatedBuildInfoDir
    inputs.property("buildMetadata", buildMetadata)
    outputs.dir(outputDir)

    doLast {
        def packagePath = buildInfoPackage.replace(".", "/")
        def targetDir = outputDir.get().dir(packagePath).asFile
        targetDir.mkdirs()
        new File(targetDir, "BuildInfo.java").text = """package ${buildInfoPackage};

public final class BuildInfo {
    public static final String GIT_COMMIT = "${javaStringLiteral(buildMetadata.get())}";

    private BuildInfo() {
    }
}
"""
    }
}

sourceSets {
    main {
        java.srcDir(generatedBuildInfoDir)
    }
}

tasks.named("compileJava") {
    dependsOn("generateBuildInfo")
}

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn("generateBuildInfo")
}
```

Then pass the generated constant to ModStatusKit from consuming-mod integration code:

```java
ModStatusConfig config = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("0.1.3")
    .clientBuild(BuildInfo.GIT_COMMIT)
    .payloadChannel("examplemod", "server_version")
    .build();
```

If the local fallback is `dev`, suppress it in normal player-facing UI unless the consuming mod deliberately wants to show local build labels.

## Update Status

For direct core use, the consuming mod can keep a `ModStatusSnapshot` and update it from its own lifecycle/networking callbacks:

```java
ModStatusSnapshot snapshot = ModStatusKit.disconnected();
```

Use the helper that matches the current connection state:

- `ModStatusKit.disconnected()` before joining a world/server and after leaving one.
- `ModStatusKit.unknown()` after connecting while waiting for a bounded detection window or server payload.
- `ModStatusKit.serverNotDetected()` when capability checks or the detection window show that the server-side consuming mod is absent.
- `ModStatusKit.connected(config, serverVersion)` after receiving the consuming mod's server version.

`connected(config, serverVersion)` compares the server version to `config.clientVersion()` and returns either `MATCHED` or `DIFFERENT`.

For repeated client integration code, use `ModStatusClientState` as the small state holder:

```java
ModStatusClientState status = ModStatusClientState.create(config);

status.unknown();                // called from the consuming mod's client join callback
status.connected("1.2.3");       // called after decoding the server version payload
status.disconnected();           // called from the consuming mod's disconnect callback

ModStatusDisplay display = status.display();
```

If the consuming mod uses a bounded detection window, it can move from `UNKNOWN` to `SERVER_NOT_DETECTED` only when the state is still unknown:

```java
status.markServerNotDetectedIfUnknown();
```

## Render Display Data

Ask ModStatusKit for UI-ready data, then render that data in the consuming mod's own UI:

```java
ModStatusDisplay display = ModStatusKit.display(config, snapshot);

String title = display.displayName();
String clientVersion = display.clientVersion();
String clientBuild = display.clientBuild();
String serverVersion = display.serverVersion();
String serverBuild = display.serverBuild();
String label = display.statusLabel();
String help = display.helpText();
StatusTone tone = display.tone();
String updateUrl = display.updateUrl();
```

Use `display.tone()` for UI color. `VersionStatus.tone()` is only the default tone for the coarse status enum, so it cannot see build metadata or mismatch severity. `ModStatusKit.display(...)` combines the status, client build, server build, and server-declared mismatch severity into the final `ModStatusDisplay.tone()` value that a UI should render.

Show the base version as the primary version. Build metadata should be optional diagnostic detail, for example `build abc1234` next to the primary version or a compact support string such as `0.1.3+abc1234`. If build metadata is missing, do not show placeholder text such as `unknown build`. If the generated fallback is a local sentinel such as `dev`, hide it in normal player-facing UI unless the consuming mod deliberately wants to expose that.

For color, keep build mismatch less alarming than a version mismatch. ModStatusKit keeps the status `MATCHED` when base versions match, but `ModStatusDisplay.tone()` returns `StatusTone.TEAL` when both sides report different builds. That teal decision happens in `ModStatusKit.display(...)`, not in `VersionStatus.tone()`. Missing or equal build metadata stays green.

Recommended colors:

- Green: public/base versions match and build metadata is absent or equal.
- Blue/teal: public/base versions match, but both sides report different builds.
- Orange: public/base versions differ and the server did not mark mismatch as breaking.
- Red: public/base versions differ and the server explicitly sent `VersionMismatchSeverity.BREAKING`.
- Gray: disconnected, unknown, or server not detected.

The consuming mod decides where this appears. A common choice is a ModMenu/config screen section when ModMenu is present. If ModMenu is absent, the consuming mod should simply skip that UI; gameplay still works.

## Status States

ModStatusKit models these informational states:

- `Matched`: client and server versions are the same; display green.
- matched base version with different reported builds: display teal from `ModStatusDisplay.tone()`.
- `Different versions`: client and server versions differ; display orange.
- public/base version mismatch marked as `VersionMismatchSeverity.BREAKING` by the server: display red.
- `Disconnected`: the client is not connected to a server or world; display gray.
- `Server not detected`: connected to a server where the consuming server mod was not detected; display gray.
- `Unknown`: connected, but no server version payload has been received; display gray.

ModStatusKit itself is informational only. It does not gate gameplay, kick players, disconnect clients, or show automatic login nags. A consuming mod may still choose to enforce its own policy based on the status data.

`BREAKING` does not enforce anything by itself. It only lets the consuming UI render a public version mismatch as red. If a consuming mod wants to kick, disconnect, block gameplay, or disable a feature, that policy still belongs to the consuming mod.

## Customize Messages

Use `ModStatusMessages.builder()` when the consuming mod needs its own labels or help text. Any omitted label falls back to the default label. Any omitted help text becomes empty.

```java
ModStatusMessages messages = ModStatusMessages.builder()
    .label(VersionStatus.DIFFERENT, "Different versions")
    .help(
        VersionStatus.DIFFERENT,
        "Client and server versions differ. Optional UI features may vary, but gameplay remains compatible."
    )
    .help(VersionStatus.SERVER_NOT_DETECTED, "This server does not appear to have Example Mod installed.")
    .build();

ModStatusConfig config = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("1.2.3")
    .payloadChannel("examplemod", "server_version")
    .messages(messages)
    .build();
```

Prefer mismatch copy that is calm and passive. ModStatusKit is meant to inform players, not pressure them to leave a server or install something extra.

## Networking Model

Networking is optional and capability-gated. Each consuming mod owns its own payload channel, for example:

- `carrybabyanimals:server_version`
- `signport:server_version`
- `multigolem:server_version`

The library core should not hardcode these mods or depend on Minecraft classes. Fabric callback registration belongs in thin consuming-mod integration code, while ModStatusKit can handle the repeatable payload mechanics:

```java
byte[] payload = ModStatusVersionPayload.encodeServerVersion(config.clientVersion());
String serverVersion = ModStatusVersionPayload.decodeServerVersion(payload);
```

Build-aware helpers are also available:

```java
byte[] payload = ModStatusVersionPayload.encodeServerVersion(config.clientVersion(), config.clientBuild());
ModStatusVersion serverVersion = ModStatusVersionPayload.decodeServerVersionInfo(payload);
```

Structured status payloads are optional. Legacy payloads that contain only `1.2.3` or `1.2.3+abc1234` still decode as passive `WARN` mismatches. Use structured status only when the consuming server wants to declare public version mismatch severity.

```java
byte[] payload = ModStatusVersionPayload.encodeServerStatus(
    config.clientVersion(),
    config.clientBuild(),
    VersionMismatchSeverity.BREAKING
);
ModStatusServerStatus serverStatus = ModStatusVersionPayload.decodeServerStatus(payload);
ModStatusSnapshot snapshot = ModStatusKit.connected(config, serverStatus);
```

On the server side, a consuming mod can delegate capability-gated sending without making ModStatusKit depend on Fabric classes:

```java
ModStatusVersionPayload.sendServerVersionIfSupported(
    config,
    channel -> canSendPlayerPayload(player, channel),
    (channel, payload) -> sendPlayerPayload(player, channel, payload)
);
```

For server-to-client version reporting, send only after Fabric's negotiated receiver/channel availability check shows that the client can receive the payload. For example, a consuming server can use its own equivalent of `ServerPlayNetworking.canSend(player, channel)` before sending. Vanilla or unmodded clients must receive no custom payloads they cannot understand.

## Embedding Strategy

Consumers should shade or relocate ModStatusKit into an internal package to avoid shared classpath collisions when multiple mods embed it at the same time.

Example relocated package names:

- `cloud.explosive.carrybabyanimals.internal.modstatus`
- `cloud.explosive.signport.internal.modstatus`
- `cloud.explosive.multigolem.internal.modstatus`

Each consuming mod should choose its own unique internal package root. Do not relocate multiple consuming mods to a shared package such as `cloud.explosive.modstatuskit.shadow`; different embedded versions would collide in the same Minecraft process.

## What Not To Do

- Do not require players to install ModStatusKit as a standalone mod.
- Do not relocate every consuming mod into the same shared shadow package.
- Do not make ModStatusKit itself gate gameplay, kick players, or disconnect clients.
- Do not hide a consuming mod's own mismatch-enforcement policy; if the consuming mod chooses to enforce matching versions, document that behavior in the consuming mod.
- Do not send custom payloads to vanilla or unmodded clients.
- Do not make ModStatusKit register Fabric callbacks, ModMenu screens, or lifecycle hooks automatically.

## Minimal End-To-End Example

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

## Testing

Run the dependency-free Java core tests:

```powershell
.\scripts\test-java-core.ps1
```

Run the release-gate tests:

```powershell
.\scripts\test-release-gate.ps1
```

## Release Gate

Before making a GitHub release, increment the root `VERSION` file, commit that change, and run:

```powershell
.\scripts\check-release-gate.ps1 -Mode PreRelease
```

The pre-release gate fails if `VERSION` is not greater than the latest known `vX.Y.Z` GitHub release/tag, or if `v<VERSION>` already exists locally, on `origin`, or as a GitHub release.

This pre-release check can run before pushing the version-bump commit. It checks the local `VERSION` value and published tag/release state, but it does not check tag-to-`HEAD` alignment until the tag exists.

After tagging, pushing the tag, and creating the GitHub release, run:

```powershell
.\scripts\check-release-gate.ps1 -Mode PostRelease
```

The post-release gate fails unless `v<VERSION>` exists locally, exists on `origin`, exists as a GitHub release, and points at the current `HEAD`.
