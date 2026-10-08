package net.pixeldreamstudios.attributepanel.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class EmiCompatImpl {

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("emi");
    }
}
