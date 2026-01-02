package net.pixeldreamstudios.attributepanel.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class ServerNetworkHandler {

    public static void handleAttributeSnapshotRequest(RequestAttributeSnapshotPayload payload, ServerPlayer requester) {
        String targetName = payload.targetName();

        requester.server.execute(() -> {
            ServerPlayer target = requester.server
                    .getPlayerList()
                    .getPlayerByName(targetName);

            if (target != null) {
                CompoundTag snapshot = AttributeDataSerializer.serialize(target);
                NetworkManager.sendToClient(requester, new SendAttributeSnapshotPayload(
                        target.getName().getString(),
                        snapshot
                ));
            } else {
                requester.sendSystemMessage(Component.literal("Could not find player: " + targetName));
            }
        });
    }
}