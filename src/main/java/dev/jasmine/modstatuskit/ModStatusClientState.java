package dev.jasmine.modstatuskit;

import java.util.Objects;

/**
 * Reusable client-side status state for consuming mods to call from their own
 * lifecycle and networking callbacks.
 */
public final class ModStatusClientState {
    private final ModStatusConfig config;
    private volatile ModStatusSnapshot snapshot;

    private ModStatusClientState(ModStatusConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.snapshot = ModStatusKit.disconnected();
    }

    public static ModStatusClientState create(ModStatusConfig config) {
        return new ModStatusClientState(config);
    }

    public ModStatusConfig config() {
        return config;
    }

    public ModStatusSnapshot snapshot() {
        return snapshot;
    }

    public ModStatusDisplay display() {
        return ModStatusKit.display(config, snapshot);
    }

    public void disconnected() {
        snapshot = ModStatusKit.disconnected();
    }

    public void unknown() {
        snapshot = ModStatusKit.unknown();
    }

    public void serverNotDetected() {
        snapshot = ModStatusKit.serverNotDetected();
    }

    public void connected(String serverVersion) {
        snapshot = ModStatusKit.connected(config, serverVersion);
    }

    public boolean markServerNotDetectedIfUnknown() {
        if (snapshot.status() != VersionStatus.UNKNOWN) {
            return false;
        }
        serverNotDetected();
        return true;
    }
}
