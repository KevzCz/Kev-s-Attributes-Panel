package net.pixeldreamstudios.attributepanel.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class ClientNetworkHandler {

    public static void handleAttributeSnapshot(SendAttributeSnapshotPayload payload) {
        Minecraft.getInstance().execute(() -> {
            String playerName = payload.playerName();
            CompoundTag data = payload.attributeData();

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("Received attribute snapshot for: " + playerName)
                );
            }

//             System.out.println(data);
        });
    }
}