package net.pixeldreamstudios.attributepanel.compat.neoforge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.attributepanel.compat.AccessoriesCompat;
// target mod has no 1.21.11 build yet; integration disabled until it ports.
public class AccessoriesCompatImpl {

    public static boolean isLoaded() {
        return false;
    }

    public static List<AccessoriesCompat.AccessoryModifierSource> getAccessoryModifierSourcesImpl(Player player) {
        // target mod has no 1.21.11 build; compat disabled
        return new java.util.ArrayList<>();
    }

    public static List<ItemStack> getAllEquippedAccessories(Player player) {
        // target mod has no 1.21.11 build; compat disabled
        return new java.util.ArrayList<>();
    }

    public static boolean isEquippedAsAccessory(Player player, ItemStack stack) {
        // target mod has no 1.21.11 build; compat disabled
        return false;
    }
}
