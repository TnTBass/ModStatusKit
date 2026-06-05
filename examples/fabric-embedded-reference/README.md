# Fabric Embedded Reference

This is a copy/paste-oriented reference for Fabric mods that embed ModStatusKit into their own jar.

The intended pattern is:

- embed and relocate ModStatusKit into the consuming mod's internal package
- keep ModStatusKit out of the player's required mod list
- use a dedicated status payload, separate from gameplay packets
- send that payload only when Fabric says the client can receive it
- let the consuming mod own the UI and render `ModStatusDisplay` in its own style
- treat missing, old, or different-version servers as passive information by default

These snippets are intentionally not a full runnable mod. Copy the pieces into your mod, update every `// CHANGE:` comment, and wire them into your existing Fabric entrypoints.

## Files

- `ExampleModStatus.java`: shared config plus small client-side status state.
- `BuildInfo.java`: example generated build metadata constant consumed by `ExampleModStatus`.
- `ExampleModStatusNetworking.java`: server payload registration, capability-gated send helper, and join hook.
- `ExampleModStatusClient.java`: client payload registration plus join/disconnect/tick timeout behavior.
- `ExampleModStatusUiSnippet.java`: optional ModMenu entrypoint plus consuming-mod-owned UI rendering from `ModStatusDisplay`.

## Build Metadata

Keep the consuming mod's public version and jar filename stable, such as `yourmod-1.2.3.jar`, unless you intentionally want build metadata in public file names. Stamp build metadata into the jar as generated source or another build-time value, then pass it to ModStatusKit:

```java
.clientVersion(CURRENT_VERSION)
.clientBuild(BuildInfo.GIT_COMMIT)
```

The `BuildInfo.java` file in this reference is a placeholder for generated consuming-mod integration code. In a real mod, generate it from Gradle/CI using sources such as `-PbuildNumber`, `GITHUB_SHA`, `GITHUB_RUN_NUMBER`, or `git rev-parse --short HEAD`.

Render the base version as the primary player-facing version. Show build metadata only as optional diagnostic detail, such as `build abc1234` or `1.2.3+abc1234`. If the build value is missing, do not show placeholder text. If a local fallback such as `dev` is generated, hide it in normal player-facing UI unless you deliberately want to expose local build labels.

For color, keep a build mismatch quieter than a version mismatch. Use green when base versions match and build metadata is absent or equal. If both client and server builds are present and differ, a blue or teal accent works well as diagnostic information: different build, but not necessarily a concern. Keep orange for public version mismatch and gray for disconnected, unknown, or server-not-detected states.

## Relocate ModStatusKit

Relocate the library package from:

```text
cloud.explosive.modstatuskit
```

to an internal package owned by your mod, for example:

```text
com.example.yourmod.internal.modstatus
```

Your final jar should contain only the relocated/internal ModStatusKit classes. Do not expose the original shared `cloud.explosive.modstatuskit` package from the consuming mod jar.

For a Shadow-style build, the important idea is:

```groovy
// CHANGE: adapt this to your build. This is illustrative, not a complete Gradle file.
shadowJar {
    relocate "cloud.explosive.modstatuskit", "com.example.yourmod.internal.modstatus"
}
```

After relocation, your Java imports should use your internal package:

```java
// CHANGE: use your relocated ModStatusKit package.
import com.example.yourmod.internal.modstatus.ModStatusConfig;
import com.example.yourmod.internal.modstatus.ModStatusDisplay;
import com.example.yourmod.internal.modstatus.ModStatusKit;
import com.example.yourmod.internal.modstatus.ModStatusMessages;
import com.example.yourmod.internal.modstatus.ModStatusSnapshot;
import com.example.yourmod.internal.modstatus.ModStatusVersionPayload;
import com.example.yourmod.internal.modstatus.VersionStatus;
```

## ModMenu Is Optional

This reference shows the CarryBabyAnimals-style UI path: an optional client-side ModMenu integration that renders a small colored indicator dot, a status label, and hover/help text from `ModStatusDisplay`.

Keep ModMenu optional. Use `compileOnly` or your loader's equivalent, put the entrypoint in client-only metadata, and do not make ModMenu a runtime dependency for gameplay or networking. If ModMenu is absent, the mod should still load and play normally; only this status UI is unavailable.

## Verification Checklist

- Final jar contains relocated/internal ModStatusKit classes only.
- No unrelocated shared ModStatusKit package is exposed.
- Vanilla clients do not receive custom payloads.
- Older servers do not disconnect clients.
- ModMenu absence does not break the mod.
- Status does not depend on gameplay packets.
- Mismatch policy, if any, is explicit and player-facing.
