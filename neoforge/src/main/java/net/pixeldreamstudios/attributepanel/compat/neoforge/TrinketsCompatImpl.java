package net.pixeldreamstudios.attributepanel.compat.neoforge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.attributepanel.compat.TrinketsCompat;

import java.util.ArrayList;
import java.util.List;

public class TrinketsCompatImpl {

    public static boolean isLoaded() {
        return false;
    }

    public static List<TrinketsCompat.TrinketModifierSource> getTrinketModifierSourcesImpl(Player player) {
        return new ArrayList<>();
    }

    public static List<ItemStack> getAllEquippedTrinkets(Player player) {
        return new ArrayList<>();
    }

    public static boolean isEquippedAsTrinket(Player player, ItemStack stack) {
        return false;
    }
}