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

Require-Text "examples\fabric-embedded-reference\ExampleModStatusNetworking.java" "VersionMismatchSeverity\.WARN" "default WARN send path"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusNetworking.java" "VersionMismatchSeverity\.BREAKING" "breaking severity source guidance"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusNetworking.java" "sendServerStatusIfSupported" "structured status send helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "static int toneColor\(StatusTone tone\)" "tone color helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "static List<String> tooltipText\(ModStatusDisplay display\)" "tooltip helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "static String versionWithBuild\(String version, String build\)" "build display helper"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "`"dev`"\.equalsIgnoreCase\(build\)" "dev build fallback hiding"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case GREEN -> 0xFF55FF55" "green color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case TEAL -> 0xFF55FFFF" "teal color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case ORANGE -> 0xFFFFAA00" "orange color mapping"
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case RED -> 0xFFFF5555" "red color mapping"
# ModStatusKit intentionally has no UNKNOWN or DISCONNECTED tone; unknown, disconnected,
# and server-not-detected display states all collapse to StatusTone.GRAY.
Require-Text "examples\fabric-embedded-reference\ExampleModStatusDisplay.java" "case GRAY -> 0xFFAAAAAA" "gray color mapping"

Write-Output "Fabric embedded reference checks passed."
