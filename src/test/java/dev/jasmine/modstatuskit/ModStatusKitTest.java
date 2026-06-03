package dev.jasmine.modstatuskit;

public final class ModStatusKitTest {
    public static void main(String[] args) {
        testDefaultMessages();
        testConfigBuilder();
        testSnapshotAndDisplayModels();
        testStatusApi();
        testCustomMessages();
        System.out.println("ModStatusKitTest passed");
    }

    private static void testDefaultMessages() {
        ModStatusMessages messages = ModStatusMessages.defaults();

        assertEquals("Matched", messages.labelFor(VersionStatus.MATCHED), "matched label");
        assertEquals("Different versions", messages.labelFor(VersionStatus.DIFFERENT), "different label");
        assertEquals("Disconnected", messages.labelFor(VersionStatus.DISCONNECTED), "disconnected label");
        assertEquals("Server not detected", messages.labelFor(VersionStatus.SERVER_NOT_DETECTED), "server not detected label");
        assertEquals("Unknown", messages.labelFor(VersionStatus.UNKNOWN), "unknown label");
        assertEquals("Different versions may affect optional features.", messages.helpFor(VersionStatus.DIFFERENT), "different help");
    }

    private static void testConfigBuilder() {
        ModStatusConfig config = ModStatusConfig.builder()
                .modId("examplemod")
                .displayName("Example Mod")
                .clientVersion("1.2.3")
                .payloadChannel("examplemod", "server_version")
                .updateUrl("https://example.invalid/examplemod")
                .messages(ModStatusMessages.defaults())
                .build();

        assertEquals("examplemod", config.modId(), "mod id");
        assertEquals("Example Mod", config.displayName(), "display name");
        assertEquals("1.2.3", config.clientVersion(), "client version");
        assertEquals("examplemod", config.payloadNamespace(), "payload namespace");
        assertEquals("server_version", config.payloadPath(), "payload path");
        assertEquals("examplemod:server_version", config.payloadChannel(), "payload channel");
        assertEquals("https://example.invalid/examplemod", config.updateUrl(), "update url");
    }

    private static void testSnapshotAndDisplayModels() {
        ModStatusSnapshot snapshot = ModStatusSnapshot.withServerVersion("1.2.3", VersionStatus.MATCHED);
        assertEquals("1.2.3", snapshot.serverVersion(), "snapshot server version");
        assertEquals(VersionStatus.MATCHED, snapshot.status(), "snapshot status");

        ModStatusDisplay display = new ModStatusDisplay(
                "Example Mod",
                "1.2.3",
                "1.2.3",
                "Matched",
                "Client and server versions match.",
                StatusTone.GREEN,
                "https://example.invalid/examplemod"
        );

        assertEquals("Example Mod", display.displayName(), "display display name");
        assertEquals("1.2.3", display.clientVersion(), "display client version");
        assertEquals("1.2.3", display.serverVersion(), "display server version");
        assertEquals("Matched", display.statusLabel(), "display status label");
        assertEquals("Client and server versions match.", display.helpText(), "display help text");
        assertEquals(StatusTone.GREEN, display.tone(), "display tone");
        assertEquals("https://example.invalid/examplemod", display.updateUrl(), "display update url");
    }

    private static ModStatusConfig exampleConfig() {
        return ModStatusConfig.builder()
                .modId("examplemod")
                .displayName("Example Mod")
                .clientVersion("1.2.3")
                .payloadChannel("examplemod", "server_version")
                .updateUrl("https://example.invalid/examplemod")
                .messages(ModStatusMessages.defaults())
                .build();
    }

    private static void testStatusApi() {
        ModStatusConfig config = exampleConfig();

        assertDisplay(config, ModStatusKit.disconnected(), VersionStatus.DISCONNECTED, "Unknown", StatusTone.GRAY);
        assertDisplay(config, ModStatusKit.unknown(), VersionStatus.UNKNOWN, "Unknown", StatusTone.GRAY);
        assertDisplay(config, ModStatusKit.serverNotDetected(), VersionStatus.SERVER_NOT_DETECTED, "Unknown", StatusTone.GRAY);
        assertDisplay(config, ModStatusKit.connected(config, "1.2.3"), VersionStatus.MATCHED, "1.2.3", StatusTone.GREEN);
        assertDisplay(config, ModStatusKit.connected(config, "1.2.4"), VersionStatus.DIFFERENT, "1.2.4", StatusTone.ORANGE);
    }

    private static void testCustomMessages() {
        ModStatusMessages messages = ModStatusMessages.builder()
                .label(VersionStatus.DIFFERENT, "Different versions")
                .help(VersionStatus.DIFFERENT, "Different versions may miss or hide new features. Gameplay remains compatible.")
                .build();
        ModStatusConfig config = ModStatusConfig.builder()
                .modId("carrybabyanimals")
                .displayName("Carry Baby Animals")
                .clientVersion("0.1.3")
                .payloadChannel("carrybabyanimals", "server_version")
                .messages(messages)
                .build();

        ModStatusDisplay display = ModStatusKit.display(config, ModStatusKit.connected(config, "0.1.4"));

        assertEquals("Different versions", display.statusLabel(), "custom different label");
        assertEquals("Different versions may miss or hide new features. Gameplay remains compatible.", display.helpText(), "custom different help");
    }

    private static void assertDisplay(
            ModStatusConfig config,
            ModStatusSnapshot snapshot,
            VersionStatus expectedStatus,
            String expectedServerVersion,
            StatusTone expectedTone
    ) {
        ModStatusDisplay display = ModStatusKit.display(config, snapshot);
        assertEquals(expectedStatus.tone(), display.tone(), "status tone from enum");
        assertEquals(expectedTone, display.tone(), "display tone");
        assertEquals(expectedServerVersion, display.serverVersion(), "display server version");
        assertEquals(config.messages().labelFor(expectedStatus), display.statusLabel(), "display status label");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected [" + expected + "] but got [" + actual + "]");
        }
    }
}
