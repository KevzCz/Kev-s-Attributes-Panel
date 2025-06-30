package net.pixeldreamstudios.attributepanel.network.payload;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static net.pixeldreamstudios.attributepanel.KevsAttributesPanel.MOD_ID;

public record SendAttributeSnapshotPayload(String playerName, NbtCompound attributeData) implements CustomPayload {
    public static final Id<SendAttributeSnapshotPayload> ID =
            new Id<>(Identifier.of(MOD_ID, "send_attribute_snapshot"));

    public static final PacketCodec<PacketByteBuf, SendAttributeSnapshotPayload> CODEC =
            PacketCodec.of(SendAttributeSnapshotPayload::write, SendAttributeSnapshotPayload::read);

    @Override
    public Id<SendAttributeSnapshotPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(playerName);
        buf.writeNbt(attributeData);
    }

    public static SendAttributeSnapshotPayload read(PacketByteBuf buf) {
        return new SendAttributeSnapshotPayload(buf.readString(), buf.readNbt());
    }
}
