package net.pixeldreamstudios.attributepanel.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class ServerNetwork {
    public static void register() {
        PayloadTypeRegistry.playC2S().register(RequestAttributeSnapshotPayload.ID, RequestAttributeSnapshotPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestAttributeSnapshotPayload.ID, (payload, context) -> {
            ServerPlayerEntity requester = context.player();
            String targetName = payload.targetName();

            requester.server.execute(() -> {
                ServerPlayerEntity target = requester.server
                        .getPlayerManager()
                        .getPlayer(targetName);

                if (target != null) {
                    NbtCompound snapshot = AttributeDataSerializer.serialize(target);
                    ServerPlayNetworking.send(requester, new SendAttributeSnapshotPayload(
                            target.getName().getString(),
                            snapshot
                    ));
                } else {
                    requester.sendMessage(Text.literal("Could not find player: " + targetName), false);
                }
            });
        });
    }
}
