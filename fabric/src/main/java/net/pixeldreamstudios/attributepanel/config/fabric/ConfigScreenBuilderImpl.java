package net.pixeldreamstudios.attributepanel.config.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public class ConfigScreenBuilderImpl {

    public static Object buildConfigScreen(Object parent) {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            try {
                return ClothConfigScreenBuilder.buildConfigScreen((Screen) parent);
            } catch (Exception e) {
                System.err.println("[Kev's Attributes Panel] Failed to load Cloth Config screen: " + e.getMessage());
            }
        }
        return null;
    }
}