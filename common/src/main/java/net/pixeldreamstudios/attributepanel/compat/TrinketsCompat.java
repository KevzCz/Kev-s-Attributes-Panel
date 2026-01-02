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

public class TrinketsCompat {

    public record TrinketModifierSource(
            ItemStack stack,
            AttributeModifier modifier,
            Holder<Attribute> attribute,
            ResourceLocation rawId,
            String slotType
    ) {}


    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }


    public static List<TrinketModifierSource> getTrinketModifierSources(Player player) {
        if (!isLoaded()) {
            return new ArrayList<>();
        }

        return getTrinketModifierSourcesImpl(player);
    }


    @ExpectPlatform
    protected static List<TrinketModifierSource> getTrinketModifierSourcesImpl(Player player) {
        throw new AssertionError();
    }


    @ExpectPlatform
    public static List<ItemStack> getAllEquippedTrinkets(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isEquippedAsTrinket(Player player, ItemStack stack) {
        throw new AssertionError();
    }
}