package net.pixeldreamstudios.attributepanel.compat.fabric;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.attributepanel.compat.TrinketsCompat;
import net.pixeldreamstudios.attributepanel.util.ModifierIds;

import java.util.*;

public class TrinketsCompatImpl {

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("trinkets");
    }

    public static List<TrinketsCompat.TrinketModifierSource> getTrinketModifierSourcesImpl(Player player) {
        List<TrinketsCompat.TrinketModifierSource> results = new ArrayList<>();

        Optional<TrinketComponent> optComponent = TrinketsApi.getTrinketComponent(player);
        if (optComponent.isEmpty()) return results;

        TrinketComponent component = optComponent.get();

        var equipped = component.getEquipped(stack -> true);

        for (var pair : equipped) {
            SlotReference ref = pair.getA();
            ItemStack stack = pair.getB();

            if (stack.isEmpty()) continue;

            String slotType = ref.inventory().getSlotType().getName();

            Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
            if (trinket == null) continue;

            Multimap<Attribute, AttributeModifier> modifiers =
                    trinket.getModifiers(stack, ref, player, SlotAttributes.getUuid(ref));
            if (modifiers == null) continue;

            Collection<Map.Entry<Attribute, AttributeModifier>> entries = modifiers.entries();

            for (Map.Entry<Attribute, AttributeModifier> entry : entries) {
                Attribute attribute = entry.getKey();
                AttributeModifier modifier = entry.getValue();
                ResourceLocation rawId = ModifierIds.of(modifier);

                results.add(new TrinketsCompat.TrinketModifierSource(
                        stack,
                        modifier,
                        attribute,
                        rawId,
                        slotType
                ));
            }
        }

        return results;
    }

    public static List<ItemStack> getAllEquippedTrinkets(Player player) {
        List<ItemStack> results = new ArrayList<>();

        Optional<TrinketComponent> optComponent = TrinketsApi.getTrinketComponent(player);
        if (optComponent.isEmpty()) return results;

        var equipped = optComponent.get().getEquipped(stack -> true);

        for (var pair : equipped) {
            ItemStack stack = pair.getB();
            if (!stack.isEmpty()) {
                results.add(stack);
            }
        }

        return results;
    }

    public static boolean isEquippedAsTrinket(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        Optional<TrinketComponent> optComponent = TrinketsApi.getTrinketComponent(player);
        if (optComponent.isEmpty()) return false;

        var equipped = optComponent.get().getEquipped(itemStack -> true);

        for (var pair : equipped) {
            ItemStack equippedStack = pair.getB();
            if (ItemStack.matches(stack, equippedStack)) {
                return true;
            }
        }

        return false;
    }
}
