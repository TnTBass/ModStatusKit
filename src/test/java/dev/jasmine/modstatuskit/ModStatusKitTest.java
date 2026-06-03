package dev.jasmine.modstatuskit;

public final class ModStatusKitTest {
    public static void main(String[] args) {
        testDefaultMessages();
        testConfigBuilder();
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

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected [" + expected + "] but got [" + actual + "]");
        }
    }
}
