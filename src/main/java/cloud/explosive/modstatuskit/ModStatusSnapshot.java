package cloud.explosive.modstatuskit;

import java.util.Objects;

/**
 * Current server-side status known by the consuming mod.
 */
public final class ModStatusSnapshot {
    private final ModStatusVersion serverVersion;
    private final VersionStatus status;

    private ModStatusSnapshot(String serverVersion, VersionStatus status) {
        String normalized = normalize(serverVersion);
        this.serverVersion = normalized == null ? null : ModStatusVersion.of(normalized);
        this.status = Objects.requireNonNull(status, "status");
    }

    private ModStatusSnapshot(ModStatusVersion serverVersion, VersionStatus status) {
        this.serverVersion = Objects.requireNonNull(serverVersion, "serverVersion");
        this.status = Objects.requireNonNull(status, "status");
    }

    public static ModStatusSnapshot disconnected() {
        return new ModStatusSnapshot((String) null, VersionStatus.DISCONNECTED);
    }

    public static ModStatusSnapshot unknown() {
        return new ModStatusSnapshot((String) null, VersionStatus.UNKNOWN);
    }

    public static ModStatusSnapshot serverNotDetected() {
        return new ModStatusSnapshot((String) null, VersionStatus.SERVER_NOT_DETECTED);
    }

    static ModStatusSnapshot withServerVersion(String serverVersion, VersionStatus status) {
        return withServerVersion(ModStatusVersion.of(serverVersion), status);
    }

    static ModStatusSnapshot withServerVersion(ModStatusVersion serverVersion, VersionStatus status) {
        if (status != VersionStatus.MATCHED && status != VersionStatus.DIFFERENT) {
            throw new IllegalArgumentException("status must be MATCHED or DIFFERENT when serverVersion is present");
        }
        return new ModStatusSnapshot(serverVersion, status);
    }

    public String serverVersion() {
        return serverVersion == null ? null : serverVersion.version();
    }

    public String serverBuild() {
        return serverVersion == null ? null : serverVersion.build();
    }

    public ModStatusVersion serverVersionInfo() {
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
