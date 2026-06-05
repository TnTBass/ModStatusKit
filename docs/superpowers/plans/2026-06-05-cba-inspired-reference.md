# CBA-Inspired Fabric Reference Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Carry Baby Animals-inspired Fabric embedded reference that is copy/paste-friendly for mod writers while preserving ModStatusKit core as dependency-free Java.

**Architecture:** Keep core unchanged by default. Improve the reference by separating consuming-mod responsibilities into status config/state, Fabric networking, optional ModMenu UI, dependency-free display helpers, and docs/checks that prove the reference still demonstrates the required state colors and boundaries.

**Tech Stack:** Java reference snippets, PowerShell source checks, dependency-free ModStatusKit core tests.

---

## File Structure

- Modify `examples/fabric-embedded-reference/ExampleModStatus.java`: keep shared consuming-mod config/state, clarify message copy, and expose lifecycle methods used by client integration.
- Modify `examples/fabric-embedded-reference/ExampleModStatusClient.java`: keep client payload receiver and join/disconnect/tick callbacks as the Fabric-owned lifecycle bridge.
- Modify `examples/fabric-embedded-reference/ExampleModStatusNetworking.java`: keep server payload registration and capability-gated status send using existing dependency-free core payload helpers.
- Modify `examples/fabric-embedded-reference/ExampleModStatusUiSnippet.java`: make the optional ModMenu/config screen example render a compact square status indicator and delegate status formatting to a focused helper.
- Create `examples/fabric-embedded-reference/ExampleModStatusDisplay.java`: dependency-free display helper for tone colors, tooltip text, build formatting, and sample states.
- Modify `examples/fabric-embedded-reference/README.md`: explain optional ModMenu, relocation, build metadata, state colors, WARN/BREAKING behavior, and how to copy the reference.
- Modify `README.md`: point mod writers at the improved reference and restate that ModMenu is optional.
- Create `scripts/check-fabric-reference.ps1`: source-level reference check for required phrases, enums, helpers, and optional ModMenu guidance.

## Task 1: Add Reference Source Check First

**Files:**
- Create: `scripts/check-fabric-reference.ps1`

- [ ] **Step 1: Write the check script**

Create `scripts/check-fabric-reference.ps1`:

```powershell
$ErrorActionPreference = "Stop"

function Require-Text {
    param(
        [string] $Path,
        [string] $Pattern,
        [string] $Label
    )

    $content = Get-Content -Raw -Path $Path
    if ($content -notmatch $Pattern) {
        throw "Missing ${Label} in ${Path}: ${Pattern}"
    }
}

Require-Text "examples\fabric-embedded-reference\README.md" "ModMenu is optional" "optional ModMenu wording"
Require-Text "examples\fabric-embedded-reference\README.md" "compileOnly" "optional ModMenu compileOnly guidance"
Require-Text "examples\fabric-embedded-reference\README.md" "VersionMismatchSeverity\.BREAKING" "breaking severity guidance"
Require-Text "examples\fabric-embedded-reference\README.md" "StatusTone\.TEAL" "teal build mismatch guidance"
Require-Text "examples\fabric-embedded-reference\README.md" "green, teal, orange, red, and gray" "all reference tones"
Require-Text "examples\fabric-embedded-reference\README.md" "compact square status indicator" "square status indicator guidance"
Require-Text "examples\fabric-embedded-reference\README.md" "square status box" "square status box guidance"

Require-Text "examples\fabric-embedded-reference\ExampleModStatusNetworking.java" "VersionMismatchSeverity\.WARN" "default WARN send path"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusNetworking.java" "sendServerStatusIfSupported" "structured status send helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "static int toneColor\(StatusTone tone\)" "tone color helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "static List<String> tooltipText\(ModStatusDisplay display\)" "tooltip helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "static String versionWithBuild\(String version, String build\)" "build display helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "`"dev`"\.equalsIgnoreCase\(build\)" "dev build fallback hiding"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "STATUS_SQUARE_SIZE" "status square size constant"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "STATUS_SQUARE_BORDER_COLOR" "status square border constant"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case GREEN -> 0xFF55FF55" "green color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case TEAL -> 0xFF55FFFF" "teal color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case ORANGE -> 0xFFFFAA00" "orange color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case RED -> 0xFFFF5555" "red color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case GRAY -> 0xFFAAAAAA" "gray color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusUiSnippet.java" "statusSquare" "status square UI hook"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusUiSnippet.java" "square status box" "status box UI guidance"

Write-Output "Fabric embedded reference checks passed."
```

- [ ] **Step 2: Run the new check to verify it fails before implementation**

Run: `.\scripts\check-fabric-reference.ps1`

Expected: FAIL because the reference does not yet contain the square status indicator language and `statusSquare` UI hook.

## Task 2: Add Display Helper And Update Optional UI Snippet

**Files:**
- Create: `examples/fabric-embedded-reference/ExampleModStatusDisplay.java`
- Modify: `examples/fabric-embedded-reference/ExampleModStatusUiSnippet.java`

- [ ] **Step 1: Add the display helper**

Create `ExampleModStatusDisplay.java` with:

```java
package com.example.yourmod;

import java.util.ArrayList;
import java.util.List;

// CHANGE: import your relocated/internal ModStatusKit package.
import com.example.yourmod.internal.modstatus.ModStatusDisplay;
import com.example.yourmod.internal.modstatus.StatusTone;

/**
 * Consuming-mod-owned display helpers for turning ModStatusKit display data
 * into the colors and strings your UI renders.
 */
public final class ExampleModStatusDisplay {
    private ExampleModStatusDisplay() {
    }

    static int toneColor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> 0xFF55FF55;
            case TEAL -> 0xFF55FFFF;
            case ORANGE -> 0xFFFFAA00;
            case RED -> 0xFFFF5555;
            case GRAY -> 0xFFAAAAAA;
        };
    }

    static List<String> tooltipText(ModStatusDisplay display) {
        List<String> lines = new ArrayList<>();
        lines.add(display.displayName());
        lines.add("Status: " + display.statusLabel());
        lines.add("Client: " + versionWithBuild(display.clientVersion(), display.clientBuild()));
        lines.add("Server: " + versionWithBuild(display.serverVersion(), display.serverBuild()));
        if (!display.helpText().isEmpty()) {
            lines.add(display.helpText());
        }
        return lines;
    }

    static String versionWithBuild(String version, String build) {
        return build == null || "dev".equalsIgnoreCase(build) ? version : version + "+" + build;
    }
}
```

- [ ] **Step 2: Update the UI snippet to use the helper**

Modify `ExampleModStatusUiSnippet.java` so `renderStatusRow` uses `ExampleModStatusDisplay.toneColor(display.tone())`, `ExampleModStatusDisplay.tooltipText(display)`, and the compact square status indicator hook.

Use this replacement shape for the status row:

```java
public static void renderStatusRow(YourUiWriter ui) {
    ModStatusDisplay display = ModStatusKit.display(
            ExampleModStatus.CONFIG,
            ExampleModStatus.CLIENT_STATE.snapshot());
    int statusColor = ExampleModStatusDisplay.toneColor(display.tone());
    List<String> tooltip = ExampleModStatusDisplay.tooltipText(display);

    ui.statusSquare(
            statusColor,
            ExampleModStatusDisplay.STATUS_SQUARE_BORDER_COLOR,
            ExampleModStatusDisplay.STATUS_SQUARE_SIZE,
            tooltip);
    ui.text(display.statusLabel(), statusColor, tooltip);
    ui.text("Client: " + ExampleModStatusDisplay.versionWithBuild(display.clientVersion(), display.clientBuild()));
    ui.text("Server: " + ExampleModStatusDisplay.versionWithBuild(display.serverVersion(), display.serverBuild()));

    if (!display.updateUrl().isEmpty()) {
        ui.link("Updates", display.updateUrl());
    }
}
```

Keep the ModMenu imports in this file only and preserve the comment that ModMenu should be compile-only/optional.

- [ ] **Step 3: Run the reference check**

Run: `.\scripts\check-fabric-reference.ps1`

Expected: still FAIL until docs contain the new exact guidance.

## Task 3: Tighten Networking And State Example

**Files:**
- Modify: `examples/fabric-embedded-reference/ExampleModStatus.java`
- Modify: `examples/fabric-embedded-reference/ExampleModStatusClient.java`
- Modify: `examples/fabric-embedded-reference/ExampleModStatusNetworking.java`

- [ ] **Step 1: Preserve client tracker shape**

Keep `ExampleModStatus.CLIENT_STATE`, `onClientJoin`, `onServerStatus`, `onClientDisconnect`, and `tick` as the consuming-mod-owned tracker. Ensure the timeout comment says this moves to passive `SERVER_NOT_DETECTED`, not a disconnect or gameplay gate.

- [ ] **Step 2: Clarify server send behavior**

In `ExampleModStatusNetworking.sendServerVersionIfSupported`, keep:

```java
ModStatusVersionPayload.sendServerStatusIfSupported(
        ExampleModStatus.CONFIG,
        VersionMismatchSeverity.WARN,
        channel -> true,
        (channel, payload) -> ServerPlayNetworking.send(player, new ServerVersionPayload(payload)));
```

Also keep the preceding `ServerPlayNetworking.canSend(player, ServerVersionPayload.TYPE)` guard as the Fabric capability check.

- [ ] **Step 3: Add explicit BREAKING guidance comment**

Near the WARN send path, include:

```java
// CHANGE: use VersionMismatchSeverity.BREAKING only when a public/base version
// mismatch is truly incompatible for your mod. Build mismatch stays diagnostic.
```

- [ ] **Step 4: Run the reference check**

Run: `.\scripts\check-fabric-reference.ps1`

Expected: still FAIL until README guidance is updated.

## Task 4: Update Example And Root Documentation

**Files:**
- Modify: `examples/fabric-embedded-reference/README.md`
- Modify: `README.md`

- [ ] **Step 1: Update example README**

Ensure `examples/fabric-embedded-reference/README.md` includes:

- exact phrase `ModMenu is optional`
- `compileOnly` optional dependency guidance
- `StatusTone.TEAL`, `StatusTone.ORANGE`, `StatusTone.RED`, and `StatusTone.GRAY`
- phrase `green, teal, orange, red, and gray`
- `VersionMismatchSeverity.WARN` as default
- `VersionMismatchSeverity.BREAKING` as explicit red public/base mismatch policy
- build metadata stamping and hiding `dev` fallback
- copy/paste instructions for mod writers
- the compact square status indicator and square status box as the recommended visual pattern

- [ ] **Step 2: Update root README**

Add a short pointer in `README.md` near Render Display Data or Networking Model:

```markdown
For a fuller Fabric example, see `examples/fabric-embedded-reference`. That reference uses optional ModMenu as one UI pattern, but ModMenu is not required to use ModStatusKit.
```

- [ ] **Step 3: Run checks**

Run: `.\scripts\check-fabric-reference.ps1`

Expected: `Fabric embedded reference checks passed.`

## Task 5: Full Verification And Review Prep

**Files:**
- All changed files

- [ ] **Step 1: Run core tests**

Run: `.\scripts\test-java-core.ps1`

Expected: `ModStatusKitTest passed`

- [ ] **Step 2: Run release gate tests**

Run: `.\scripts\test-release-gate.ps1`

Expected: `Release gate tests passed.`

- [ ] **Step 3: Run reference checks**

Run: `.\scripts\check-fabric-reference.ps1`

Expected: `Fabric embedded reference checks passed.`

- [ ] **Step 4: Run whitespace diff check**

Run: `git diff --check`

Expected: no output and exit code 0.

- [ ] **Step 5: Inspect changed files**

Run: `git status --short`

Expected changed files are limited to:

- `README.md`
- `docs/superpowers/specs/2026-06-05-cba-inspired-reference-design.md`
- `docs/superpowers/plans/2026-06-05-cba-inspired-reference.md`
- `examples/fabric-embedded-reference/README.md`
- `examples/fabric-embedded-reference/ExampleModStatus.java`
- `examples/fabric-embedded-reference/ExampleModStatusClient.java`
- `examples/fabric-embedded-reference/ExampleModStatusNetworking.java`
- `examples/fabric-embedded-reference/ExampleModStatusUiSnippet.java`
- `examples/fabric-embedded-reference/ExampleModStatusDisplay.java`
- `scripts/check-fabric-reference.ps1`

## Task 6: Revue Implementation Review And Commit

**Files:**
- All changed files

- [ ] **Step 1: Run `implementation-review`**

Create a Revue implementation review over the final changed file list with these commands recorded:

- `.\scripts\test-java-core.ps1`
- `.\scripts\test-release-gate.ps1`
- `.\scripts\check-fabric-reference.ps1`
- `git diff --check`

- [ ] **Step 2: Action valid findings**

Use `superpowers:receiving-code-review` before changing files for findings. If material changes are made, rerun targeted verification and repeat a targeted Revue follow-up.

- [ ] **Step 3: Final verification before completion**

Use `superpowers:verification-before-completion`, rerun all verification commands, and inspect `git status --short`.

- [ ] **Step 4: Commit locally**

After review and verification are clean:

```powershell
git add README.md docs\superpowers\specs\2026-06-05-cba-inspired-reference-design.md docs\superpowers\plans\2026-06-05-cba-inspired-reference.md examples\fabric-embedded-reference scripts\check-fabric-reference.ps1
git commit -m "Improve Fabric embedded reference"
```

Do not push, tag, publish, or release.

## Self-Review

- Spec coverage: The tasks cover the reference source, UI helper, networking/state examples, docs, verification, Revue review, and local commit.
- Placeholder scan: The plan intentionally avoids unresolved placeholders. `CHANGE:` appears only inside reference snippets where mod writers must edit consuming-mod-specific names.
- Type consistency: Helper signatures match the design spec: `toneColor` returns `int`, `tooltipText` returns `List<String>`, and `versionWithBuild` returns `String`.
