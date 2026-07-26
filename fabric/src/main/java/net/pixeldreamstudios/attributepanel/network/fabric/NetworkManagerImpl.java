package net.pixeldreamstudios.attributepanel.network.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.pixeldreamstudios.attributepanel.network.ClientNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.ServerNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class NetworkManagerImpl {

    public static void registerClientPayloads() {
        ClientPlayNetworking.registerGlobalReceiver(
                SendAttributeSnapshotPayload.ID,
                (client, handler, buf, responseSender) -> {
                    SendAttributeSnapshotPayload payload = SendAttributeSnapshotPayload.read(buf);
                    client.execute(() -> ClientNetworkHandler.handleAttributeSnapshot(payload));
                }
        );
    }

    public static void registerServerPayloads() {
        ServerPlayNetworking.registerGlobalReceiver(
                RequestAttributeSnapshotPayload.ID,
                (server, player, handler, buf, responseSender) -> {
                    RequestAttributeSnapshotPayload payload = RequestAttributeSnapshotPayload.read(buf);
                    server.execute(() -> ServerNetworkHandler.handleAttributeSnapshotRequest(payload, player));
                }
        );
    }

    public static void sendToServer(RequestAttributeSnapshotPayload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        RequestAttributeSnapshotPayload.write(buf, payload);
        ClientPlayNetworking.send(RequestAttributeSnapshotPayload.ID, buf);
    }

    public static void sendToClient(ServerPlayer player, SendAttributeSnapshotPayload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        SendAttributeSnapshotPayload.write(buf, payload);
        ServerPlayNetworking.send(player, SendAttributeSnapshotPayload.ID, buf);
    }
}
