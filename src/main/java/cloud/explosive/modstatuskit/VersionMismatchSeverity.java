package cloud.explosive.modstatuskit;

/**
 * Server-declared severity for public/base version mismatches.
 */
public enum VersionMismatchSeverity {
    WARN,
    BREAKING;

    static VersionMismatchSeverity fromPayloadValue(String value) {
        String normalized = ModStatusStrings.optionalText(value);
        if (normalized == null) {
            return WARN;
        }
        for (VersionMismatchSeverity severity : values()) {
            if (severity.name().equalsIgnoreCase(normalized)) {
                return severity;
            }
        }
        return WARN;
    }
}
