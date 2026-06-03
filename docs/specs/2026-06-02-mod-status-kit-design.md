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
- Let each consuming mod customize short status/help messages.
- Provide pure comparison/state logic that can be tested without Minecraft or Fabric.

## Non-Goals

- Do not make ModStatusKit a required player-installed mod.
- Do not make a standalone Fabric mod artifact the first target.
- Do not build a combined multi-mod status dashboard in v1.
- Do not duplicate ModMenu's general update checker.
- Do not enforce version matching.
- Do not kick, disconnect, block login, or gate gameplay.
- Do not add automatic login nags in v1.
- Do not hardcode CarryBabyAnimals, SignPort, or MultiGolem into the core library.

## Consuming-Mod Model

Each consuming mod embeds ModStatusKit and calls a tiny API from its own client/config code. The mod owns all Minecraft-specific integration, including how it discovers connection lifecycle events, how it registers payloads, and how it renders the display model.

The core API should accept a configuration object with:

- `modId`
- `displayName`
- `clientVersion`
- optional `updateUrl`
- payload channel namespace
- payload channel path
- custom status messages
- optional protocol or status version for future compatibility

The core API should expose pure model objects and helper methods, not Fabric entrypoints. A consuming mod should be able to do the equivalent of:

```java
ModStatusConfig config = ModStatusConfig.builder()
    .modId("examplemod")
    .displayName("Example Mod")
    .clientVersion("1.2.3")
    .payloadChannel("examplemod", "server_version")
    .messages(ModStatusMessages.defaults())
    .build();

ModStatusSnapshot snapshot = ModStatusKit.disconnected(config);
ModStatusDisplay display = ModStatusKit.display(config, snapshot);
```

Exact names can change during implementation planning, but the API shape should stay small: provide config, update state, ask for display data.

## Embedded and Relocated Library Strategy

ModStatusKit should be consumed as source, a local jar, or a shaded dependency and relocated into the consuming mod's internal namespace. This avoids collisions when multiple mods embed different ModStatusKit copies in the same Minecraft process.

Each consuming mod must relocate ModStatusKit under a unique mod-specific package root. Do not relocate multiple consumers to the same shared package such as `dev.jasmine.modstatuskit.shadow`; that would reintroduce classpath collisions when different embedded versions are installed together. A safe pattern is `<consumer.package>.internal.modstatus` or `<consumer.package>.shadow.modstatuskit`.

Recommended relocated package examples:

- `dev.jasmine.carrybabyanimals.internal.modstatus`
- `dev.jasmine.signport.internal.modstatus`
- `dev.jasmine.multigolem.internal.modstatus`

The core package in this repository can use a neutral namespace such as `dev.jasmine.modstatuskit`. Build documentation should explain that consuming mods should relocate it before shipping.

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

The core library should model state transitions but should not directly subscribe to Minecraft lifecycle events. Consuming mods remain responsible for calling the API at the right moments:

- disconnected before joining a world or server
- unknown immediately after connecting, before payload information is available and while detection is still pending
- server not detected when capability checks show the server-side mod/channel is absent, or when the consuming mod's bounded detection window elapses without receiving the expected payload
- server version received when a valid version payload arrives
- disconnected again after leaving the world/server

The core library should not choose the detection timeout. Each consuming mod should pick a small, documented bound that fits its connection flow, then pass the resulting state into ModStatusKit.

## Payload Model

Each consuming mod owns its own payload channel. Examples:

- `carrybabyanimals:server_version`
- `signport:server_version`
- `multigolem:server_version`

The payload should be small. For v1, a server version string is enough. If future compatibility requires it, the payload can include a protocol/status version and display version separately.

Networking must be optional and capability-gated. The recommended v1 direction is server-to-client version reporting: the consuming client registers its own receiver for its mod-specific channel, and the consuming server sends only after Fabric's negotiated receiver/channel availability check, such as `ServerPlayNetworking.canSend(player, channel)`, confirms that the client can receive that payload. Vanilla or unmodded clients must receive no custom payloads they cannot understand.

Because each mod owns a separate channel and embeds a relocated copy of the library, CarryBabyAnimals, SignPort, and MultiGolem can all be installed on the same client/server without sharing mutable status state or colliding on payload IDs.

## ModMenu and Status Section Model

ModStatusKit should not depend on ModMenu in the core. ModMenu integration is implemented by each consuming mod; ModStatusKit core provides no ModMenu dependency or adapter. It should return a display model with:

- display name
- client version text
- server version text
- status label
- status tone
- optional help text
- optional update URL

The consuming mod decides whether to render that model in ModMenu. If ModMenu is not installed, the consuming mod should simply skip its ModMenu UI entrypoint or status section. ModStatusKit should not make ModMenu mandatory.

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

Fabric networking and ModMenu rendering should be tested in consuming mods later, not in the core v1 library.

## Example Consumers

### CarryBabyAnimals

CarryBabyAnimals embeds and relocates ModStatusKit to:

```text
dev.jasmine.carrybabyanimals.internal.modstatus
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
dev.jasmine.signport.internal.modstatus
```

It owns the payload channel:

```text
signport:server_version
```

Its UI can provide SignPort-specific wording while using the same core states and tones.

### MultiGolem

MultiGolem embeds and relocates ModStatusKit to:

```text
dev.jasmine.multigolem.internal.modstatus
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
- `src/main/java/dev/jasmine/modstatuskit/VersionStatus.java`
- `src/main/java/dev/jasmine/modstatuskit/StatusTone.java`
- `src/test/java/dev/jasmine/modstatuskit/ModStatusKitTest.java`

The scaffold should avoid Fabric dependencies unless a later approved plan adds a thin optional adapter package.

## Acceptance Criteria

- Players install only the consuming mod, not ModStatusKit.
- ModStatusKit can be embedded and relocated into multiple mods at the same time.
- The core library has no hardcoded consumer mod IDs.
- The core status logic is testable without Minecraft, Fabric, or ModMenu.
- Known equal versions are green/matched.
- Known unequal versions are orange/different.
- disconnected, server-not-detected, and unknown states are gray.
- Server state is clearable on disconnect and refreshable on reconnect through consuming-mod calls.
- Networking remains optional and capability-gated in consuming mods.
- The v1 design remains informational only.
