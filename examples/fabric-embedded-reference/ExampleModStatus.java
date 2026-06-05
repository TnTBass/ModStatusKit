package com.example.yourmod;

// CHANGE: import your relocated/internal ModStatusKit package.
import com.example.yourmod.internal.modstatus.ModStatusConfig;
import com.example.yourmod.internal.modstatus.ModStatusClientState;
import com.example.yourmod.internal.modstatus.ModStatusMessages;
import com.example.yourmod.internal.modstatus.ModStatusServerStatus;
import com.example.yourmod.internal.modstatus.VersionStatus;

/**
 * Shared status configuration plus a tiny client-side state holder.
 */
public final class ExampleModStatus {
    // CHANGE: use your real mod id, display name, version, and update URL.
    public static final String MOD_ID = "yourmod";
    public static final String DISPLAY_NAME = "Your Mod";
    public static final String CURRENT_VERSION = "1.2.3";
    public static final String UPDATE_URL = "https://modrinth.com/mod/yourmod";

    // CHANGE: dedicate this channel to passive status, not gameplay behavior.
    public static final String SERVER_VERSION_CHANNEL_PATH = "server_version";

    public static final ModStatusConfig CONFIG = ModStatusConfig.builder()
            .modId(MOD_ID)
            .displayName(DISPLAY_NAME)
            .clientVersion(CURRENT_VERSION)
            .clientBuild(BuildInfo.GIT_COMMIT)
            .updateUrl(UPDATE_URL)
            .payloadChannel(MOD_ID, SERVER_VERSION_CHANNEL_PATH)
            .messages(ModStatusMessages.builder()
                    .label(VersionStatus.MATCHED, "Server matches")
                    .label(VersionStatus.DIFFERENT, "Different versions")
                    .label(VersionStatus.DISCONNECTED, "Disconnected")
                    .label(VersionStatus.SERVER_NOT_DETECTED, "Server not detected")
                    .label(VersionStatus.UNKNOWN, "Unknown")
                    .help(VersionStatus.MATCHED, "Client and server versions match.")
                    .help(VersionStatus.DIFFERENT,
                            "Different versions may miss or hide new features. Gameplay remains compatible.")
                    .help(VersionStatus.DISCONNECTED, "Join a world or server to check server status.")
                    .help(VersionStatus.SERVER_NOT_DETECTED,
                            "The server did not send status for this mod. Gameplay remains compatible.")
                    .help(VersionStatus.UNKNOWN, "Waiting briefly for server status.")
                    .build())
            .build();

    private ExampleModStatus() {
    }

    public static final ModStatusClientState CLIENT_STATE = ModStatusClientState.create(CONFIG);

    private static int ticksSinceJoin;
    private static boolean waitingForServerStatus;

    public static void onClientJoin() {
        CLIENT_STATE.unknown();
        ticksSinceJoin = 0;
        waitingForServerStatus = true;
    }

    public static void onClientDisconnect() {
        CLIENT_STATE.disconnected();
        ticksSinceJoin = 0;
        waitingForServerStatus = false;
    }

    public static void onServerVersion(String serverVersion) {
        onServerStatus(ModStatusServerStatus.of(serverVersion));
    }

    public static void onServerStatus(ModStatusServerStatus serverStatus) {
        CLIENT_STATE.connected(serverStatus);
        waitingForServerStatus = false;
    }

    public static void tick() {
        if (!waitingForServerStatus) {
            return;
        }

        ticksSinceJoin++;

        // CHANGE: pick a short timeout that feels natural in your client UI.
        if (ticksSinceJoin >= 100) {
            CLIENT_STATE.markServerNotDetectedIfUnknown();
            waitingForServerStatus = false;
        }
    }
}
