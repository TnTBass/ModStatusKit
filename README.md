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
- optional update URL
- payload channel namespace and path
- custom status/help messages

Use a channel namespace owned by the consuming mod, normally the mod id. Use a path that is unique within that mod, such as `server_version` or `mod_status/server_version`.

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
String serverVersion = display.serverVersion();
String label = display.statusLabel();
String help = display.helpText();
StatusTone tone = display.tone();
String updateUrl = display.updateUrl();
```

The consuming mod decides where this appears. A common choice is a ModMenu/config screen section when ModMenu is present. If ModMenu is absent, the consuming mod should simply skip that UI; gameplay still works.

## Status States

ModStatusKit models these informational states:

- `Matched`: client and server versions are the same; display green.
- `Different versions`: client and server versions differ; display orange.
- `Disconnected`: the client is not connected to a server or world; display gray.
- `Server not detected`: connected to a server where the consuming server mod was not detected; display gray.
- `Unknown`: connected, but no server version payload has been received; display gray.

ModStatusKit itself is informational only. It does not gate gameplay, kick players, disconnect clients, or show automatic login nags. A consuming mod may still choose to enforce its own policy based on the status data.

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
