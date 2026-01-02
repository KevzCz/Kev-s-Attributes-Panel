package net.pixeldreamstudios.attributepanel.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.network.AttributeDataSerializer;

public class AttributePanelAPI {

    public static CompoundTag getAttributeSnapshot(Player player) {
        return AttributeDataSerializer.serialize(player);
    }
}