package cloud.explosive.modstatuskit;

/**
 * Informational client/server version status.
 */
public enum VersionStatus {
    MATCHED,
    DIFFERENT,
    DISCONNECTED,
    SERVER_NOT_DETECTED,
    UNKNOWN;

    public StatusTone tone() {
        return switch (this) {
            case MATCHED -> StatusTone.GREEN;
            case DIFFERENT -> StatusTone.ORANGE;
            case DISCONNECTED, SERVER_NOT_DETECTED, UNKNOWN -> StatusTone.GRAY;
        };
    }
}
