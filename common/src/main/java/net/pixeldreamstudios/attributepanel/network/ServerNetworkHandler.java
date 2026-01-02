package net.pixeldreamstudios.attributepanel.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class ServerNetworkHandler {

    public static void handleAttributeSnapshotRequest(RequestAttributeSnapshotPayload payload, ServerPlayer requester) {
        String targetName = payload.targetName();

        KevsAttributesPanel.LOGGER.info("Received snapshot request from {} for target: {}",
                requester.getName().getString(), targetName);

        ServerPlayer target = requester.server
                .getPlayerList()
                .getPlayerByName(targetName);

        if (target != null) {
            KevsAttributesPanel.LOGGER.info("Found target player: {}", targetName);
            CompoundTag snapshot = AttributeDataSerializer.serialize(target);

            KevsAttributesPanel.LOGGER.info("Serialized {} attributes", snapshot.getAllKeys().size());

            NetworkManager.sendToClient(requester, new SendAttributeSnapshotPayload(
                    target.getName().getString(),
                    snapshot
            ));
        } else {
            KevsAttributesPanel.LOGGER.warn("Could not find player:  {}", targetName);
            requester.sendSystemMessage(Component.literal("Could not find player: " + targetName));
        }
    }
}