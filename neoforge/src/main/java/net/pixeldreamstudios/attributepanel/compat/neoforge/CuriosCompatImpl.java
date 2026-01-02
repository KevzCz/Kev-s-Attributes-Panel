package net.pixeldreamstudios.attributepanel.compat.neoforge;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CuriosCompatImpl {

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static List<CuriosCompat.CurioModifierSource> getCurioModifierSourcesImpl(Player player) {
        List<CuriosCompat.CurioModifierSource> results = new ArrayList<>();

        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
        if (optional.isEmpty()) return results;

        ICuriosItemHandler handler = optional.get();

        for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
            String slotType = entry.getKey();
            ICurioStacksHandler stacksHandler = entry.getValue();

            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                if (stack.isEmpty()) continue;

                SlotContext slotContext = new SlotContext(slotType, player, i, false, true);

                if (stack.getItem() instanceof ICurioItem curioItem) {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("curios", slotType + "_" + i);
                    Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                            curioItem.getAttributeModifiers(slotContext, id, stack);

                    for (Map.Entry<Holder<Attribute>, AttributeModifier> modEntry : modifiers.entries()) {
                        Holder<Attribute> attribute = modEntry.getKey();
                        AttributeModifier modifier = modEntry.getValue();
                        ResourceLocation rawId = modifier.id();

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
        }

        return results;
    }

    public static List<ItemStack> getAllEquippedCurios(Player player) {
        List<ItemStack> results = new ArrayList<>();

        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
        if (optional.isEmpty()) return results;

        ICuriosItemHandler handler = optional.get();

        for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                if (! stack.isEmpty()) {
                    results.add(stack);
                }
            }
        }

        return results;
    }

    public static boolean isEquippedAsCurio(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
        if (optional.isEmpty()) return false;

        ICuriosItemHandler handler = optional.get();

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