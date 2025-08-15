package net.pixeldreamstudios.attributepanel.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.*;


class BookAttributePanelDrawable {
    private final AttributePanelDrawable root;

    private static final Identifier BOOK_TEXTURE = Identifier.of("minecraft", "textures/gui/book.png");
    private static final Identifier INFO_ICON   = Identifier.of("kevs-attributes-panel", "textures/gui/attribute_book.png");
    private static final int INFO_ICON_SIZE = 8;


    BookAttributePanelDrawable(AttributePanelDrawable root) {
        this.root = root;
    }

    void render(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer tr = root.mc().textRenderer;
        int rowHeight = 20;
        int padding = 20;

        context.drawTexture(BOOK_TEXTURE, root.left() - 25, root.top(), 0, 0, 240, 230, 240, 230);

        int visibleRows = AttributePanelDrawable.MAX_ROWS;
        int totalPages = (int) Math.ceil(root.getCachedStats().size() / (float) visibleRows);
        root.setCurrentPage(Math.min(root.getCurrentPage(), Math.max(totalPages - 1, 0)));

        if (root.getShowOnlyChanged() && root.getCachedStats().isEmpty()) {
            String noStatsText = "No changed attributes";
            int textWidth = tr.getWidth(noStatsText);
            drawBookButtons(context, tr, mouseX, mouseY, root.top() + root.panelHeight() - 20);
            context.drawText(tr, noStatsText, root.left() + (root.panelWidth() - textWidth) / 2 + 5, root.top() + 20, Formatting.DARK_GRAY.getColorValue(), false);
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

            String statName = stat.name().getString();
            int maxNameWidth = root.panelWidth() / 2 - 7;

            context.fill(root.left() + 12, yOffset + rowHeight + 2, root.left() + root.panelWidth() - 12, yOffset + rowHeight + 1, 0xFFD6C4A3);

            String topLine, bottomLine = null;
            if (tr.getWidth(statName) <= maxNameWidth) {
                topLine = statName;
            } else {
                topLine = tr.trimToWidth(statName, maxNameWidth);
                String remainder = statName.substring(topLine.length()).trim();
                if (!remainder.isEmpty()) {
                    bottomLine = tr.trimToWidth(remainder, maxNameWidth);
                    if (tr.getWidth(remainder) > maxNameWidth) {
                        while (tr.getWidth(bottomLine + "...") > maxNameWidth && bottomLine.length() > 0) {
                            bottomLine = bottomLine.substring(0, bottomLine.length() - 1);
                        }
                        bottomLine += "...";
                    }
                }
            }

            int nameX = root.left() + 15;
            int valueX = root.left() + root.panelWidth() - 12;
            int nameY = yOffset + (rowHeight - 8) / 2;

            if (bottomLine == null) {
                context.drawText(tr, topLine, nameX, nameY, 0x3A2F23, false);
            } else {
                context.drawText(tr, topLine, nameX, yOffset + 4, 0x3A2F23, false);
                context.drawText(tr, bottomLine, nameX, yOffset + 12, 0x6D5C48, false);
            }

            String valueStr = stat.percent() ? String.format("%d%%", (int) (stat.current() * 100)) : String.format("%.2f", stat.current());
            int color = stat.isChanged() ? (stat.current() > stat.base() ? Formatting.GREEN.getColorValue() : Formatting.RED.getColorValue()) : Formatting.GRAY.getColorValue();
            valueStr += stat.isChanged() ? (stat.current() > stat.base() ? " ↑" : " ↓") : "";

            context.drawText(tr, valueStr, valueX - tr.getWidth(valueStr), nameY, color, false);

            if (mouseX >= root.left() && mouseX <= root.left() + root.panelWidth() && mouseY >= yOffset && mouseY <= yOffset + rowHeight) {
                hoverIndex = i;
            }
        }

        int infoX = root.left() + root.panelWidth() - INFO_ICON_SIZE - 95;
        int infoY = root.top() + 10;

        context.drawTexture(INFO_ICON, infoX, infoY,
                0, 0, INFO_ICON_SIZE, INFO_ICON_SIZE,
                INFO_ICON_SIZE, INFO_ICON_SIZE);

        if (mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE
                && mouseY >= infoY && mouseY <= infoY + INFO_ICON_SIZE) {
            drawGlobalBonusTooltip(mouseX, mouseY);
        }

        drawBookTooltipButton(context, tr, hoverIndex, mouseX, mouseY, rowHeight, padding);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {

        int infoX = root.left() + root.panelWidth() - INFO_ICON_SIZE - 95;
        int infoY = root.top() + 10;

        int btnY = root.top() + root.panelHeight() - 20;
        ButtonCoords coords = getBookButtonCoords(root.mc().textRenderer);

        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, coords.prevX(), btnY, coords.prevW(), 10)) {
            if (root.getCurrentPage() > 0) root.setCurrentPage(root.getCurrentPage() - 1);
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
            return true;
        }
        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, coords.nextX(), btnY, coords.nextW(), 10)) {
            int totalPages = (int) Math.ceil(root.getCachedStats().size() / (float) AttributePanelDrawable.MAX_ROWS);
            if (root.getCurrentPage() < totalPages - 1) root.setCurrentPage(root.getCurrentPage() + 1);
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
            return true;
        }
        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, coords.checkX(), btnY, coords.checkW(), 10)) {
            root.setShowOnlyChanged(!root.getShowOnlyChanged());
            root.setCurrentPage(0);
            root.cacheStats();
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
            return true;
        }

        return false;
    }

    private record ButtonCoords(int prevX, int checkX, int nextX, int prevW, int checkW, int nextW) {}

    private ButtonCoords getBookButtonCoords(TextRenderer tr) {
        String prevText = "««";
        String nextText = "»»";
        String checkLabel = "[ " + (root.getShowOnlyChanged() ? "✓" : " ") + " ]";

        int spacing = 24;

        int prevW = tr.getWidth(prevText);
        int checkW = tr.getWidth(checkLabel);
        int nextW = tr.getWidth(nextText);

        int totalWidth = prevW + spacing + checkW + spacing + nextW;
        int startX = root.left() + (root.panelWidth() - totalWidth) / 2;

        int prevX = startX;
        int checkX = prevX + prevW + spacing;
        int nextX = checkX + checkW + spacing;

        return new ButtonCoords(prevX, checkX, nextX, prevW, checkW, nextW);
    }

    private void drawBookButtons(DrawContext context, TextRenderer tr, int mouseX, int mouseY, int btnY) {
        String prevText = "««";
        String nextText = "»»";
        String checkLabel = "[ " + (root.getShowOnlyChanged() ? "✓" : " ") + " ]";

        int spacing = 24;
        int buttonHeight = 10;

        int prevWidth = tr.getWidth(prevText);
        int checkWidth = tr.getWidth(checkLabel);
        int nextWidth = tr.getWidth(nextText);

        int totalWidth = prevWidth + spacing + checkWidth + spacing + nextWidth;
        int startX = root.left() + (root.panelWidth() - totalWidth) / 2;

        int prevX = startX;
        int checkX = prevX + prevWidth + spacing;
        int nextX = checkX + checkWidth + spacing;

        context.drawText(tr, prevText, prevX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, prevX, btnY, prevWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);
        context.drawText(tr, checkLabel, checkX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, checkX, btnY, checkWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);
        context.drawText(tr, nextText, nextX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, nextX, btnY, nextWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);
    }

    private void drawBookTooltipButton(DrawContext context, TextRenderer tr, int hoverIndex, int mouseX, int mouseY, int rowHeight, int padding) {
        root.drawTooltipContent(context, tr, hoverIndex, mouseX, mouseY, rowHeight, padding);
        int btnY = root.top() + root.panelHeight() - 20;
        drawBookButtons(context, tr, mouseX, mouseY, btnY);
    }

    private void drawGlobalBonusTooltip(int mouseX, int mouseY) {
        PlayerEntity player = root.mc().player;
        if (player == null) return;

        Map<String, Double> flatMap = new TreeMap<>();
        Map<String, Double> baseMultMap = new TreeMap<>();
        Map<String, Double> totalMultMap = new TreeMap<>();

        for (RegistryEntry<EntityAttribute> entry : Registries.ATTRIBUTE.streamEntries().toList()) {
            EntityAttribute attr = entry.value();
            EntityAttributeInstance instance = player.getAttributeInstance(entry);
            if (instance == null || instance.getModifiers().isEmpty()) continue;

            String attrName = Text.translatable(attr.getTranslationKey()).getString();

            for (EntityAttributeModifier mod : instance.getModifiers()) {
                double value = mod.value();
                if (Math.abs(value) < 0.0001) continue;

                switch (mod.operation()) {
                    case ADD_VALUE -> flatMap.merge(attrName, value, Double::sum);
                    case ADD_MULTIPLIED_BASE -> baseMultMap.merge(attrName, value, Double::sum);
                    case ADD_MULTIPLIED_TOTAL -> totalMultMap.merge(attrName, value, Double::sum);
                }
            }
        }

        List<Text> lines = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();

        lines.add(Text.translatable("attributepanel.message.bonuses").formatted(Formatting.GOLD));
        icons.add(ItemStack.EMPTY);

        boolean addedAny = false;

        for (String attr : flatMap.keySet()) {
            double value = flatMap.get(attr);
            lines.add(Text.literal(String.format("- %s: %+,.2f", attr, value)).formatted(Formatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }
        for (String attr : baseMultMap.keySet()) {
            double value = baseMultMap.get(attr);
            lines.add(Text.literal(String.format("- %s: %+d%% Base", attr, (int)(value * 100))).formatted(Formatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }
        for (String attr : totalMultMap.keySet()) {
            double value = totalMultMap.get(attr);
            lines.add(Text.literal(String.format("- %s: %+d%% Total", attr, (int)(value * 100))).formatted(Formatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }

        if (!addedAny) {
            lines.add(Text.literal("No active modifiers").formatted(Formatting.GRAY));
            icons.add(ItemStack.EMPTY);
        }

        root.enqueueTooltip(lines, icons, mouseX, mouseY);
    }
}
