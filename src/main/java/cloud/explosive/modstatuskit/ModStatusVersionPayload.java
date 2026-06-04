package cloud.explosive.modstatuskit;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Dependency-free payload helpers for consuming mods to use from Fabric
 * networking callbacks.
 */
public final class ModStatusVersionPayload {
    private ModStatusVersionPayload() {
    }

    @FunctionalInterface
    public interface PayloadSupport {
        boolean canSend(String channel);
    }

    @FunctionalInterface
    public interface PayloadSender {
        void send(String channel, byte[] payload);
    }

    public static byte[] encodeServerVersion(String serverVersion) {
        return ModStatusStrings.requireText(serverVersion, "serverVersion").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] encodeServerVersion(String serverVersion, String serverBuild) {
        return ModStatusVersion.of(serverVersion, serverBuild).toPayloadString().getBytes(StandardCharsets.UTF_8);
    }

    public static String decodeServerVersion(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        return ModStatusStrings.requireText(new String(payload, StandardCharsets.UTF_8), "serverVersion");
    }

    public static ModStatusVersion decodeServerVersionInfo(byte[] payload) {
        return ModStatusVersion.of(decodeServerVersion(payload));
    }

    public static boolean sendServerVersionIfSupported(
            ModStatusConfig config,
            PayloadSupport support,
            PayloadSender sender
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(sender, "sender");

        String channel = config.payloadChannel();
        if (!support.canSend(channel)) {
            return false;
        }
        ModStatusVersion version = config.clientVersionInfo();
        sender.send(channel, encodeServerVersion(version.version(), version.build()));
        return true;
    }
}
