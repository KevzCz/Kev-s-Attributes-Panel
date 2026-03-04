package net.pixeldreamstudios.attributepanel.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable;
import net.pixeldreamstudios.attributepanel.accessor.AttributePanelAccessor;
import net.pixeldreamstudios.attributepanel.network.AttributeDataSerializer;

public class AttributePanelAPI {

    public static CompoundTag getAttributeSnapshot(Player player) {
        return AttributeDataSerializer.serialize(player);
    }

    public static void togglePanel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        
        Screen screen = mc.screen;
        if (screen instanceof AbstractContainerScreen<?>) {
            if (screen instanceof AttributePanelAccessor accessor) {
                AttributePanelDrawable panel = accessor.attributespanel$getAttributePanel();
                if (panel != null) {
                    panel.toggle();
                }
            }
        }
    }
}