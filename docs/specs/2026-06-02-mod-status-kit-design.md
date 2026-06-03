# ModStatusKit Design

## Problem Statement

Fabric mod players often install only the mod they care about. A mod may have useful client-side UI, optional server-side behavior, or both. When the client and server copies differ, a player can miss new UI features, see stale help text, or wonder whether a server has the matching mod installed.

ModStatusKit provides a small reusable status model that consuming mods can embed into their own jars. The first consumers are expected to be CarryBabyAnimals, SignPort, and MultiGolem, but the core library must stay generic.

## Goals

- Let a consuming Fabric mod show client/server version status in its own ModMenu or config UI.
- Keep the v1 core extremely lightweight and Java-only.
- Provide simple, repeatable, usable API calls for consuming mods.
- Support embedded and relocated copies in multiple mods installed at the same time.
- Keep all networking optional and capability-gated.
- Provide dependency-free helpers for repeated client state and payload mechanics.
- Let each consuming mod customize short status/help messages.
- Provide pure comparison/state logic that can be tested without Minecraft or Fabric.

## Non-Goals

- Do not make ModStatusKit a required player-installed mod.
- Do not make a standalone Fabric mod artifact the first target.
- Do not build a combined multi-mod status dashboard in v1.
- Do not duplicate ModMenu's general update checker.
- Do not make ModStatusKit itself enforce version matching.
- Do not make ModStatusKit itself kick, disconnect, block login, or gate gameplay.
- Do not add automatic login nags in v1.
- Do not hardcode CarryBabyAnimals, SignPort, or MultiGolem into the core library.
- Do not add a ModMenu, YACL, Cloth Config, or other UI helper layer in this phase.

## Consuming-Mod Model

Each consuming mod embeds ModStatusKit and calls a tiny API from its own client/config code. The mod owns its identity, version source, payload channel, display copy, relocation setup, UI placement, and enforcement policy. ModStatusKit provides raw status/display models plus dependency-free client state and payload helpers that the consuming mod can call from its own Fabric callbacks.

The core API should accept a configuration object with:

- `modId`
- `displayName`
- `clientVersion`
- optional `updateUrl`
- payload channel namespace
- payload channel path
- custom status messages

The core API should expose pure model objects and helper methods, not Fabric entrypoints. A consuming mod should be able to do the equivalent of:

```java
ModStatusConfig config = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("1.2.3")
    .payloadChannel("examplemod", "server_version")
    .messages(ModStatusMessages.defaults())
    .build();

ModStatusSnapshot snapshot = ModStatusKit.disconnected();
ModStatusDisplay display = ModStatusKit.display(config, snapshot);
```

Exact names can change during implementation planning, but the API shape should stay small: provide config, update state, ask for display data.

For consuming mods that want less repeated glue, a reusable client state holder should manage the current snapshot without subscribing to Fabric events itself:

```java
ModStatusClientState status = ModStatusClientState.create(config);

status.unknown();
status.connected("1.2.3");
status.markServerNotDetectedIfUnknown();
status.disconnected();

ModStatusDisplay display = status.display();
```

## Embedded and Relocated Library Strategy

ModStatusKit should be consumed as source, a local jar, or a shaded dependency and relocated into the consuming mod's internal namespace. This avoids collisions when multiple mods embed different ModStatusKit copies in the same Minecraft process.

Each consuming mod must relocate ModStatusKit under a unique mod-specific package root. Do not relocate multiple consumers to the same shared package such as `cloud.explosive.modstatuskit.shadow`; that would reintroduce classpath collisions when different embedded versions are installed together. A safe pattern is `<consumer.package>.internal.modstatus` or `<consumer.package>.shadow.modstatuskit`.

Recommended relocated package examples:

- `cloud.explosive.carrybabyanimals.internal.modstatus`
- `cloud.explosive.signport.internal.modstatus`
- `cloud.explosive.multigolem.internal.modstatus`

The core package in this repository can use a neutral namespace such as `dev.jasmine.modstatuskit`. Build documentation should explain that consuming mods should relocate it under their own package root before shipping.

## Version Comparison States

The reusable status model should derive one of these states:

- `MATCHED`: client and server versions are both known and equal.
- `DIFFERENT`: client and server versions are both known and not equal.
- `DISCONNECTED`: the client is not connected to a server or local world.
- `SERVER_NOT_DETECTED`: connected, and the consuming mod's detection window has elapsed without seeing the expected server-side channel or version payload.
- `UNKNOWN`: connected, but server status/version has not been received yet and detection is still pending.

Display tones:

- `MATCHED` uses green.
- `DIFFERENT` uses orange.
- `DISCONNECTED`, `SERVER_NOT_DETECTED`, and `UNKNOWN` use gray.

Required behavior:

- Client older than server: orange `Different versions`.
- Client newer than server: orange `Different versions`.
- Client and server same: green `Matched`.
- Client not connected to a server/world: gray `Disconnected`.
- Connected to a server without the consuming server mod installed: gray `Server not detected` or equivalent configured wording.
- Connected but version payload not received: gray `Unknown`.

No semantic version ordering is required for v1. Any unequal known versions are simply different.

## Connection Lifecycle

The consuming mod should clear the server version state on disconnect. On reconnect, it should start from `UNKNOWN` until capability detection and payload exchange complete or a bounded detection window elapses.

The core library should model state transitions but should not directly subscribe to Minecraft lifecycle events. Consuming mods remain responsible for calling the API or `ModStatusClientState` at the right moments:

- disconnected before joining a world or server
- unknown immediately after connecting, before payload information is available and while detection is still pending
- server not detected when capability checks show the server-side mod/channel is absent, or when the consuming mod's bounded detection window elapses without receiving the expected payload
- server version received when a valid version payload arrives
- disconnected again after leaving the world/server

The core library should not choose the detection timeout. Each consuming mod should pick a small, documented bound that fits its connection flow, then call `markServerNotDetectedIfUnknown()` when that window expires.

## Payload Model

Each consuming mod owns its own payload channel. Examples:

- `carrybabyanimals:server_version`
- `signport:server_version`
- `multigolem:server_version`

The payload should be small. For v1, a server version string is enough. If future compatibility requires it, the payload can include a protocol/status version and display version separately.

ModStatusKit should provide dependency-free UTF-8 helpers for this small payload:

```java
byte[] payload = ModStatusVersionPayload.encodeServerVersion(config.clientVersion());
String serverVersion = ModStatusVersionPayload.decodeServerVersion(payload);
```

Networking must be optional and capability-gated. The recommended v1 direction is server-to-client version reporting: the consuming client registers its own receiver for its mod-specific channel, and the consuming server sends only after Fabric's negotiated receiver/channel availability check, such as `ServerPlayNetworking.canSend(player, channel)`, confirms that the client can receive that payload. Vanilla or unmodded clients must receive no custom payloads they cannot understand.

ModStatusKit can help with the repeated send decision without importing Fabric classes:

```java
ModStatusVersionPayload.sendServerVersionIfSupported(
    config,
    channel -> canSendPlayerPayload(player, channel),
    (channel, payload) -> sendPlayerPayload(player, channel, payload)
);
```

Because each mod owns a separate channel and embeds a relocated copy of the library, CarryBabyAnimals, SignPort, and MultiGolem can all be installed on the same client/server without sharing mutable status state or colliding on payload IDs.

## UI and Status Section Model

ModStatusKit should not depend on ModMenu in the core. ModMenu integration is implemented by each consuming mod; ModStatusKit core provides no ModMenu dependency or adapter. The agreed direction for this phase is to keep UI integration at the raw display-model level, not add UI helper packages.

The display model contains:

- display name
- client version text
- server version text
- status label
- status tone
- optional help text
- optional update URL

The consuming mod decides whether to render that model in ModMenu, a custom config screen, another UI surface, or nowhere. If ModMenu is not installed on the client, the consuming mod should simply skip its ModMenu UI entrypoint or status section. ModStatusKit should not make ModMenu mandatory.

Optional UI helper packages are intentionally out of scope for this phase. The library should not force a visual layout, status screen, badge, row, or tone mapping beyond the raw `ModStatusDisplay` fields.

## Enforcement Policy Model

ModStatusKit itself is passive. It reports status and provides display data; it does not kick players, disconnect clients, block login, or gate gameplay.

A consuming mod may choose to enforce matching versions or require a server-side companion mod. That policy belongs to the consuming mod. If a consuming mod intentionally disconnects a player because of mismatch or missing server/client support, it should provide a clear, mod-specific disconnect reason instead of relying on vague vanilla failure text.

Example disconnect reason copy:

```text
MultiGolem version mismatch. Please install the matching client version.
```

## Custom Message Model

The library should provide default labels/messages, while allowing each consuming mod to override the short status/help copy.

CarryBabyAnimals mismatch example:

```text
Different versions may miss or hide new features. Gameplay remains compatible.
```

SignPort and MultiGolem should be able to provide equivalent wording without changing library code.

The custom message model should cover at least:

- matched
- different
- disconnected
- unknown
- server not detected

## Testing Strategy

The first implementation should focus tests on pure Java logic:

- matching versions produce `MATCHED` and green tone
- unequal known versions produce `DIFFERENT` and orange tone
- disconnected state produces `DISCONNECTED` and gray tone
- server-not-detected state produces `SERVER_NOT_DETECTED` and gray tone
- connected-without-payload state produces `UNKNOWN` and gray tone
- reconnect/disconnect flows can clear server state when the consuming mod calls the appropriate API
- custom messages override defaults without changing status comparison

Fabric callback registration and ModMenu rendering should be tested in consuming mods later. Dependency-free state and payload helpers should be tested in ModStatusKit core.

## Example Consumers

### CarryBabyAnimals

CarryBabyAnimals embeds and relocates ModStatusKit to:

```text
cloud.explosive.carrybabyanimals.internal.modstatus
```

It owns the payload channel:

```text
carrybabyanimals:server_version
```

Its ModMenu/config UI can render the status model and use custom mismatch text:

```text
Different versions may miss or hide new features. Gameplay remains compatible.
```

### SignPort

SignPort embeds and relocates ModStatusKit to:

```text
cloud.explosive.signport.internal.modstatus
```

It owns the payload channel:

```text
signport:server_version
```

Its UI can provide SignPort-specific wording while using the same core states and tones.

### MultiGolem

MultiGolem embeds and relocates ModStatusKit to:

```text
cloud.explosive.multigolem.internal.modstatus
```

It owns the payload channel:

```text
multigolem:server_version
```

Its UI can provide MultiGolem-specific wording while using the same core states and tones.

## Initial Project Shape

The initial repository should contain:

- `README.md`
- `AGENTS.md`
- this design spec

After this design is approved, the implementation plan should add a tiny Java core scaffold with tests. A likely source shape is:

- `src/main/java/dev/jasmine/modstatuskit/ModStatusKit.java`
- `src/main/java/dev/jasmine/modstatuskit/ModStatusConfig.java`
- `src/main/java/dev/jasmine/modstatuskit/ModStatusMessages.java`
- `src/main/java/dev/jasmine/modstatuskit/ModStatusSnapshot.java`
- `src/main/java/dev/jasmine/modstatuskit/ModStatusDisplay.java`
- `src/main/java/dev/jasmine/modstatuskit/ModStatusClientState.java`
- `src/main/java/dev/jasmine/modstatuskit/ModStatusVersionPayload.java`
- `src/main/java/dev/jasmine/modstatuskit/VersionStatus.java`
- `src/main/java/dev/jasmine/modstatuskit/StatusTone.java`
- `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`

The scaffold should avoid Fabric dependencies. Optional integration helpers should remain dependency-free by accepting consuming-mod callbacks instead of importing Fabric classes.

## Acceptance Criteria

- ModStatusKit can be embedded and relocated into multiple mods at the same time.
- The core library has no hardcoded consumer mod IDs.
- The core status logic is testable without Minecraft, Fabric, or ModMenu.
- Dependency-free client state and payload helpers reduce repeated Fabric integration glue.
- Known equal versions are green/matched.
- Known unequal versions are orange/different.
- disconnected, server-not-detected, and unknown states are gray.
- Server state is clearable on disconnect and refreshable on reconnect through consuming-mod calls.
- Networking remains optional and capability-gated in consuming mods.
- The v1 design remains informational only.
