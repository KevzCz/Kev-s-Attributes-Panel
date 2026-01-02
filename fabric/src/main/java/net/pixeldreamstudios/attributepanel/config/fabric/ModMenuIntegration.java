package net.pixeldreamstudios.attributepanel.config.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.attributepanel.config.ConfigScreenBuilder;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return ConfigScreenBuilder::buildConfigScreen;
        }
        return parent -> null;
    }
}