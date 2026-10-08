package net.pixeldreamstudios.attributepanel.compat.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.effect.MobEffect;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class EmiEffectBridge {
    private static final Map<MobEffect, EmiStack> EFFECT_STACKS = new IdentityHashMap<>();
    private static List<EmiStack> indexedFrom = null;
    private static int indexedSize = -1;

    private EmiEffectBridge() {}

    public static EmiStack find(MobEffect effect) {
        List<EmiStack> index = EmiApi.getIndexStacks();
        if (index == null) return null;

        if (index != indexedFrom || index.size() != indexedSize) {
            EFFECT_STACKS.clear();
            for (EmiStack stack : index) {
                if (stack.getKey() instanceof MobEffect keyEffect) {
                    EFFECT_STACKS.putIfAbsent(keyEffect, stack);
                }
            }
            indexedFrom = index;
            indexedSize = index.size();
        }
        return EFFECT_STACKS.get(effect);
    }

    public static boolean hasEntry(MobEffect effect) {
        return find(effect) != null;
    }

    public static boolean displayRecipes(MobEffect effect) {
        EmiStack stack = find(effect);
        if (stack == null) return false;
        EmiApi.displayRecipes(stack);
        return true;
    }
}
