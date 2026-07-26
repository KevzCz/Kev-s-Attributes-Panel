package net.pixeldreamstudios.attributepanel.network.payload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import static net.pixeldreamstudios.attributepanel.KevsAttributesPanel.MOD_ID;

public record SendAttributeSnapshotPayload(String playerName, CompoundTag attributeData) {

    public static final ResourceLocation ID =
            new ResourceLocation(MOD_ID, "send_attribute_snapshot");

    public static void write(FriendlyByteBuf buf, SendAttributeSnapshotPayload payload) {
        buf.writeUtf(payload.playerName);
        buf.writeNbt(payload.attributeData);
    }

    public static SendAttributeSnapshotPayload read(FriendlyByteBuf buf) {
        return new SendAttributeSnapshotPayload(buf.readUtf(), buf.readNbt());
    }
}
