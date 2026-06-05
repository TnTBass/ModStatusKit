# CBA-Inspired Fabric Reference Design

## Goal

Improve `examples/fabric-embedded-reference` so mod writers can copy a realistic Fabric integration pattern modeled on the quality of Carry Baby Animals' ModStatusKit usage, while preserving ModStatusKit core as a dependency-free Java library.

## Context

ModStatusKit already provides dependency-free core APIs for:

- `ModStatusConfig` and custom messages.
- `ModStatusClientState` for connection lifecycle state.
- `ModStatusVersionPayload` for legacy and structured status payloads.
- `ModStatusDisplay` and `StatusTone` for UI-ready display data.
- `VersionMismatchSeverity` for passive `WARN` versus explicit `BREAKING` mismatch tone.

The current embedded Fabric reference explains the right boundary, but the UI sample is still more of a sketch than a real integration surface. Carry Baby Animals demonstrates a better pattern:

- A mod-owned status config and message catalog.
- A client tracker separate from UI code.
- Capability-gated server status payload sending.
- Client join, disconnect, and tick timeout state transitions.
- An optional ModMenu entrypoint that opens the consuming mod's config screen.
- A small status indicator in that screen with tone color, hover/help text, client/server versions, and optional build metadata.

## Scope

This work changes the ModStatusKit repository only, inside the assigned worktree:

- `examples/fabric-embedded-reference`
- README/example docs
- focused local checks or tests for example helper behavior if practical
- design and implementation plan artifacts

Existing files under `examples/fabric-embedded-reference` should be selectively updated or replaced so the final directory reads as one coherent reference. Do not preserve stale sketch code beside the improved example just to minimize diff size.

This work does not change Carry Baby Animals, MultiGolem, SignPort, Minecraft-Docker, or any other repo.

## Non-Goals

- Do not add Fabric, Loom, Minecraft, or ModMenu dependencies to ModStatusKit core.
- Do not make ModStatusKit register Fabric callbacks, payloads, ModMenu screens, or lifecycle hooks.
- Do not change mismatch semantics:
  - base/public version match with different builds remains matched status with teal diagnostic tone
  - default or `WARN` public version mismatch remains orange
  - `BREAKING` public version mismatch may render red
  - disconnected, unknown, and server-not-detected states remain gray
- Do not publish, release, tag, or push.

## Recommended Approach

Use a richer example/docs-only implementation unless Revue or implementation reveals a concrete dependency-free helper gap. The reference should be shaped like a consuming mod integration, not like core library code.

The example should show these responsibilities as separate units:

- `ExampleModStatus`: shared mod-owned config, messages, constants, and current status state.
- `ExampleModStatusClient`: Fabric client registration, join/disconnect/tick lifecycle calls, and client payload receiver.
- `ExampleModStatusNetworking`: Fabric server payload registration and capability-gated status sending.
- `ExampleModStatusUiSnippet`: optional ModMenu entrypoint plus a consuming-mod-owned status row/helper that renders `ModStatusDisplay`.
- `BuildInfo`: generated build metadata placeholder.
- Example README: copy/paste guidance, relocation guidance, optional ModMenu guidance, state/tone examples, and verification checklist.

## Reference UI Shape

The UI reference should demonstrate a Carry Baby Animals-style status surface:

- A small colored dot or compact indicator in a config screen.
- Tooltip/help text built from `ModStatusDisplay`.
- Client and server base versions.
- Optional build metadata shown as `version+build` only when a real build is present.
- Local fallback build values such as `dev` hidden from normal player-facing UI.
- Optional update link where the consuming mod wants one.

The example should provide named helpers that are easy to lift:

- `toneColor(StatusTone tone) -> int` for ARGB green, teal, orange, red, and gray colors.
- `tooltipText(ModStatusDisplay display) -> List<String>` for compact hover text before converting to Minecraft text components.
- `versionWithBuild(String version, String build) -> String` for build metadata display.
- sample display objects or sample comments that show green, teal, orange, red, and gray states.

These helpers belong in the reference example, not in core, because UI formatting and colors are consuming-mod policy.

## ModMenu Boundary

The docs and example must say clearly:

- ModMenu is optional.
- The reference uses ModMenu as one good integration pattern because many Fabric mod writers already expose client config there.
- ModMenu should be `compileOnly` or otherwise optional.
- ModMenu should live in client-only metadata.
- If ModMenu is absent, gameplay, networking, and ModStatusKit use still work; only that UI path is unavailable.

## Networking Boundary

The example should preserve capability-gated server-to-client status payload sending:

- Register the consuming mod's own status payload type in Fabric integration code.
- Send only after the Fabric capability check confirms the client can receive it.
- Use the existing dependency-free core API `ModStatusVersionPayload.sendServerStatusIfSupported(ModStatusConfig, VersionMismatchSeverity, PayloadSupport, PayloadSender)` for structured payloads.
- Keep the Fabric capability check example-owned by passing lambdas for `PayloadSupport` and `PayloadSender`; core receives only a channel string and payload bytes.
- Use `VersionMismatchSeverity.WARN` by default.
- Use `VersionMismatchSeverity.BREAKING` only when the consuming mod explicitly wants a red public-version incompatibility indicator.
- Keep status payloads separate from gameplay packets.

## Build Metadata Boundary

The current build metadata stamping guidance remains valid and must be preserved:

- ModStatusKit does not discover Git/runtime build data.
- Mod writers stamp build metadata through Gradle or CI.
- The public jar version and filename should usually stay stable.
- Build metadata is diagnostic display/support data, not a compatibility criterion.
- Missing build metadata should not display placeholder text.
- Local fallback values such as `dev` should be hidden in normal player-facing UI unless the consuming mod deliberately exposes them.

## Tests And Checks

No Java core behavior change is expected. If implementation changes only examples/docs, core tests still run for regression protection.

Focused checks should cover example logic where it can compile without Minecraft/Fabric dependencies. The preferred option is a small script that compiles and runs dependency-free example helpers if those helpers are split into a plain Java class, or a text/source check that validates the reference contains:

- The exact phrase `ModMenu is optional`.
- `compileOnly` guidance for the optional ModMenu dependency.
- `VersionMismatchSeverity.WARN` default send path.
- `VersionMismatchSeverity.BREAKING` example or documentation.
- `StatusTone.TEAL`, `StatusTone.ORANGE`, `StatusTone.RED`, and `StatusTone.GRAY` UI mapping.
- `dev` build fallback hiding.

Verification should include:

- `.\scripts\test-java-core.ps1`
- `.\scripts\test-release-gate.ps1`
- any new example/docs check script
- `git diff --check`

## Acceptance Criteria

- `examples/fabric-embedded-reference` reads like a practical consuming-mod integration modeled on Carry Baby Animals' status integration quality.
- Example docs are copy/paste-friendly for mod writers and call out required edits explicitly.
- README/example docs clearly state that ModMenu is optional and not required to use ModStatusKit.
- Core remains dependency-free and runtime-agnostic.
- Current build metadata stamping docs and guidance remain present.
- Current mismatch severity and tone behavior remains unchanged.
- The reference demonstrates green, teal, orange, red, and gray states where practical.
- Revue `design-spec-review`, `implementation-plan-review`, and `implementation-review` gates are completed and valid findings are actioned.
