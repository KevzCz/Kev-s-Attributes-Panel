package net.pixeldreamstudios.attributepanel.network.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.network.ClientNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.ServerNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class NetworkManagerImpl {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KevsAttributesPanel.MOD_ID);

        registrar.playToServer(
                RequestAttributeSnapshotPayload.TYPE,
                RequestAttributeSnapshotPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() ->
                            ServerNetworkHandler.handleAttributeSnapshotRequest(payload, (ServerPlayer) context.player())
                    );
                }
        );

        registrar.playToClient(
                SendAttributeSnapshotPayload.TYPE,
                SendAttributeSnapshotPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() ->
                            ClientNetworkHandler.handleAttributeSnapshot(payload)
                    );
                }
        );
    }

    public static void registerClientPayloads() {
    }

    public static void registerServerPayloads() {
    }

    public static void sendToServer(RequestAttributeSnapshotPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToClient(ServerPlayer player, SendAttributeSnapshotPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}