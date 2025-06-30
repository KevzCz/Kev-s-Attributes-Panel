package net.pixeldreamstudios.attributepanel.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static net.pixeldreamstudios.attributepanel.KevsAttributesPanel.MOD_ID;

public record RequestAttributeSnapshotPayload(String targetName) implements CustomPayload {
    public static final Id<RequestAttributeSnapshotPayload> ID =
            new Id<>(Identifier.of(MOD_ID, "request_attribute_snapshot"));

    public static final PacketCodec<PacketByteBuf, RequestAttributeSnapshotPayload> CODEC =
            PacketCodec.of(RequestAttributeSnapshotPayload::write, RequestAttributeSnapshotPayload::read);

    @Override
    public Id<RequestAttributeSnapshotPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(targetName);
    }

    public static RequestAttributeSnapshotPayload read(PacketByteBuf buf) {
        return new RequestAttributeSnapshotPayload(buf.readString());
    }
}
