# ModStatusKit

ModStatusKit is a tiny reusable Java library for Fabric Minecraft mods that want to show client/server version status in their own UI.

It is intended to be embedded and relocated into each consuming mod jar. Players install only the actual mod they care about, such as CarryBabyAnimals, SignPort, or MultiGolem. Players should not need to install a separate ModStatusKit mod.

## Intended Use

A consuming mod provides a small configuration object:

- mod id
- display name
- current client version
- optional update URL
- payload channel namespace and path
- custom status/help messages
- optional protocol or status version for future compatibility

The library returns a simple display model that the consuming mod can render in its own ModMenu/config UI when ModMenu is present. If ModMenu is absent, nothing breaks and no status UI is shown.

## Status States

ModStatusKit models these informational states:

- `Matched`: client and server versions are the same; display green.
- `Different versions`: client and server versions differ; display orange.
- `Disconnected`: the client is not connected to a server or world; display gray.
- `Server not detected`: connected to a server where the consuming server mod was not detected; display gray.
- `Unknown`: connected, but no server version payload has been received; display gray.

The status is informational only. It does not gate gameplay, kick players, disconnect clients, or show automatic login nags.

## Networking Model

Networking is optional and capability-gated. Each consuming mod owns its own payload channel, for example:

- `carrybabyanimals:server_version`
- `signport:server_version`
- `multigolem:server_version`

The library core should not hardcode these mods or depend on Minecraft classes. Fabric networking belongs in thin consuming-mod integration code so vanilla/unmodded clients never receive custom payloads they cannot understand.

## Embedding Strategy

Consumers should shade or relocate ModStatusKit into an internal package to avoid shared classpath collisions when multiple mods embed it at the same time.

Example relocated package names:

- `dev.jasmine.carrybabyanimals.internal.modstatus`
- `dev.jasmine.signport.internal.modstatus`
- `dev.jasmine.multigolem.internal.modstatus`

The v1 goal is simple, repeatable, usable API calls from each consuming mod, not a mandatory published dependency or a standalone player-installed mod.

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
