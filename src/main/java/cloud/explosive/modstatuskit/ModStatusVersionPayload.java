package cloud.explosive.modstatuskit;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Dependency-free payload helpers for consuming mods to use from Fabric
 * networking callbacks.
 */
public final class ModStatusVersionPayload {
    private static final String STRUCTURED_PREFIX = "MSK2";
    private static final String KEY_VERSION = "version";
    private static final String KEY_BUILD = "build";
    private static final String KEY_VERSION_MISMATCH_SEVERITY = "versionMismatchSeverity";

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

    public static byte[] encodeServerStatus(
            String serverVersion,
            String serverBuild,
            VersionMismatchSeverity versionMismatchSeverity
    ) {
        ModStatusVersion version = ModStatusVersion.of(serverVersion, serverBuild);
        VersionMismatchSeverity severity = versionMismatchSeverity == null
                ? VersionMismatchSeverity.WARN
                : versionMismatchSeverity;
        StringBuilder payload = new StringBuilder();
        payload.append(STRUCTURED_PREFIX).append('\n');
        appendField(payload, KEY_VERSION, version.version());
        if (version.build() != null) {
            appendField(payload, KEY_BUILD, version.build());
        }
        appendField(payload, KEY_VERSION_MISMATCH_SEVERITY, severity.name());
        return payload.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String decodeServerVersion(byte[] payload) {
        String text = decodePayloadText(payload);
        text = normalizeLineEndings(text);
        if (text.startsWith(STRUCTURED_PREFIX + "\n")) {
            return decodeServerStatus(payload).serverVersionInfo().toPayloadString();
        }
        return ModStatusStrings.requireText(text, "serverVersion");
    }

    public static ModStatusVersion decodeServerVersionInfo(byte[] payload) {
        return ModStatusVersion.of(decodeServerVersion(payload));
    }

    public static ModStatusServerStatus decodeServerStatus(byte[] payload) {
        String text = decodePayloadText(payload);
        text = normalizeLineEndings(text);
        if (!text.startsWith(STRUCTURED_PREFIX + "\n")) {
            return ModStatusServerStatus.of(ModStatusVersion.of(text), VersionMismatchSeverity.WARN);
        }

        String version = null;
        String build = null;
        VersionMismatchSeverity severity = VersionMismatchSeverity.WARN;
        String[] lines = text.split("\\n");
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index];
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator);
            String value = line.substring(separator + 1);
            if (KEY_VERSION.equals(key)) {
                version = ModStatusStrings.requireText(value, key);
            } else if (KEY_BUILD.equals(key)) {
                build = value;
            } else if (KEY_VERSION_MISMATCH_SEVERITY.equals(key)) {
                severity = VersionMismatchSeverity.fromPayloadValue(value);
            }
        }
        if (version == null) {
            throw new IllegalArgumentException("structured payload missing version");
        }
        return ModStatusServerStatus.of(version, build, severity);
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

    public static boolean sendServerStatusIfSupported(
            ModStatusConfig config,
            VersionMismatchSeverity versionMismatchSeverity,
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
        sender.send(channel, encodeServerStatus(version.version(), version.build(), versionMismatchSeverity));
        return true;
    }

    private static String decodePayloadText(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        return ModStatusStrings.requireText(new String(payload, StandardCharsets.UTF_8), "serverVersion");
    }

    private static String normalizeLineEndings(String text) {
        return text.indexOf('\r') < 0 ? text : text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void appendField(StringBuilder payload, String key, String value) {
        String normalized = ModStatusStrings.requireText(value, key);
        if (normalized.indexOf('\n') >= 0 || normalized.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(key + " must not contain line breaks");
        }
        payload.append(key).append('=').append(normalized).append('\n');
    }
}
