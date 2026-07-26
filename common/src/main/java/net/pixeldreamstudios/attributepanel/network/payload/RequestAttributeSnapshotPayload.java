package net.pixeldreamstudios.attributepanel.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import static net.pixeldreamstudios.attributepanel.KevsAttributesPanel.MOD_ID;

public record RequestAttributeSnapshotPayload(String targetName) {

    public static final ResourceLocation ID =
            new ResourceLocation(MOD_ID, "request_attribute_snapshot");

    public static void write(FriendlyByteBuf buf, RequestAttributeSnapshotPayload payload) {
        buf.writeUtf(payload.targetName);
    }

    public static RequestAttributeSnapshotPayload read(FriendlyByteBuf buf) {
        return new RequestAttributeSnapshotPayload(buf.readUtf());
    }
}
