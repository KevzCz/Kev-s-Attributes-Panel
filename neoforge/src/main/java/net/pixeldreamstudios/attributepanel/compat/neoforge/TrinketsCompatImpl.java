package net.pixeldreamstudios.attributepanel.compat.neoforge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.attributepanel.compat.TrinketsCompat;
// target mod has no 1.21.11 build yet; integration disabled until it ports.
public class TrinketsCompatImpl {

    public static boolean isLoaded() {
        return false;
    }

    public static List<TrinketsCompat.TrinketModifierSource> getTrinketModifierSourcesImpl(Player player) {
        // target mod has no 1.21.11 build; compat disabled
        return new java.util.ArrayList<>();
    }

    public static List<ItemStack> getAllEquippedTrinkets(Player player) {
        // target mod has no 1.21.11 build; compat disabled
        return new java.util.ArrayList<>();
    }

    public static boolean isEquippedAsTrinket(Player player, ItemStack stack) {
        // target mod has no 1.21.11 build; compat disabled
        return false;
    }
}
