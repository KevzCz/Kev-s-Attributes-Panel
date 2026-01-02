package net.pixeldreamstudios.attributepanel.config.neoforge;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

public class ConfigScreenBuilderImpl {

    public static Screen buildConfigScreen(Screen parent) {
        if (ModList.get().isLoaded("cloth_config")) {
            try {
                return ClothConfigScreenBuilder.buildConfigScreen(parent);
            } catch (Exception e) {
                System.err.println("[Kev's Attributes Panel] Failed to load Cloth Config screen: " + e.getMessage());
            }
        }
        return null;
    }
}