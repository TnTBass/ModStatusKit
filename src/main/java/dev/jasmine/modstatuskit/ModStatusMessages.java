package dev.jasmine.modstatuskit;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Labels and short help text supplied by a consuming mod.
 */
public final class ModStatusMessages {
    private final EnumMap<VersionStatus, String> labels;
    private final EnumMap<VersionStatus, String> helpText;

    private ModStatusMessages(EnumMap<VersionStatus, String> labels, EnumMap<VersionStatus, String> helpText) {
        this.labels = labels;
        this.helpText = helpText;
    }

    public static ModStatusMessages defaults() {
        return builder()
                .label(VersionStatus.MATCHED, "Matched")
                .label(VersionStatus.DIFFERENT, "Different versions")
                .label(VersionStatus.DISCONNECTED, "Disconnected")
                .label(VersionStatus.SERVER_NOT_DETECTED, "Server not detected")
                .label(VersionStatus.UNKNOWN, "Unknown")
                .help(VersionStatus.MATCHED, "Client and server versions match.")
                .help(VersionStatus.DIFFERENT, "Different versions may affect optional features.")
                .help(VersionStatus.DISCONNECTED, "Not connected to a server or world.")
                .help(VersionStatus.SERVER_NOT_DETECTED, "No matching server-side mod was detected.")
                .help(VersionStatus.UNKNOWN, "Server version has not been received yet.")
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String labelFor(VersionStatus status) {
        return labels.get(Objects.requireNonNull(status, "status"));
    }

    public String helpFor(VersionStatus status) {
        return helpText.get(Objects.requireNonNull(status, "status"));
    }

    public static final class Builder {
        private final EnumMap<VersionStatus, String> labels = new EnumMap<>(VersionStatus.class);
        private final EnumMap<VersionStatus, String> helpText = new EnumMap<>(VersionStatus.class);

        public Builder label(VersionStatus status, String label) {
            labels.put(Objects.requireNonNull(status, "status"), requireText(label, "label"));
            return this;
        }

        public Builder help(VersionStatus status, String help) {
            helpText.put(Objects.requireNonNull(status, "status"), requireText(help, "help"));
            return this;
        }

        public ModStatusMessages build() {
            for (VersionStatus status : VersionStatus.values()) {
                labels.putIfAbsent(status, defaultLabel(status));
                helpText.putIfAbsent(status, "");
            }
            return new ModStatusMessages(copyOf(labels), copyOf(helpText));
        }

        private static EnumMap<VersionStatus, String> copyOf(Map<VersionStatus, String> source) {
            return new EnumMap<>(source);
        }

        private static String defaultLabel(VersionStatus status) {
            return switch (status) {
                case MATCHED -> "Matched";
                case DIFFERENT -> "Different versions";
                case DISCONNECTED -> "Disconnected";
                case SERVER_NOT_DETECTED -> "Server not detected";
                case UNKNOWN -> "Unknown";
            };
        }

        private static String requireText(String value, String name) {
            String trimmed = Objects.requireNonNull(value, name).trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return trimmed;
        }
    }
}
