package net.pixeldreamstudios.attributepanel.client;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;

public record StatEntry(
        Component name,
        double base,
        double current,
        boolean percent,
        Holder<Attribute> attribute,
        int bonusCount,
        double rawBase,
        double rawCurrent
) {
    public StatEntry(Component name, double base, double current, boolean percent, Holder<Attribute> attribute) {
        this(name, base, current, percent, attribute, 0, base, current);
    }

    public StatEntry(Component name, double base, double current, boolean percent, Holder<Attribute> attribute, int bonusCount) {
        this(name, base, current, percent, attribute, bonusCount, base, current);
    }

    public boolean isChanged() {
        return Math.abs(rawCurrent - rawBase) > 0.001;
    }

    public boolean isNaN() {
        return Double.isNaN(current) || Double.isNaN(base);
    }
}