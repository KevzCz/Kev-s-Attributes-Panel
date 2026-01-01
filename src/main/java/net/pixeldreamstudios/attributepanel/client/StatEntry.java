package net.pixeldreamstudios.attributepanel.client;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public record StatEntry(
        Text name,
        double base,
        double current,
        boolean percent,
        RegistryEntry<EntityAttribute> attribute,
        int bonusCount
) {
    public StatEntry(Text name, double base, double current, boolean percent, RegistryEntry<EntityAttribute> attribute) {
        this(name, base, current, percent, attribute, 0);
    }

    public boolean isChanged() {
        return Math.abs(current - base) > 0.001;
    }

    public boolean isNaN() {
        return Double.isNaN(current) || Double.isNaN(base);
    }
}