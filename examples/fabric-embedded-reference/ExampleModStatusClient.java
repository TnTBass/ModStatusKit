package com.example.yourmod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Client-side status networking and timeout behavior.
 */
public final class ExampleModStatusClient {
    private ExampleModStatusClient() {
    }

    public static void registerClient() {
        PayloadTypeRegistry.playS2C().register(
                ExampleModStatusNetworking.ServerVersionPayload.TYPE,
                ExampleModStatusNetworking.ServerVersionPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(
                ExampleModStatusNetworking.ServerVersionPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ExampleModStatus.onServerVersion(payload.serverVersion())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ExampleModStatus.onClientJoin());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ExampleModStatus.onClientDisconnect());

        ClientTickEvents.END_CLIENT_TICK.register(client ->
                ExampleModStatus.tick());

        // CHANGE: optional. If your client has static UI caches, clear them here.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
                ExampleModStatus.onClientDisconnect());
    }
}
