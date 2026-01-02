package net.pixeldreamstudios.attributepanel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.pixeldreamstudios.attributepanel.compat.IconLeadingCompat;
@Environment(EnvType.CLIENT)
class VanillaAttributePanelDrawable {
    private final AttributePanelDrawable root;

    private static final ResourceLocation NAME_BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/light_gray_concrete.png");
    private static final ResourceLocation VALUE_BG = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gray_concrete.png");

    VanillaAttributePanelDrawable(AttributePanelDrawable root) {
        this.root = root;
    }

    void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Font font = root.mc().font;
        int rowHeight = 24;
        int padding = 2;

        int visibleRows = AttributePanelDrawable.MAX_ROWS;
        int totalPages = (int) Math.ceil(root.getCachedStats().size() / (float) visibleRows);
        root.setCurrentPage(Math.min(root.getCurrentPage(), Math.max(totalPages - 1, 0)));

        if (root.getShowOnlyChanged() && root.getCachedStats().isEmpty()) {
            String noStatsText = "No changed attributes";
            int textWidth = font.width(noStatsText);
            context.drawString(font, noStatsText, root.left() + (root.panelWidth() - textWidth) / 2, root.top() + 8, 0xAAAAAA, false);
            drawVanillaButtons(context, font, mouseX, mouseY, root.top() + root.panelHeight() - 12);
            return;
        }

        int startIndex = root.getCurrentPage() * visibleRows;
        int endIndex = Math.min(startIndex + visibleRows, root.getCachedStats().size());
        int rowY = root.top() + padding;

        int hoverIndex = -1;

        for (int i = startIndex; i < endIndex; i++) {
            StatEntry stat = root.getCachedStats().get(i);
            int rowIndex = i - startIndex;
            int yOffset = rowY + rowIndex * rowHeight;

            String rawName = stat.name().getString();
            String translationKey = Component.translatable(stat.attribute().value().getDescriptionId()).getString();

            IconLeadingCompat.IconSplit split = IconLeadingCompat.extractIconWithFallback(rawName, translationKey);
            String statName = split.cleanName;

            int maxNameWidth = root.panelWidth() / 2 - 8;

            String topLine, bottomLine = null;
            if (font.width(statName) <= maxNameWidth) {
                topLine = statName;
            } else {
                topLine = font.plainSubstrByWidth(statName, maxNameWidth);
                String remainder = statName.substring(topLine.length()).trim();
                if (!remainder.isEmpty()) {
                    bottomLine = font.plainSubstrByWidth(remainder, maxNameWidth);
                    if (font.width(remainder) > maxNameWidth) {
                        while (font.width(bottomLine + "...") > maxNameWidth && bottomLine.length() > 0) {
                            bottomLine = bottomLine.substring(0, bottomLine.length() - 1);
                        }
                        bottomLine += "...";
                    }
                }
            }

            int nameX = root.left() + 15;
            int valueX = root.left() + root.panelWidth() - 12;
            int nameY = yOffset + (rowHeight - 8) / 2;

            if (split.hasIcon()) {
                context.drawString(font, split.leadingIcon, nameX, nameY, 0x3A2F23, false);
                nameX += font.width(split.leadingIcon + " ");
            }

            if (bottomLine == null) {
                context.drawString(font, topLine, nameX, nameY, 0x3A2F23, false);
            } else {
                context.drawString(font, topLine, nameX, yOffset + 4, 0x3A2F23, false);
                context.drawString(font, bottomLine, nameX, yOffset + 12, 0x6D5C48, false);
            }

            String valueStr = stat.percent() ? String.format("%d%%", (int) (stat.current() * 100)) : String.format("%.2f", stat.current());
            int color = stat.isChanged() ? (stat.current() > stat.base() ? 0x55FF55 : 0xFF5555) : 0xAAAAAA;
            valueStr += stat.isChanged() ? (stat.current() > stat.base() ? " ↑" : " ↓") : "";

            int valueY = yOffset + (rowHeight - 8) / 2;
            context.drawString(font, valueStr, root.left() + root.panelWidth() - 4 - font.width(valueStr), valueY, color, false);

            context.fill(root.left(), yOffset + rowHeight - 1, root.left() + root.panelWidth(), yOffset + rowHeight, 0xFF444444);

            if (mouseX >= root.left() && mouseX <= root.left() + root.panelWidth() && mouseY >= yOffset && mouseY <= yOffset + rowHeight) {
                hoverIndex = i;
            }
        }

        drawVanillaTooltipButton(context, font, hoverIndex, mouseX, mouseY, rowHeight, padding);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        int btnY = root.top() + root.panelHeight() - 12;

        String prevText = "« Prev";
        String nextText = "Next »";
        String checkLabel = "[ " + (root.getShowOnlyChanged() ? "✓" : " ") + " ]";

        int spacing = 12;

        Font font = root.mc().font;
        int prevWidth = font.width(prevText);
        int checkWidth = font.width(checkLabel);
        int nextWidth = font.width(nextText);

        int centerX = root.left() + root.panelWidth() / 2;

        int prevX = centerX - checkWidth / 2 - spacing - prevWidth;
        int checkX = centerX - checkWidth / 2;
        int nextX = centerX + checkWidth / 2 + spacing;

        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, prevX, btnY, prevWidth, 10)) {
            if (root.getCurrentPage() > 0) root.setCurrentPage(root.getCurrentPage() - 1);
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.5F);
            return true;
        }

        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, nextX, btnY, nextWidth, 10)) {
            int totalPages = (int) Math.ceil(root.getCachedStats().size() / (float) AttributePanelDrawable.MAX_ROWS);
            if (root.getCurrentPage() < totalPages - 1) root.setCurrentPage(root.getCurrentPage() + 1);
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.5F);
            return true;
        }

        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, checkX, btnY, checkWidth, 10)) {
            root.setShowOnlyChanged(!root.getShowOnlyChanged());
            root.setCurrentPage(0);
            root.cacheStats();
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 0.5F);
            return true;
        }

        return false;
    }

    private void drawVanillaButtons(GuiGraphics context, Font font, int mouseX, int mouseY, int btnY) {
        String prevText = "« Prev";
        String nextText = "Next »";
        String checkLabel = "[ " + (root.getShowOnlyChanged() ? "✓" : " ") + " ]";

        int spacing = 12;
        int buttonHeight = 10;

        int prevWidth = font.width(prevText);
        int checkWidth = font.width(checkLabel);
        int nextWidth = font.width(nextText);

        int centerX = root.left() + root.panelWidth() / 2;

        int prevX = centerX - checkWidth / 2 - spacing - prevWidth;
        int checkX = centerX - checkWidth / 2;
        int nextX = centerX + checkWidth / 2 + spacing;

        context.drawString(font, prevText, prevX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, prevX, btnY, prevWidth, buttonHeight) ? 0xFFFFFF : 0xAAAAAA, false);
        context.drawString(font, checkLabel, checkX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, checkX, btnY, checkWidth, buttonHeight) ? 0xFFFFFF : 0xAAAAAA, false);
        context.drawString(font, nextText, nextX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, nextX, btnY, nextWidth, buttonHeight) ? 0xFFFFFF : 0xAAAAAA, false);
    }

    private void drawVanillaTooltipButton(GuiGraphics context, Font font, int hoverIndex, int mouseX, int mouseY, int rowHeight, int padding) {
        root.drawTooltipContent(context, font, hoverIndex, mouseX, mouseY, rowHeight, padding);
        int btnY = root.top() + root.panelHeight() - 12;
        drawVanillaButtons(context, font, mouseX, mouseY, btnY);
    }
}