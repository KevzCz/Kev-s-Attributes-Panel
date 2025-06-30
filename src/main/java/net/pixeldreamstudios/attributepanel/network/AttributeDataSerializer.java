package net.pixeldreamstudios.attributepanel.network;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class AttributeDataSerializer {
    public static NbtCompound serialize(PlayerEntity player) {
        NbtCompound root = new NbtCompound();

        for (RegistryEntry<EntityAttribute> attributeEntry : Registries.ATTRIBUTE.streamEntries().toList()) {
            EntityAttributeInstance instance = player.getAttributeInstance(attributeEntry);
            if (instance == null) continue;

            Identifier id = attributeEntry.getKey().map(key -> key.getValue()).orElse(null);
            if (id == null) continue;

            NbtCompound attrNbt = new NbtCompound();
            attrNbt.putDouble("Base", instance.getBaseValue());
            attrNbt.putDouble("Final", instance.getValue());

            NbtList flat = new NbtList();
            NbtList baseMult = new NbtList();
            NbtList totalMult = new NbtList();

            for (EntityAttributeModifier mod : instance.getModifiers()) {
                NbtCompound modTag = new NbtCompound();
                modTag.putString("Id", mod.id().toString());
                modTag.putDouble("Value", mod.value());
                modTag.putString("Source", guessSource(mod.id().getNamespace()));

                switch (mod.operation()) {
                    case ADD_VALUE -> flat.add(modTag);
                    case ADD_MULTIPLIED_BASE -> baseMult.add(modTag);
                    case ADD_MULTIPLIED_TOTAL -> totalMult.add(modTag);
                }
            }

            if (player instanceof LivingEntity living) {
                for (var entry : living.getStatusEffects()) {
                    var effect = entry.getEffectType().value();
                    int amplifier = entry.getAmplifier();

                    effect.forEachAttributeModifier(amplifier, (attribute, modifier) -> {
                        if (!attribute.equals(attributeEntry.value())) return;

                        NbtCompound modTag = new NbtCompound();
                        modTag.putString("Id", "effect:" + effect.getTranslationKey());
                        modTag.putString("Source", "Vanilla");
                        modTag.putDouble("Value", modifier.value());

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
