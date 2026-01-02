package net.pixeldreamstudios.attributepanel.network;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class AttributeDataSerializer {
    public static CompoundTag serialize(Player player) {
        CompoundTag root = new CompoundTag();

        for (Holder<Attribute> attributeHolder : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance instance = player.getAttribute(attributeHolder);
            if (instance == null) continue;

            ResourceLocation id = attributeHolder.unwrapKey()
                    .map(key -> key.location())
                    .orElse(null);

            if (id == null) continue;

            CompoundTag attrNbt = new CompoundTag();
            attrNbt.putDouble("Base", instance.getBaseValue());
            attrNbt.putDouble("Final", instance.getValue());

            ListTag flat = new ListTag();
            ListTag baseMult = new ListTag();
            ListTag totalMult = new ListTag();

            for (AttributeModifier mod : instance.getModifiers()) {
                CompoundTag modTag = new CompoundTag();

                modTag.putString("Id", mod.id().toString());
                modTag.putDouble("Value", mod.amount());
                modTag.putString("Source", guessSource(mod.id().getNamespace()));

                switch (mod.operation()) {
                    case ADD_VALUE -> flat.add(modTag);
                    case ADD_MULTIPLIED_BASE -> baseMult.add(modTag);
                    case ADD_MULTIPLIED_TOTAL -> totalMult.add(modTag);
                }
            }

            if (player instanceof LivingEntity living) {
                for (var entry : living.getActiveEffects()) {
                    var effect = entry.getEffect().value();
                    int amplifier = entry.getAmplifier();

                    effect.createModifiers(amplifier, (attribute, modifier) -> {
                        if (!attribute.equals(attributeHolder.value())) return;

                        CompoundTag modTag = new CompoundTag();
                        modTag.putString("Id", "effect:" + effect.getDescriptionId());
                        modTag.putString("Source", "Vanilla");
                        modTag.putDouble("Value", modifier.amount());

                        switch (modifier.operation()) {
                            case ADD_VALUE -> flat.add(modTag);
                            case ADD_MULTIPLIED_BASE -> baseMult.add(modTag);
                            case ADD_MULTIPLIED_TOTAL -> totalMult.add(modTag);
                        }
                    });
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