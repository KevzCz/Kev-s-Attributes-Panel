package net.pixeldreamstudios.attributepanel.client;

import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.pixeldreamstudios.attributepanel.config.*;

public class ValueColorHelper {

    public static int getValueColor(StatEntry stat) {
        if (!AttributesPanelConfig.INSTANCE.enableColorCodedValues) {
            return 0xFFFFFF;
        }

        if (!stat.isChanged()) {
            return 0xFFFFFF;
        }

        double change = stat.current() - stat.base();

        if (Math.abs(change) < 0.001) {
            return 0xFFFFFF;
        }

        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(stat.attribute().value());
        if (attrId == null) {
            return 0xFFFFFF;
        }

        String idStr = attrId.toString();

        boolean positiveWhenHigher = AttributesPanelConfig.INSTANCE.positiveWhenHigher.contains(idStr);
        boolean positiveWhenLower = AttributesPanelConfig.INSTANCE.positiveWhenLower.contains(idStr);

        if (!positiveWhenHigher && !positiveWhenLower) {
            positiveWhenHigher = true;
        }

        boolean isIncrease = change > 0;

        boolean isPositiveChange = positiveWhenHigher == isIncrease;

        if (isPositiveChange) {
            return 0x00FF00;
        } else {
            return 0xFF4444;
        }
    }

    public static int applyLightTheme(int color, boolean isLightTheme) {
        if (!isLightTheme) {
            return color;
        }

        if (color == 0x00FF00) {
            return 0x00AA00;
        } else if (color == 0xFF4444) {
            return 0xCC0000;
        }

        return color;
    }
}
