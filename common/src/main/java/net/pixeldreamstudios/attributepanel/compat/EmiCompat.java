package net.pixeldreamstudios.attributepanel.compat;

import net.minecraft.world.effect.MobEffect;

// EMI has no 1.21.11 build yet; the integration is disabled until it ports.
public class EmiCompat {

    public static boolean isLoaded() {
        return false;
    }

    public static boolean hasEffectEntry(MobEffect effect) {
        return false;
    }

    public static boolean showEffectRecipes(MobEffect effect) {
        return false;
    }
}
