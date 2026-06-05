package com.example.yourmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

// CHANGE: import your relocated/internal ModStatusKit package.
import com.example.yourmod.internal.modstatus.ModStatusDisplay;
import com.example.yourmod.internal.modstatus.ModStatusKit;

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
        // Use the display tone, not VersionStatus.tone(); display tone includes build/severity context.
        int statusColor = ExampleModStatusDisplay.toneColor(display.tone());
        List<String> tooltip = ExampleModStatusDisplay.tooltipText(display);

        ui.statusDot(statusColor, tooltip);
        ui.text(display.statusLabel(), statusColor, tooltip);
        ui.text("Client: " + ExampleModStatusDisplay.versionWithBuild(display.clientVersion(), display.clientBuild()));
        ui.text("Server: " + ExampleModStatusDisplay.versionWithBuild(display.serverVersion(), display.serverBuild()));

        // CHANGE: show the update URL only where it fits your UI and policy.
        if (!display.updateUrl().isEmpty()) {
            ui.link("Updates", display.updateUrl());
        }
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

        void statusDot(int argb, List<String> hoverText);

        void text(String text);

        void text(String text, int argb, List<String> hoverText);

        void link(String label, String url);
    }
}
