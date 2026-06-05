package cloud.explosive.modstatuskit;

import java.util.Objects;

/**
 * Tiny public facade for consuming mods.
 */
public final class ModStatusKit {
    private static final String UNKNOWN_SERVER_VERSION = "Unknown";

    private ModStatusKit() {
    }

    public static ModStatusSnapshot disconnected() {
        return ModStatusSnapshot.disconnected();
    }

    public static ModStatusSnapshot unknown() {
        return ModStatusSnapshot.unknown();
    }

    public static ModStatusSnapshot serverNotDetected() {
        return ModStatusSnapshot.serverNotDetected();
    }

    public static ModStatusSnapshot connected(ModStatusConfig config, String serverVersion) {
        return connected(config, ModStatusServerStatus.of(serverVersion));
    }

    public static ModStatusSnapshot connected(ModStatusConfig config, ModStatusServerStatus serverStatus) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(serverStatus, "serverStatus");
        VersionStatus status = config.clientVersionInfo().version().equals(serverStatus.serverVersionInfo().version())
                ? VersionStatus.MATCHED
                : VersionStatus.DIFFERENT;
        return ModStatusSnapshot.withServerVersion(
                serverStatus.serverVersionInfo(),
                status,
                serverStatus.versionMismatchSeverity()
        );
    }

    public static ModStatusDisplay display(ModStatusConfig config, ModStatusSnapshot snapshot) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(snapshot, "snapshot");

        String serverVersion = snapshot.serverVersion() == null ? UNKNOWN_SERVER_VERSION : snapshot.serverVersion();
        VersionStatus status = snapshot.status();
        ModStatusMessages messages = config.messages();

        return new ModStatusDisplay(
                config.displayName(),
                config.clientVersion(),
                config.clientBuild(),
                serverVersion,
                snapshot.serverBuild(),
                messages.labelFor(status),
                messages.helpFor(status),
                toneFor(status, snapshot.versionMismatchSeverity(), config.clientBuild(), snapshot.serverBuild()),
                config.updateUrl()
        );
    }

    private static StatusTone toneFor(
            VersionStatus status,
            VersionMismatchSeverity severity,
            String clientBuild,
            String serverBuild
    ) {
        if (status == VersionStatus.MATCHED && buildsDiffer(clientBuild, serverBuild)) {
            return StatusTone.TEAL;
        }
        if (status == VersionStatus.DIFFERENT && severity == VersionMismatchSeverity.BREAKING) {
            return StatusTone.RED;
        }
        return status.tone();
    }

    private static boolean buildsDiffer(String clientBuild, String serverBuild) {
        return clientBuild != null && serverBuild != null && !clientBuild.equals(serverBuild);
    }

}
