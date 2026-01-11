package net.pixeldreamstudios.attributepanel.config.neoforge;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

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