package net.pixeldreamstudios.attributepanel.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.effect.MobEffect;
import net.pixeldreamstudios.attributepanel.compat.emi.EmiEffectBridge;

public class EmiCompat {

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    public static boolean hasEffectEntry(MobEffect effect) {
        return effect != null && isLoaded() && EmiEffectBridge.hasEntry(effect);
    }

    public static boolean showEffectRecipes(MobEffect effect) {
        return effect != null && isLoaded() && EmiEffectBridge.displayRecipes(effect);
    }
}
