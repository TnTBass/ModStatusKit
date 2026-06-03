package dev.jasmine.modstatuskit;

import java.util.Objects;

/**
 * UI-ready status data for the consuming mod to render.
 */
public final class ModStatusDisplay {
    private final String displayName;
    private final String clientVersion;
    private final String serverVersion;
    private final String statusLabel;
    private final String helpText;
    private final StatusTone tone;
    private final String updateUrl;

    public ModStatusDisplay(
            String displayName,
            String clientVersion,
            String serverVersion,
            String statusLabel,
            String helpText,
            StatusTone tone,
            String updateUrl
    ) {
        this.displayName = ModStatusStrings.requireText(displayName, "displayName");
        this.clientVersion = ModStatusStrings.requireText(clientVersion, "clientVersion");
        this.serverVersion = ModStatusStrings.requireText(serverVersion, "serverVersion");
        this.statusLabel = ModStatusStrings.requireText(statusLabel, "statusLabel");
        this.helpText = helpText == null ? "" : helpText.trim();
        this.tone = Objects.requireNonNull(tone, "tone");
        this.updateUrl = ModStatusStrings.optionalText(updateUrl);
    }

    public String displayName() {
        return displayName;
    }

    public String clientVersion() {
        return clientVersion;
    }

    public String serverVersion() {
        return serverVersion;
    }

    public String statusLabel() {
        return statusLabel;
    }

    public String helpText() {
        return helpText;
    }

    public StatusTone tone() {
        return tone;
    }

    public String updateUrl() {
        return updateUrl;
    }

}
