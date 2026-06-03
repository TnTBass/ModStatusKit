package cloud.explosive.modstatuskit;

import java.util.Objects;

/**
 * Current server-side status known by the consuming mod.
 */
public final class ModStatusSnapshot {
    private final String serverVersion;
    private final VersionStatus status;

    private ModStatusSnapshot(String serverVersion, VersionStatus status) {
        this.serverVersion = normalize(serverVersion);
        this.status = Objects.requireNonNull(status, "status");
    }

    public static ModStatusSnapshot disconnected() {
        return new ModStatusSnapshot(null, VersionStatus.DISCONNECTED);
    }

    public static ModStatusSnapshot unknown() {
        return new ModStatusSnapshot(null, VersionStatus.UNKNOWN);
    }

    public static ModStatusSnapshot serverNotDetected() {
        return new ModStatusSnapshot(null, VersionStatus.SERVER_NOT_DETECTED);
    }

    static ModStatusSnapshot withServerVersion(String serverVersion, VersionStatus status) {
        if (status != VersionStatus.MATCHED && status != VersionStatus.DIFFERENT) {
            throw new IllegalArgumentException("status must be MATCHED or DIFFERENT when serverVersion is present");
        }
        String normalized = ModStatusStrings.requireText(serverVersion, "serverVersion");
        return new ModStatusSnapshot(normalized, status);
    }

    public String serverVersion() {
        return serverVersion;
    }

    public VersionStatus status() {
        return status;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
