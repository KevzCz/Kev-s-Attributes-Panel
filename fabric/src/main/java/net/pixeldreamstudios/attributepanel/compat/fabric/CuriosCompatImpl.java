package net.pixeldreamstudios.attributepanel.compat.fabric;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;

import java.util.ArrayList;
import java.util.List;

public class CuriosCompatImpl {

    public static boolean isLoaded() {
        return false;
    }

    public static List<CuriosCompat.CurioModifierSource> getCurioModifierSourcesImpl(Player player) {
        return new ArrayList<>();
    }

    public static List<ItemStack> getAllEquippedCurios(Player player) {
        return new ArrayList<>();
    }

    public static boolean isEquippedAsCurio(Player player, ItemStack stack) {
        return false;
    }
}