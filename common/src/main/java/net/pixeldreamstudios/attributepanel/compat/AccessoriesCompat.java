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

public class AccessoriesCompat {

    public record AccessoryModifierSource(
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

    public static List<AccessoryModifierSource> getAccessoryModifierSources(Player player) {
        if (!isLoaded()) {
            return new ArrayList<>();
        }
        return getAccessoryModifierSourcesImpl(player);
    }

    @ExpectPlatform
    protected static List<AccessoryModifierSource> getAccessoryModifierSourcesImpl(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ItemStack> getAllEquippedAccessories(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isEquippedAsAccessory(Player player, ItemStack stack) {
        throw new AssertionError();
    }
}