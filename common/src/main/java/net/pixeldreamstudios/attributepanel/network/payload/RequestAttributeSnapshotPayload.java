package net.pixeldreamstudios.attributepanel.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static net.pixeldreamstudios.attributepanel.KevsAttributesPanel.MOD_ID;

public record RequestAttributeSnapshotPayload(String targetName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestAttributeSnapshotPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "request_attribute_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, RequestAttributeSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(RequestAttributeSnapshotPayload:: write, RequestAttributeSnapshotPayload::read);

    @Override
    public CustomPacketPayload.Type<RequestAttributeSnapshotPayload> type() {
        return TYPE;
    }

    public static void write(FriendlyByteBuf buf, RequestAttributeSnapshotPayload payload) {
        buf.writeUtf(payload.targetName);
    }

    public static RequestAttributeSnapshotPayload read(FriendlyByteBuf buf) {
        return new RequestAttributeSnapshotPayload(buf.readUtf());
    }
}