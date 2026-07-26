package net.pixeldreamstudios.attributepanel.compat.fabric;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.attributepanel.util.ModifierIds;
import net.pixeldreamstudios.attributepanel.compat.AccessoriesCompat;

import java.util.ArrayList;
import java.util.List;

public class AccessoriesCompatImpl {

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("accessories");
    }

    public static List<AccessoriesCompat.AccessoryModifierSource> getAccessoryModifierSourcesImpl(Player player) {
        List<AccessoriesCompat.AccessoryModifierSource> results = new ArrayList<>();

        var capability = AccessoriesCapability.get(player);
        if (capability == null) return results;

        var containers = capability.getContainers();
        if (containers == null) return results;

        for (var entry : containers.entrySet()) {
            var container = entry.getValue();
            String slotType = entry.getKey();

            for (int i = 0; i < container.getSize(); i++) {
                ItemStack stack = container.getAccessories().getItem(i);
                if (stack.isEmpty()) continue;

                SlotReference slotRef =
                        SlotReference.of(player, slotType, i);

                var builder = AccessoriesAPI.getAttributeModifiers(stack, slotRef);
                var modifiers = builder.getAttributeModifiers(false);

                for (var modEntry : modifiers.entries()) {
                    Attribute attribute = modEntry.getKey();
                    AttributeModifier modifier = modEntry.getValue();

                    results.add(new AccessoriesCompat.AccessoryModifierSource(
                            stack,
                            modifier,
                            attribute,
                            ModifierIds.of(modifier),
                            slotType
                    ));
                }
            }
        }

        return results;
    }

    public static List<ItemStack> getAllEquippedAccessories(Player player) {
        List<ItemStack> results = new ArrayList<>();

        var capability = AccessoriesCapability.get(player);
        if (capability == null) return results;

        var containers = capability.getContainers();
        if (containers == null) return results;

        for (var container : containers.values()) {
            for (int i = 0; i < container.getSize(); i++) {
                ItemStack stack = container.getAccessories().getItem(i);
                if (!stack.isEmpty()) {
                    results.add(stack);
                }
            }
        }

        return results;
    }

    public static boolean isEquippedAsAccessory(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        var capability = AccessoriesCapability.get(player);
        if (capability == null) return false;

        var containers = capability.getContainers();
        if (containers == null) return false;

        for (var container : containers.values()) {
            for (int i = 0; i < container.getSize(); i++) {
                ItemStack equippedStack = container.getAccessories().getItem(i);
                if (ItemStack.matches(stack, equippedStack)) {
                    return true;
                }
            }
        }

        return false;
    }
}