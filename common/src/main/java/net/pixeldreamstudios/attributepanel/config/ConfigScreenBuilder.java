package net.pixeldreamstudios.attributepanel.config;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.gui.screens.Screen;

public class ConfigScreenBuilder {
    
    @ExpectPlatform
    public static Screen buildConfigScreen(Screen parent) {
        throw new AssertionError();
    }
}