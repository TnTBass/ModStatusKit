package com.example.yourmod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Identifier;

// CHANGE: import your relocated/internal ModStatusKit package.
import com.example.yourmod.internal.modstatus.VersionMismatchSeverity;
import com.example.yourmod.internal.modstatus.ModStatusVersionPayload;

/**
 * Server-side status networking. Keep this separate from gameplay packets.
 */
public final class ExampleModStatusNetworking {
    private ExampleModStatusNetworking() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(ServerVersionPayload.TYPE, ServerVersionPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendServerVersionIfSupported(handler.player));

        // CHANGE: optional. Use whatever server lifecycle hook your mod already owns.
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                ExampleMod.LOGGER.info("Registered {} status payload", ExampleModStatus.CONFIG.payloadChannel()));
    }

    public static void sendServerVersionIfSupported(ServerPlayer player) {
        // This is the critical vanilla/old-client safety gate.
        if (!ServerPlayNetworking.canSend(player, ServerVersionPayload.TYPE)) {
            return;
        }

        ModStatusVersionPayload.sendServerStatusIfSupported(
                ExampleModStatus.CONFIG,
                // CHANGE: use VersionMismatchSeverity.BREAKING only when a public/base version
                // mismatch is truly incompatible for your mod. Build mismatch stays diagnostic.
                VersionMismatchSeverity.WARN,
                channel -> true,
                (channel, payload) -> ServerPlayNetworking.send(player, new ServerVersionPayload(payload)));
    }

    /**
     * Fabric custom payload wrapper for the status version.
     *
     * This uses ModStatusKit's plain Java payload helpers while keeping
     * Fabric-specific TYPE/CODEC in the consuming mod's integration layer.
     */
    public record ServerVersionPayload(byte[] value) implements CustomPayload {
        public static final Id<ServerVersionPayload> TYPE = new Id<>(
                // CHANGE: Fabric/MC versions differ between Identifier.of(...) and new Identifier(...).
                new Identifier(ExampleModStatus.MOD_ID, ExampleModStatus.SERVER_VERSION_CHANNEL_PATH));

        public static final PacketCodec<RegistryByteBuf, ServerVersionPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeByteArray(payload.value()),
                buf -> new ServerVersionPayload(buf.readByteArray(64)));

        public byte[] value() {
            return value;
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }
}
