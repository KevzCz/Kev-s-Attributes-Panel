package net.pixeldreamstudios.attributepanel.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.pixeldreamstudios.attributepanel.network.AttributeDataSerializer;

public class AttributePanelAPI {
    public static NbtCompound getAttributeSnapshot(PlayerEntity player) {
        return AttributeDataSerializer.serialize(player);
    }
}
