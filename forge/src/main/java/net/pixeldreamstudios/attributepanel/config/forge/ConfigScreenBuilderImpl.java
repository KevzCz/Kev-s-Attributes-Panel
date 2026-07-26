package net.pixeldreamstudios.attributepanel.config.forge;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public class ConfigScreenBuilderImpl {

    public static Object buildConfigScreen(Object parent) {
        if (ModList.get().isLoaded("cloth_config")) {
            try {
                return ClothConfigScreenBuilder.buildConfigScreen((Screen) parent);
            } catch (Exception e) {
                System.err.println("[Kev's Attributes Panel] Failed to load Cloth Config screen: " + e.getMessage());
            }
        }
        return null;
    }
}