package net.pixeldreamstudios.attributepanel.config.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client. gui.screens.Screen;

public class ConfigScreenBuilderImpl {

    public static Screen buildConfigScreen(Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            try {
                return ClothConfigScreenBuilder.buildConfigScreen(parent);
            } catch (Exception e) {
                System.err.println("[Kev's Attributes Panel] Failed to load Cloth Config screen: " + e.getMessage());
            }
        }
        return null;
    }
}