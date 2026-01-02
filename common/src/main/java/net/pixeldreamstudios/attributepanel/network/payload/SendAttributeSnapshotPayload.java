package net.pixeldreamstudios.attributepanel.network.payload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static net.pixeldreamstudios.attributepanel.KevsAttributesPanel.MOD_ID;

public record SendAttributeSnapshotPayload(String playerName, CompoundTag attributeData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SendAttributeSnapshotPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "send_attribute_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, SendAttributeSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(SendAttributeSnapshotPayload::write, SendAttributeSnapshotPayload::read);

    @Override
    public CustomPacketPayload.Type<SendAttributeSnapshotPayload> type() {
        return TYPE;
    }

    public static void write(FriendlyByteBuf buf, SendAttributeSnapshotPayload payload) {
        buf.writeUtf(payload.playerName);
        buf.writeNbt(payload.attributeData);
    }

    public static SendAttributeSnapshotPayload read(FriendlyByteBuf buf) {
        return new SendAttributeSnapshotPayload(buf.readUtf(), buf.readNbt());
    }
}