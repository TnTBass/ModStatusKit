# Optional Build Metadata Design

## Goal

ModStatusKit should optionally distinguish builds that share the same public mod version, such as multiple jars labeled `0.4.0`, without making build identity required or noisy.

If build metadata is absent, unsupported, or not supplied by a consuming mod, ModStatusKit should behave as it does today.

## Supported Inputs

Mod writers may provide build metadata in either of these supported forms:

- Inline SemVer build metadata: `0.4.0+abc1234`
- Explicit build metadata from generated build info: version `0.4.0` plus build `abc1234`

Only the SemVer `+build` form is parsed from version strings. Other formats, including `0.4.0-abc1234`, remain ordinary version text and do not create separate build metadata.

## Normalization

ModStatusKit should normalize supported inputs into:

- base version: the user-facing compatibility version, such as `0.4.0`
- optional build identifier: the diagnostic build value, such as `abc1234`

These should be equivalent:

```java
.clientVersion("0.4.0+abc1234")
```

```java
.clientVersion("0.4.0")
.clientBuild("abc1234")
```

If both inline and explicit build metadata are supplied, the explicit value should win because it came from the mod writer's build integration intentionally.

## Status Behavior

The normal matched/different status should compare base versions by default. A client and server that are both `0.4.0` should remain matched even if one side has no build metadata.

When both sides provide build metadata and the build identifiers differ, ModStatusKit should expose that diagnostic difference for display or support tooling. It should not turn the main status orange by default unless a future configuration option explicitly asks for strict build matching.

## Payload Behavior

The server payload should support both:

- a single version string that may contain inline build metadata, such as `0.4.0+abc1234`
- separate version and build fields for mods that generate build metadata during Gradle or CI builds

Existing one-string payload usage should remain valid.

## Display Behavior

Build identifiers should appear only when present. If no build metadata exists, no extra placeholder or unknown build text should appear.

Recommended display treatment:

- main version text: `0.4.0`
- optional detail text: `build abc1234`

This keeps the feature helpful for diagnostics without making normal players worry about internal build details.

## Build Source Responsibility

ModStatusKit should not try to discover Git information at runtime. Consuming mods are responsible for stamping build metadata into their jar during Gradle or CI builds, then passing that value to ModStatusKit.

Common build sources include:

- `git rev-parse --short HEAD` executed by Gradle
- CI-provided commit values such as `GITHUB_SHA`
- generated Java constants
- generated properties resources
- expanded metadata in `fabric.mod.json`

## Testing

Tests should cover:

- plain versions continue to match as before
- inline `+build` metadata is parsed into version plus build
- explicit build metadata is accepted
- explicit build metadata overrides inline metadata
- unsupported version formats are left alone
- missing build metadata produces no display placeholder
- build mismatch is diagnostic only by default
