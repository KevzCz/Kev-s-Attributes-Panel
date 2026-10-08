package net.pixeldreamstudios.attributepanel.compat.forge;

import net.minecraftforge.fml.ModList;

public class EmiCompatImpl {

    public static boolean isLoaded() {
        return ModList.get().isLoaded("emi");
    }
}
