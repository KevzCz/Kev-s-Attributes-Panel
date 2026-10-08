package net.pixeldreamstudios.attributepanel.compat.neoforge;

import net.neoforged.fml.ModList;

public class EmiCompatImpl {

    public static boolean isLoaded() {
        return ModList.get().isLoaded("emi");
    }
}
