package net.pixeldreamstudios.attributepanel.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class ClientNetwork {
    public static void register() {
        PayloadTypeRegistry.playS2C().register(SendAttributeSnapshotPayload.ID, SendAttributeSnapshotPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(SendAttributeSnapshotPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                String playerName = payload.playerName();
                NbtCompound data = payload.attributeData();

                MinecraftClient.getInstance().player.sendMessage(
                        net.minecraft.text.Text.literal("Received attribute snapshot for: " + playerName), false
                );

//               System.out.println(data);
            });
        });
    }
}
