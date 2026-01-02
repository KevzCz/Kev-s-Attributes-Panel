package net.pixeldreamstudios.attributepanel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.attributepanel.compat.IconLeadingCompat;

import java.util.ArrayList;
import java.util.List;
@Environment(EnvType.CLIENT)
public class AttributeDescriptionProvider {
    private static final String DESCRIPTION_PREFIX = "description.";

    public static TooltipContents getTooltip(Attribute attribute) {
        String descKey = DESCRIPTION_PREFIX + attribute.getDescriptionId();
        String desc = I18n.get(descKey);

        String id = BuiltInRegistries.ATTRIBUTE.getKey(attribute) != null
                ? BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString()
                : "[unregistered]";

        List<Component> lines = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();

        Component attrNameText = Component.translatable(attribute.getDescriptionId());
        IconLeadingCompat.IconSplit split = IconLeadingCompat.extractIcon(attrNameText);

        if (split.hasIcon()) {
            lines.add(Component.literal(split.leadingIcon + " ").append(Component.literal(split.cleanName)).withStyle(ChatFormatting.BOLD));
        } else {
            lines.add(attrNameText.copy().withStyle(ChatFormatting.BOLD));
        }
        icons.add(ItemStack.EMPTY);

        if (!desc.isEmpty() && !desc.equals(descKey)) {
            lines.add(Component.literal(desc));
            icons.add(ItemStack.EMPTY);
        }

        boolean advancedTooltips = isAdvancedTooltipsEnabled();
        boolean shiftDown = Screen.hasShiftDown();
        if (advancedTooltips || shiftDown) {
            lines.add(Component.literal(id).withStyle(ChatFormatting.DARK_GRAY));
            icons.add(ItemStack.EMPTY);
        }

        return new TooltipContents(lines, icons);
    }

    private static boolean isAdvancedTooltipsEnabled() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return false;
        return mc.options.advancedItemTooltips;
    }

    public record TooltipContents(List<Component> lines, List<ItemStack> icons) {}
}