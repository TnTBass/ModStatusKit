package cloud.explosive.modstatuskit;

import java.util.Objects;

/**
 * Server-published status data decoded from the consuming mod's status payload.
 */
public final class ModStatusServerStatus {
    private final ModStatusVersion serverVersion;
    private final VersionMismatchSeverity versionMismatchSeverity;

    private ModStatusServerStatus(ModStatusVersion serverVersion, VersionMismatchSeverity versionMismatchSeverity) {
        this.serverVersion = Objects.requireNonNull(serverVersion, "serverVersion");
        this.versionMismatchSeverity = versionMismatchSeverity == null
                ? VersionMismatchSeverity.WARN
                : versionMismatchSeverity;
    }

    public static ModStatusServerStatus of(String serverVersion) {
        return of(serverVersion, null, VersionMismatchSeverity.WARN);
    }

    public static ModStatusServerStatus of(
            String serverVersion,
            String serverBuild,
            VersionMismatchSeverity versionMismatchSeverity
    ) {
        return new ModStatusServerStatus(
                ModStatusVersion.of(serverVersion, serverBuild),
                versionMismatchSeverity
        );
    }

    public static ModStatusServerStatus of(ModStatusVersion serverVersion, VersionMismatchSeverity versionMismatchSeverity) {
        return new ModStatusServerStatus(serverVersion, versionMismatchSeverity);
    }

    public String serverVersion() {
        return serverVersion.version();
    }

    public String serverBuild() {
        return serverVersion.build();
    }

    public ModStatusVersion serverVersionInfo() {
        return serverVersion;
    }

    public VersionMismatchSeverity versionMismatchSeverity() {
        return versionMismatchSeverity;
    }
}
