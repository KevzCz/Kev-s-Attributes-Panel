package net.pixeldreamstudios.attributepanel.network.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import net.pixeldreamstudios.attributepanel.network.ClientNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.ServerNetworkHandler;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

import java.util.function.Supplier;


public class NetworkManagerImpl {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RequestAttributeSnapshotPayload.ID,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered = false;

    public static synchronized void registerPayloads() {
        if (registered) return;
        registered = true;

        int id = 0;

        CHANNEL.registerMessage(
                id++,
                RequestAttributeSnapshotPayload.class,
                (payload, buf) -> RequestAttributeSnapshotPayload.write(buf, payload),
                RequestAttributeSnapshotPayload::read,
                NetworkManagerImpl::handleRequestOnServer
        );

        CHANNEL.registerMessage(
                id++,
                SendAttributeSnapshotPayload.class,
                (payload, buf) -> SendAttributeSnapshotPayload.write(buf, payload),
                SendAttributeSnapshotPayload::read,
                NetworkManagerImpl::handleSnapshotOnClient
        );
    }

    private static void handleRequestOnServer(RequestAttributeSnapshotPayload payload,
                                              Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ServerNetworkHandler.handleAttributeSnapshotRequest(payload, sender);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleSnapshotOnClient(SendAttributeSnapshotPayload payload,
                                               Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientNetworkHandler.handleAttributeSnapshot(payload));
        context.setPacketHandled(true);
    }

    public static void registerClientPayloads() {
        registerPayloads();
    }

    public static void registerServerPayloads() {
        registerPayloads();
    }

    public static void sendToServer(RequestAttributeSnapshotPayload payload) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), payload);
    }

    public static void sendToClient(ServerPlayer player, SendAttributeSnapshotPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
