package net.pixeldreamstudios.attributepanel.compat.forge;

import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;
import net.pixeldreamstudios.attributepanel.util.ModifierIds;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CuriosCompatImpl {

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    private static ICuriosItemHandler handlerFor(Player player) {
        LazyOptional<ICuriosItemHandler> lazy = CuriosApi.getCuriosInventory(player);
        return lazy.resolve().orElse(null);
    }

    public static List<CuriosCompat.CurioModifierSource> getCurioModifierSourcesImpl(Player player) {
        List<CuriosCompat.CurioModifierSource> results = new ArrayList<>();

        ICuriosItemHandler handler = handlerFor(player);
        if (handler == null) return results;

        for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
            String slotType = entry.getKey();
            ICurioStacksHandler stacksHandler = entry.getValue();

            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                if (stack.isEmpty()) continue;

                SlotContext slotContext = new SlotContext(slotType, player, i, false, true);

                Multimap<Attribute, AttributeModifier> modifiers = CuriosApi.getAttributeModifiers(
                        slotContext, CuriosApi.getSlotUuid(slotContext), stack);
                if (modifiers == null) continue;

                for (Map.Entry<Attribute, AttributeModifier> modEntry : modifiers.entries()) {
                    Attribute attribute = modEntry.getKey();
                    AttributeModifier modifier = modEntry.getValue();
                    ResourceLocation rawId = ModifierIds.of(modifier);

                    results.add(new CuriosCompat.CurioModifierSource(
                            stack,
                            modifier,
                            attribute,
                            rawId,
                            slotType
                    ));
                }
            }
        }

        return results;
    }

    public static List<ItemStack> getAllEquippedCurios(Player player) {
        List<ItemStack> results = new ArrayList<>();

        ICuriosItemHandler handler = handlerFor(player);
        if (handler == null) return results;

        for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    results.add(stack);
                }
            }
        }

        return results;
    }

    public static boolean isEquippedAsCurio(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        ICuriosItemHandler handler = handlerFor(player);
        if (handler == null) return false;

        for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack equippedStack = stacksHandler.getStacks().getStackInSlot(i);
                if (ItemStack.matches(stack, equippedStack)) {
                    return true;
                }
            }
        }

        return false;
    }
}
