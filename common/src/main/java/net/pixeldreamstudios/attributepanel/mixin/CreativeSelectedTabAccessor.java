package net.pixeldreamstudios.attributepanel.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeSelectedTabAccessor {

    @Accessor("selectedTab")
    static CreativeModeTab attributespanel$getSelectedTab() {
        throw new AssertionError();
    }
}
