package net.pixeldreamstudios.attributepanel.network.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.attributepanel.network.ClientNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.ServerNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class NetworkManagerImpl {

    public static void registerClientPayloads() {
        PayloadTypeRegistry.playS2C().register(
                SendAttributeSnapshotPayload.TYPE,
                SendAttributeSnapshotPayload.STREAM_CODEC
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SendAttributeSnapshotPayload.TYPE,
                (payload, context) -> ClientNetworkHandler.handleAttributeSnapshot(payload)
        );
    }

    public static void registerServerPayloads() {
        PayloadTypeRegistry.playC2S().register(
                RequestAttributeSnapshotPayload.TYPE,
                RequestAttributeSnapshotPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                RequestAttributeSnapshotPayload.TYPE,
                (payload, context) -> ServerNetworkHandler.handleAttributeSnapshotRequest(payload, context.player())
        );
    }

    public static void sendToServer(RequestAttributeSnapshotPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendToClient(ServerPlayer player, SendAttributeSnapshotPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}