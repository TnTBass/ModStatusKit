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
        String helpText = display.helpText();
        if (helpText != null && !helpText.isEmpty()) {
            lines.add(helpText);
        }
        return lines;
    }

    static String versionWithBuild(String version, String build) {
        if (version == null || version.isBlank()) {
            return "Unknown";
        }
        return build == null || build.isBlank() || "dev".equalsIgnoreCase(build) ? version : version + "+" + build;
    }
}
