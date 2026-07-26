package net.pixeldreamstudios.attributepanel.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CuriosCompat {

    public record CurioModifierSource(
            ItemStack stack,
            AttributeModifier modifier,
            Attribute attribute,
            ResourceLocation rawId,
            String slotType
    ) {}

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    public static List<CurioModifierSource> getCurioModifierSources(Player player) {
        if (!isLoaded()) {
            return new ArrayList<>();
        }

        return getCurioModifierSourcesImpl(player);
    }

    @ExpectPlatform
    protected static List<CurioModifierSource> getCurioModifierSourcesImpl(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ItemStack> getAllEquippedCurios(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isEquippedAsCurio(Player player, ItemStack stack) {
        throw new AssertionError();
    }
}