package net.pixeldreamstudios.attributepanel.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class ClientNetworkHandler {

    public static void handleAttributeSnapshot(SendAttributeSnapshotPayload payload) {
        String playerName = payload.playerName();
        CompoundTag data = payload.attributeData();

        KevsAttributesPanel.LOGGER.info("Received attribute snapshot for:  {}", playerName);
        KevsAttributesPanel.LOGGER.info("Snapshot data: {}", data);

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("Received attribute snapshot for: " + playerName)
            );

            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("Data: " + data.toString())
            );
        }
    }
}