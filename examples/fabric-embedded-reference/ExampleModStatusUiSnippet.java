package com.example.yourmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

// CHANGE: import your relocated/internal ModStatusKit package.
import com.example.yourmod.internal.modstatus.ModStatusDisplay;
import com.example.yourmod.internal.modstatus.ModStatusKit;
import com.example.yourmod.internal.modstatus.StatusTone;

/**
 * Optional client-side ModMenu integration.
 *
 * CHANGE: register this class only from your client ModMenu entrypoint metadata.
 * ModMenu should be compileOnly/optional; the rest of the mod must work without it.
 */
public final class ExampleModStatusUiSnippet implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ExampleStatusScreen::new;
    }

    /**
     * CHANGE: replace this with your real ModMenu/config screen. The important
     * part is the status row: colored dot, label, and hover text.
     */
    private static final class ExampleStatusScreen extends Screen {
        private final Screen parent;

        private ExampleStatusScreen(Screen parent) {
            super(net.minecraft.text.Text.literal(ExampleModStatus.DISPLAY_NAME));
            this.parent = parent;
        }

        @Override
        protected void init() {
            YourUiWriter ui = YourUiWriter.from(this);
            renderStatusRow(ui);

            // CHANGE: add your normal config controls here.
        }

        @Override
        public void close() {
            client.setScreen(parent);
        }
    }

    public static void renderStatusRow(YourUiWriter ui) {
        ModStatusDisplay display = ModStatusKit.display(
                ExampleModStatus.CONFIG,
                ExampleModStatus.CLIENT_STATE.snapshot());
        int statusColor = statusColorFor(display);

        ui.statusDot(statusColor, display.helpText());
        ui.text(display.statusLabel(), statusColor, display.helpText());
        ui.text("Client: " + display.clientVersion());
        ui.text("Server: " + display.serverVersion());

        // CHANGE: show the update URL only where it fits your UI and policy.
        if (!display.updateUrl().isEmpty()) {
            ui.link("Updates", display.updateUrl());
        }
    }

    private static int statusColorFor(ModStatusDisplay display) {
        if (display.tone() == StatusTone.GREEN && hasBuildMismatch(display)) {
            return 0x33D6D6;
        }
        return colorFor(display.tone());
    }

    private static boolean hasBuildMismatch(ModStatusDisplay display) {
        String clientBuild = display.clientBuild();
        String serverBuild = display.serverBuild();
        return clientBuild != null && serverBuild != null && !clientBuild.equals(serverBuild);
    }

    private static int colorFor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> 0x55FF55;
            case ORANGE -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
        };
    }

    /**
     * CHANGE: replace this adapter with your actual screen/widget/draw-context
     * code. CarryBabyAnimals-style UI uses the same idea: draw the dot and make
     * the status row hoverable for the help text.
     */
    public interface YourUiWriter {
        static YourUiWriter from(Screen screen) {
            // CHANGE: return an adapter backed by your screen's widgets/draw code.
            throw new UnsupportedOperationException("Replace with your UI adapter");
        }

        void statusDot(int rgb, String hoverText);

        void text(String text);

        void text(String text, int rgb, String hoverText);

        void link(String label, String url);
    }
}
