package net.pixeldreamstudios.attributepanel.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.util.ModifierIds;

public class AttributeDataSerializer {
    public static CompoundTag serialize(Player player) {
        CompoundTag root = new CompoundTag();

        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);

            if (id == null) continue;

            CompoundTag attrNbt = new CompoundTag();
            attrNbt.putDouble("Base", instance.getBaseValue());
            attrNbt.putDouble("Final", instance.getValue());

            ListTag flat = new ListTag();
            ListTag baseMult = new ListTag();
            ListTag totalMult = new ListTag();

            for (AttributeModifier mod : instance.getModifiers()) {
                CompoundTag modTag = new CompoundTag();

                ResourceLocation modId = ModifierIds.of(mod);
                modTag.putString("Id", modId.toString());
                modTag.putDouble("Value", mod.getAmount());
                modTag.putString("Source", guessSource(modId.getNamespace()));

                switch (mod.getOperation()) {
                    case ADDITION -> flat.add(modTag);
                    case MULTIPLY_BASE -> baseMult.add(modTag);
                    case MULTIPLY_TOTAL -> totalMult.add(modTag);
                }
            }

            for (var entry : player.getActiveEffects()) {
                var effect = entry.getEffect();
                int amplifier = entry.getAmplifier();

                AttributeModifier template = effect.getAttributeModifiers().get(attribute);
                if (template == null) continue;

                CompoundTag modTag = new CompoundTag();
                modTag.putString("Id", "effect:" + effect.getDescriptionId());
                modTag.putString("Source", "Vanilla");
                modTag.putDouble("Value", effect.getAttributeModifierValue(amplifier, template));

                switch (template.getOperation()) {
                    case ADDITION -> flat.add(modTag);
                    case MULTIPLY_BASE -> baseMult.add(modTag);
                    case MULTIPLY_TOTAL -> totalMult.add(modTag);
                }
            }

            attrNbt.put("Flat", flat);
            attrNbt.put("BaseMult", baseMult);
            attrNbt.put("TotalMult", totalMult);

            root.put(id.toString(), attrNbt);
        }

        return root;
    }

    private static String guessSource(String namespace) {
        return switch (namespace) {
            case "trinkets" -> "Trinket";
            case "accessories" -> "Accessories";
            case "puffish_skills" -> "Puffish Skill";
            case "tiered" -> "Tiered Item";
            case "minecraft" -> "Vanilla";
            default -> "Mod: " + namespace;
        };
    }
}
