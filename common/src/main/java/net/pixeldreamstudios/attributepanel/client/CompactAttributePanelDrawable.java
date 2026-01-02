package net.pixeldreamstudios.attributepanel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;
import net.pixeldreamstudios.attributepanel.compat.IconLeadingCompat;
import net.pixeldreamstudios.attributepanel.compat.TrinketsCompat;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable.formatModifierId;
@Environment(EnvType.CLIENT)
public class CompactAttributePanelDrawable implements Renderable, GuiEventListener, NarratableEntry {
    private final AttributePanelDrawable root;

    private static final int HEADER_AFTER_GAP = 10;
    private static final float DEFAULT_FONT_SCALE = 0.55f;
    private static final float HEADER_FONT_SCALE = 1f;
    private static final int ROW_H_TEXT = 9;
    private static final int HEADER_ICON_SIZE = 9;
    private static final int ATTR_ICON_SIZE = 9;
    private static final int TEX_ICON_SRC_PX = 16;

    private static final int DIVIDER_TEXTURE_W = 64;
    private static final int DIVIDER_TEXTURE_H = 5;
    private static final int DIVIDER_PADDING = 4;
    private static final int DIVIDER_TOTAL_H = DIVIDER_TEXTURE_H + DIVIDER_PADDING;
    private static final int BULLET_SIZE = 3;
    private static final int DEFAULT_SIDE_PADDING = 15;
    private static final int PADDING_TOP = 20;
    private static final int PADDING_BOTTOM = 6;

    private static final int SCROLLBAR_GAP = 3;
    private static final float SCROLLBAR_W = 3.15f;
    private static final int SCROLL_STEP_PX = 18;

    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/gui.png");
    private static final ResourceLocation DIVIDER_TEXTURE = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/divider.png");
    private static final ResourceLocation PERMA = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/perma.png");
    private static final ResourceLocation EQUIPPED = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/equipped.png");
    private static final ResourceLocation DD_POWER_ICON = ResourceLocation.fromNamespaceAndPath("dungeon_difficulty", "textures/symbol/power_level.png");
    private static final ResourceLocation FTB_QUEST_BOOK_ICON = ResourceLocation.fromNamespaceAndPath("ftbquests", "textures/item/book.png");
    private static final ResourceLocation INFO_ICON = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/info.png");
    private static final ResourceLocation CHANGED = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/changed.png");
    private static final int INFO_ICON_SIZE = 12;
    private static final ResourceLocation SEARCH_ICON = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/search.png");
    private static final int SEARCH_ICON_SIZE = 12;
    private boolean searchVisible = false;
    private boolean searchFocused = false;
    private String searchText = "";
    private static final int SEARCH_BAR_H = 14;
    private static final int SEARCH_BAR_MARGIN = 6;

    private final List<Item> items = new ArrayList<>();
    private final List<Integer> prefixHeights = new ArrayList<>();
    private static final int ICON_VALUE_GAP = 5;
    private int scrollPx = 0;
    private static final int BULLET_Y_NUDGE = -3;
    private boolean draggingBar = false;
    private int dragGrabOffset = 0;

    private static final DecimalFormatSymbols DFS = new DecimalFormatSymbols(Locale.US);
    private static final ThreadLocal<DecimalFormat> NUM_FMT = ThreadLocal.withInitial(() -> {
        DecimalFormat df = new DecimalFormat("#.###", DFS);
        df.setGroupingUsed(false);
        return df;
    });

    private boolean hoveringInfoIcon = false;
    private int globalTooltipScrollOffset = 0;
    private static final int GLOBAL_TOOLTIP_SCROLL_STEP = 10;
    private TooltipRenderData cachedTooltipData = null;

    private static class TooltipRenderData {
        List<Component> allLines;
        List<ItemStack> allIcons;
        int tooltipWidth;
        int tooltipHeight;
        int maxVisibleLines;
        int totalLines;

        TooltipRenderData(List<Component> allLines, List<ItemStack> allIcons, int tooltipWidth,
                          int tooltipHeight, int maxVisibleLines, int totalLines) {
            this.allLines = allLines;
            this.allIcons = allIcons;
            this.tooltipWidth = tooltipWidth;
            this.tooltipHeight = tooltipHeight;
            this.maxVisibleLines = maxVisibleLines;
            this.totalLines = totalLines;
        }
    }

    public CompactAttributePanelDrawable(AttributePanelDrawable root) {
        this.root = root;
    }

    private boolean isSearching() {
        return searchText != null && !searchText.isBlank();
    }

    private static String fmtValue(StatEntry stat, boolean inBonusesGroup) {
        if (stat.isNaN()) {
            return inBonusesGroup ? "" : (stat.bonusCount() + " bonuses");
        }

        double v = stat.percent() ?  stat.current() * 100.0 : stat.current();
        if (Math.abs(v) < 1e-9) v = 0;
        String s = NUM_FMT.get().format(v);
        return stat.percent() ? s + "%" : s;
    }

    private float fontScale() {
        var cfg = AttributesPanelConfig.INSTANCE.compact;
        if (cfg != null && cfg.textScale > 0f) return cfg.textScale;
        return DEFAULT_FONT_SCALE;
    }

    private int sidePadding() {
        var cfg = AttributesPanelConfig.INSTANCE.compact;
        if (cfg != null) return cfg.sidePadding;
        return DEFAULT_SIDE_PADDING;
    }

    private int iconColW() {
        return (int) Math.ceil(ATTR_ICON_SIZE * fontScale());
    }

    private boolean isLightTextTheme() {
        var cfg = AttributesPanelConfig.INSTANCE.compact;
        return cfg != null && cfg.textTheme == AttributesPanelConfig.TextTheme.LIGHT;
    }

    private int colorBody() {
        return isLightTextTheme() ? 0x1E1E1E : 0xFFFFFF;
    }

    private int colorHeader() {
        return isLightTextTheme() ? 0x111827 : 0xFFFFFF;
    }

    private int colorMuted() {
        return isLightTextTheme() ? 0x6B7280 : 0xAAAAAA;
    }

    private int colorBullet() {
        return isLightTextTheme() ? 0x2D2D2D : 0xFFFFFF;
    }

    private int colorSearchText() {
        return isLightTextTheme() ? 0x1E1E1E : 0xFFFFFF;
    }

    private int colorSearchPlaceholder() {
        return isLightTextTheme() ? 0x888888 : 0xCCCCCC;
    }

    private int colorCaretFill() {
        return isLightTextTheme() ? 0xFF000000 : 0xFFFFFFFF;
    }

    private int colorSelectionFill() {
        return isLightTextTheme() ? 0x66333333 : 0x66FFFFFF;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        Font font = root.mc().font;

        int bgX = root.left() - 16;
        int bgY = root.top();
        int bgW = root.panelWidth() + 16;
        int bgH = root.panelHeight();

        ctx.blit(BACKGROUND_TEXTURE, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);

        buildLayout();

        int padX = sidePadding();
        int innerX = bgX + padX;
        int innerW = bgW - (padX * 2);
        int innerY = bgY + PADDING_TOP;
        int innerH = Math.max(0, bgH - (PADDING_TOP + PADDING_BOTTOM));

        int contentH = totalContentHeight();
        int maxOffset = Math.max(0, contentH - innerH);
        if (scrollPx > maxOffset) scrollPx = maxOffset;
        if (scrollPx < 0) scrollPx = 0;

        int iconsY = bgY + 9;

        int searchX = bgX + 7;
        ctx.blit(SEARCH_ICON, searchX, iconsY, 0, 0, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
        if (mouseX >= searchX && mouseX <= searchX + SEARCH_ICON_SIZE &&
                mouseY >= iconsY && mouseY <= iconsY + SEARCH_ICON_SIZE) {
            root.enqueueTooltip(List.of(Component.translatable("attributepanel.search.tooltip", "Search Attributes")),
                    List.of(ItemStack.EMPTY), mouseX, mouseY);
        }

        int infoX = bgX + bgW - INFO_ICON_SIZE - 7;
        ctx.blit(INFO_ICON, infoX, iconsY, 0, 0, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE);

        hoveringInfoIcon = mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE &&
                mouseY >= iconsY && mouseY <= iconsY + INFO_ICON_SIZE;

        if (hoveringInfoIcon) {
            drawGlobalBonusTooltip(ctx, mouseX, mouseY);
        } else {
            globalTooltipScrollOffset = 0;
            cachedTooltipData = null;
        }

        if (searchVisible) {
            int barW = bgW;
            int barH = SEARCH_BAR_H;
            int barX = bgX;
            int barY = bgY - barH - SEARCH_BAR_MARGIN;

            ctx.fill(barX, barY, barX + barW, barY + barH, 0xCC000000);
            ctx.fill(barX, barY, barX + barW, barY + 1, 0xFFAAAAAA);
            ctx.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFFAAAAAA);
            ctx.fill(barX, barY, barX + 1, barY + barH, 0xFFAAAAAA);
            ctx.fill(barX + barW - 1, barY, barX + barW, barY + barH, 0xFFAAAAAA);

            font = root.mc().font;
            int textX = barX + 6;
            int textY = barY + (barH - font.lineHeight) / 2;
            int visibleW = Math.max(1, barW - 12);

            int caretClamped = Math.max(0, Math.min(caret, searchText.length()));
            if (searchText.isEmpty()) textScrollPx = 0;

            int textW = font.width(searchText);
            int caretPx = font.width(searchText.substring(0, caretClamped));
            int maxScroll = Math.max(0, textW - visibleW);

            if (caretPx - textScrollPx > visibleW) textScrollPx = caretPx - visibleW;
            if (caretPx - textScrollPx < 0) textScrollPx = caretPx;
            if (textScrollPx < 0) textScrollPx = 0;
            if (textScrollPx > maxScroll) textScrollPx = maxScroll;

            String toShow = searchText.isBlank() ? "Search…" : searchText;
            int color = searchText.isBlank() ? colorSearchPlaceholder() : colorSearchText();

            ctx.enableScissor(barX + 1, barY + 1, barX + barW - 1, barY + barH - 1);

            if (!searchText.isBlank() && hasSelection()) {
                int a = Math.min(selA(), searchText.length());
                int b = Math.min(selB(), searchText.length());
                if (a != b) {
                    int beforeW = font.width(searchText.substring(0, a));
                    int selW = font.width(searchText.substring(a, b));
                    int sx0 = textX + beforeW - textScrollPx;
                    int sx1 = sx0 + selW;
                    ctx.fill(sx0, barY + 3, sx1, barY + barH - 3, colorSelectionFill());
                }
            }

            int drawX = textX - textScrollPx;
            ctx.drawString(font, toShow, drawX, textY, color, false);

            if (searchFocused) {
                int caretDrawX = textX + caretPx - textScrollPx;
                ctx.fill(caretDrawX, barY + 3, caretDrawX + 1, barY + barH - 3, colorCaretFill());
            }

            ctx.disableScissor();
        }

        if (items.isEmpty()) {
            drawEmptyState(ctx, font, innerX, innerW, innerY);
            return;
        }

        ctx.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        int idx = firstVisibleIndex(scrollPx);
        int y = innerY - (scrollPx - prefixAt(idx)) + (isSearching() ? 5 : 0);

        while (idx < items.size()) {
            Item it = items.get(idx);
            if (y + it.height > innerY + innerH) break;

            switch (it.type) {
                case DIVIDER -> drawDivider(ctx, innerX, innerW, y);
                case HEADER -> drawHeader(ctx, font, innerX, innerW, y, it.header);
                case STAT -> {
                    drawStat(ctx, font, innerX, y, it.stat);

                    if (mouseX >= innerX && mouseX <= innerX + innerW &&
                            mouseY >= y && mouseY <= y + ROW_H_TEXT) {
                        handleStatHover(font, innerX, y, it.stat, mouseX, mouseY);
                    }
                }
            }

            y += it.height;
            idx++;
        }

        ctx.disableScissor();

        drawScrollbar(ctx, bgX, bgY, bgW, innerY, innerH, contentH);
    }

    private void drawEmptyState(GuiGraphics ctx, Font font, int innerX, int innerW, int innerY) {
        String msg = isSearching()
                ? "No attributes match \"" + searchText + "\""
                : "No attributes";

        int wrapWidth = Math.max(1, innerW - 8);
        int x = innerX + 4;
        int y = innerY + 7;

        Component component = Component.literal(msg);
        List<FormattedCharSequence> lines = font.split(component, wrapWidth);

        for (FormattedCharSequence line : lines) {
            ctx.drawString(font, line, x, y, colorMuted(), true);
            y += font.lineHeight + 2;
        }
    }

    private ResourceLocation resolveAttrIcon(ResourceLocation attrId, AttributesPanelConfig.CompactSettings cfg) {
        if (attrId == null || cfg == null) return null;

        for (var headerDef : cfg.headers) {
            if (headerDef == null || headerDef.attributes == null) continue;

            for (var spec : headerDef.attributes) {
                if (spec == null || spec.id == null || spec.id.isBlank()) continue;

                ResourceLocation icon = tryIdentifier(spec.icon);
                if (icon == null) continue;

                if (spec.id.contains("*")) {
                    if (globToPattern(spec.id).matcher(attrId.toString()).matches()) {
                        return icon;
                    }
                } else {
                    ResourceLocation id = tryIdentifier(spec.id);
                    if (id != null && id.equals(attrId)) {
                        return icon;
                    }
                }
            }
        }
        return null;
    }

    private void handleStatHover(Font font, int innerX, int y, StatRow row, int mouseX, int mouseY) {
        String rawName = row.stat.name().getString();
        NameSplit split = splitLeadingIcon(rawName);

        final int iconLeft = innerX;
        final int rowY0 = y, rowY1 = y + ROW_H_TEXT;

        float fs = fontScale();
        int iconW = iconColW();

        if (row.inBonusesGroup) {
            String bonusText = split.cleanName + " (" + row.stat.bonusCount() + " bonus" +
                    (row.stat.bonusCount() == 1 ? "" : "es") + ")";

            int nameLeft = iconLeft + iconW + ICON_VALUE_GAP;

            int cleanNameW = (int) Math.ceil(font.width(split.cleanName) * fs);
            int spaceW = (int) Math.ceil(font.width(" ") * fs);
            int openParenW = (int) Math.ceil(font.width("(") * fs);

            int valuePartLeft = nameLeft + cleanNameW + spaceW + openParenW;

            String bonusCountText = row.stat.bonusCount() + " bonus" + (row.stat.bonusCount() == 1 ? "" : "es") + ")";
            int valuePartW = (int) Math.ceil(font.width(bonusCountText) * fs);

            int iconX0 = iconLeft, iconX1 = iconLeft + iconW;
            int nameX0 = nameLeft, nameX1 = nameLeft + cleanNameW;
            int valueX0 = valuePartLeft, valueX1 = valuePartLeft + valuePartW;

            boolean overIcon = mouseX >= iconX0 && mouseX <= iconX1 && mouseY >= rowY0 && mouseY <= rowY1;
            boolean overName = mouseX >= nameX0 && mouseX <= nameX1 && mouseY >= rowY0 && mouseY <= rowY1;
            boolean overValue = mouseX >= valueX0 && mouseX <= valueX1 && mouseY >= rowY0 && mouseY <= rowY1;

            if (overIcon || overName) {
                showDescriptionTooltip(row.stat, mouseX, mouseY);
            } else if (overValue) {
                showCalculationTooltip(row.stat, mouseX, mouseY);
            }
        } else {
            String valueStr = fmtValue(row.stat, row.inBonusesGroup);

            final int valueLeft = iconLeft + iconW + ICON_VALUE_GAP;

            int valueW = (int) Math.ceil(font.width(valueStr) * fs);
            int spaceW = (int) Math.ceil(font.width(" ") * fs);
            int nameW = (int) Math.ceil(font.width(split.cleanName) * fs);
            int nameLeft = valueLeft + valueW + spaceW;

            int iconX0 = iconLeft, iconX1 = iconLeft + iconW;
            int valueX0 = valueLeft, valueX1 = valueLeft + valueW;
            int nameX0 = nameLeft, nameX1 = nameLeft + nameW;

            boolean overIcon = mouseX >= iconX0 && mouseX <= iconX1 && mouseY >= rowY0 && mouseY <= rowY1;
            boolean overValue = mouseX >= valueX0 && mouseX <= valueX1 && mouseY >= rowY0 && mouseY <= rowY1;
            boolean overName = mouseX >= nameX0 && mouseX <= nameX1 && mouseY >= rowY0 && mouseY <= rowY1;

            if (overIcon || overName) {
                showDescriptionTooltip(row.stat, mouseX, mouseY);
            } else if (overValue) {
                showCalculationTooltip(row.stat, mouseX, mouseY);
            }
        }
    }

    private void showDescriptionTooltip(StatEntry stat, int mouseX, int mouseY) {
        var tip = AttributeDescriptionProvider.getTooltip(stat.attribute().value());
        root.enqueueTooltip(tip.lines(), tip.icons(), mouseX, mouseY);
    }

    private void showCalculationTooltip(StatEntry stat, int mouseX, int mouseY) {
        var client = root.mc();
        var player = client.player;
        if (player == null) return;

        boolean shiftDown = GLFW.glfwGetKey(
                client.getWindow().getWindow(),
                client.options.keyShift.getDefaultKey().getValue()
        ) == GLFW.GLFW_PRESS;

        AttributeInstance instance = player.getAttribute(stat.attribute());
        if (instance == null) return;

        List<Component> lines = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();
        List<ResourceLocation> texIcons = new ArrayList<>();

        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(stat.attribute().value());
        String idStr = attrId == null ?  "" : attrId.toString();
        boolean isBase100Percent = AttributesPanelConfig.INSTANCE.percentAttributesBase100.contains(idStr);

        double rawBase = instance.getBaseValue();
        double rawFinal = instance.getValue();

        boolean isNaN = Double.isNaN(rawBase) || Double.isNaN(rawFinal);

        java.util.function.Function<Double, String> fmtPct = (d) -> {
            if (Math.abs(d - Math.round(d)) < 0.01) {
                return String.format("%d%%", (int) Math.round(d));
            }
            return String.format("%.2f%%", d);
        };

        if (!isNaN) {
            if (isBase100Percent) {
                if (Math.abs(rawBase) < 0.001) {
                    lines.add(Component.translatable("attributepanel.tooltip.base", fmtPct.apply(0.0)));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                    lines.add(Component.translatable("attributepanel.tooltip.final", fmtPct.apply(rawFinal * 100)));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                } else {
                    double percentChange = ((rawFinal - rawBase) / rawBase) * 100;
                    lines.add(Component.translatable("attributepanel.tooltip.base", String.format("%s (%.2f)", fmtPct.apply(0.0), rawBase)));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                    lines.add(Component.translatable("attributepanel.tooltip.final", String.format("%s (%.2f)", fmtPct.apply(percentChange), rawFinal)));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                }
            } else {
                lines.add(Component.translatable("attributepanel.tooltip.base", String.format("%.2f", rawBase)));
                icons.add(ItemStack.EMPTY);
                texIcons.add(null);
                lines.add(Component.translatable("attributepanel.tooltip.final", String.format("%.2f", rawFinal)));
                icons.add(ItemStack.EMPTY);
                texIcons.add(null);
            }
        }

        double flat = 0.0, multBase = 0.0, multTotal = 0.0;
        double puffFlat = 0.0, puffBase = 0.0, puffTotal = 0.0;
        boolean hasPuffish = false;
        List<Double> flatParts = new ArrayList<>();
        List<Double> baseMultParts = new ArrayList<>();
        List<Double> totalMultParts = new ArrayList<>();

        var unmatchedAccessorySources = new ArrayList<Object>();
        if (TrinketsCompat.isLoaded()) {
            unmatchedAccessorySources.addAll(TrinketsCompat.getTrinketModifierSources(player));
        }
        if (CuriosCompat.isLoaded()) {
            unmatchedAccessorySources.addAll(CuriosCompat.getCurioModifierSources(player));
        }

        boolean hasAnyModifiers = !instance.getModifiers().isEmpty();

        if (!instance.getModifiers().isEmpty()) {
            if (!lines.isEmpty()) {
                lines.add(Component.empty());
                icons.add(ItemStack.EMPTY);
                texIcons.add(null);
            }
            lines.add(Component.translatable("attributepanel.message.modifiers").withStyle(ChatFormatting.YELLOW));
            icons.add(ItemStack.EMPTY);
            texIcons.add(null);

            for (AttributeModifier mod : instance.getModifiers()) {
                var rawId = mod.id();

                if (rawId.getNamespace().equals("tiered")) {
                    String fullPath = rawId.getPath();
                    String[] parts = fullPath.split("/");
                    if (parts.length >= 3 && parts[parts.length - 1].contains("_")) {
                        rawId = ResourceLocation.fromNamespaceAndPath("tiered", parts[parts.length - 1]);
                    }
                }

                if (rawId.getNamespace().equals("puffish_skills")) {
                    hasPuffish = true;
                    switch (mod.operation()) {
                        case ADD_VALUE -> puffFlat += mod.amount();
                        case ADD_MULTIPLIED_BASE -> puffBase += mod.amount();
                        case ADD_MULTIPLIED_TOTAL -> puffTotal += mod.amount();
                    }
                    continue;
                }

                String opText;
                ChatFormatting color;
                switch (mod.operation()) {
                    case ADD_VALUE -> {
                        double v = mod.amount();
                        flat += v;
                        flatParts.add(v);
                        color = v >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                        opText = (v >= 0 ? "+" : "") + String.format("%.2f", v);
                    }
                    case ADD_MULTIPLIED_BASE -> {
                        double v = mod.amount();
                        multBase += v;
                        baseMultParts.add(v);
                        int p = (int) Math.round(v * 100);
                        color = p >= 0 ?  ChatFormatting.GREEN : ChatFormatting.RED;
                        opText = (p >= 0 ? "+" : "") + p + "% Base";
                    }
                    case ADD_MULTIPLIED_TOTAL -> {
                        double v = mod.amount();
                        multTotal += v;
                        totalMultParts.add(v);
                        int p = (int) Math.round(v * 100);
                        color = p >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                        opText = (p >= 0 ? "+" : "") + p + "% Total";
                    }
                    default -> {
                        color = ChatFormatting.GRAY;
                        opText = "?   ";
                    }
                }

                String fullPath = rawId.getPath();
                String[] idParts = fullPath.split("\\.", 2);
                ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), idParts[0]);
                String customName = (idParts.length > 1) ? idParts[1] :  null;
                boolean usedCustomName = false;

                Component displayName = Component.literal(formatModifierId(modId));
                ItemStack iconStack = ItemStack.EMPTY;
                ResourceLocation texIcon = null;
                boolean foundSource = false;

                if (rawId.getNamespace().equals("texture")) {
                    String[] texParts = fullPath.split("\\.");
                    if (texParts.length >= 4) {
                        String textureNamespace = texParts[0];
                        StringBuilder texPathBuilder = new StringBuilder("textures");

                        for (int i = 1; i < texParts.length - 1; i++) {
                            texPathBuilder.append("/").append(texParts[i]);
                        }
                        texPathBuilder.append(".png");

                        String customNameFromPath = texParts[texParts.length - 1];
                        texIcon = ResourceLocation.fromNamespaceAndPath(textureNamespace, texPathBuilder.toString());

                        String pretty = Arrays.stream(customNameFromPath.split("_"))
                                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(" "));
                        displayName = Component.literal(pretty).withStyle(ChatFormatting.LIGHT_PURPLE);
                        usedCustomName = true;
                        foundSource = true;
                    }
                }

                Component enchDisplayName = null;
                ItemStack enchIconStack = ItemStack.EMPTY;
                boolean enchantParsed = false;

                String path = rawId.getPath();
                String lowerPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
                if (lowerPath.startsWith("enchantment.") || lowerPath.startsWith("enchantment/") || lowerPath.contains("enchantment")) {
                    String possible = path;
                    int idxDot = path.indexOf('.');
                    int idxSlash = path.indexOf('/');
                    if (idxDot >= 0 && path.startsWith("enchantment.")) {
                        possible = path.substring("enchantment.".length());
                    } else if (idxSlash >= 0 && path.startsWith("enchantment/")) {
                        possible = path.substring("enchantment/".length());
                    } else {
                        if (path.startsWith("enchantment")) {
                            int sep = Math.max(path.indexOf('.'), path.indexOf('/'));
                            if (sep >= 0 && sep + 1 < path.length()) possible = path.substring(sep + 1);
                        }
                    }

                    int stop = possible.length();
                    int s1 = possible.indexOf('/');
                    int s2 = possible.indexOf('.');
                    if (s1 >= 0) stop = Math.min(stop, s1);
                    if (s2 >= 0) stop = Math.min(stop, s2);
                    String enchKey = (stop > 0 && stop <= possible.length()) ? possible.substring(0, stop) : possible;

                    if (enchKey != null && !enchKey.isBlank()) {
                        try {
                            ResourceLocation enchId = ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), enchKey);

                            if (root.mc().player != null && root.mc().player.level() != null) {
                                var drm = root.mc().player.level().registryAccess();
                                Registry<Enchantment> enchantmentRegistry = drm.registryOrThrow(Registries.ENCHANTMENT);

                                ResourceKey<Enchantment> enchantKey = ResourceKey.create(Registries.ENCHANTMENT, enchId);
                                var entryOpt = enchantmentRegistry.getHolder(enchantKey);

                                if (entryOpt.isPresent()) {
                                    var enchEntry = entryOpt.get();
                                    int lvl = Math.max(1, (int) Math.round(Math.abs(mod.amount())));
                                    enchIconStack = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchEntry, lvl));

                                    enchDisplayName = Enchantment.getFullname(enchEntry, lvl).copy().withStyle(ChatFormatting.AQUA);
                                    enchantParsed = true;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (foundSource) {
                    lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                    icons.add(iconStack);
                    texIcons.add(texIcon);
                    continue;
                }

                SEARCH_EQUIPPED:
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = player.getItemBySlot(slot);
                    if (stack.isEmpty()) continue;

                    final boolean[] matched = {false};

                    var comp = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                    if (comp != null) {
                        for (var entry : comp.modifiers()) {
                            if (entry.slot().test(slot) &&
                                    entry.modifier().id().equals(mod.id()) &&
                                    entry.attribute().equals(stat.attribute())) {
                                matched[0] = true;
                                break;
                            }
                        }
                    }

                    if (!matched[0]) {
                        var defaultMods = stack.getItem().getDefaultAttributeModifiers();
                        for (var entry : defaultMods.modifiers()) {
                            if (entry.slot().test(slot) &&
                                    entry.modifier().id().equals(mod.id()) &&
                                    entry.attribute().equals(stat.attribute())) {
                                matched[0] = true;
                                break;
                            }
                        }
                    }

                    if (matched[0]) {
                        iconStack = stack;
                        try {
                            if (!usedCustomName) displayName = stack.getHoverName().copy();
                        } catch (Exception ignored) {
                        }
                        foundSource = true;
                        break SEARCH_EQUIPPED;
                    }
                }

                if (!foundSource && enchantParsed) {
                    Component useName = enchDisplayName != null ? enchDisplayName : displayName;
                    ItemStack useIcon = !enchIconStack.isEmpty() ? enchIconStack : iconStack;

                    lines.add(useName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                    icons.add(useIcon);
                    texIcons.add(null);
                    foundSource = true;
                    continue;
                }

                if (!foundSource && !"dungeon_difficulty".equals(rawId.getNamespace()) && !"morequesttypes".equals(rawId.getNamespace())) {
                    String[] pathParts = rawId.getPath().split("\\.", 2)[0].split("/");
                    if (pathParts.length > 0) {
                        ResourceLocation guess = ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), pathParts[pathParts.length - 1]);
                        if (BuiltInRegistries.ITEM.containsKey(guess)) {
                            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(guess);
                            iconStack = new ItemStack(item);
                            displayName = iconStack.getHoverName().copy().withStyle(iconStack.getRarity().color());
                            foundSource = true;
                        }
                    }
                }
                if (!foundSource) {
                    try {
                        String rawPath = rawId.getPath().toLowerCase(Locale.ROOT);
                        if (rawPath.contains("set_bonus")) {
                            List<net.spell_engine.api.item.set.EquipmentSet.SourcedItemStack> sourced = new ArrayList<>();

                            for (EquipmentSlot slot :  EquipmentSlot.values()) {
                                ItemStack s = player.getItemBySlot(slot);
                                if (s != null && !s.isEmpty()) {
                                    sourced.add(new net.spell_engine.api.item.set.EquipmentSet.SourcedItemStack(s, slot.getName()));
                                }
                            }

                            if (TrinketsCompat.isLoaded()) {
                                try {
                                    for (var src : TrinketsCompat.getTrinketModifierSources(player)) {
                                        sourced.add(new net.spell_engine.api.item.set.EquipmentSet.SourcedItemStack(src.stack(), "trinket"));
                                    }
                                } catch (Exception ignored) {}
                            }

                            if (CuriosCompat.isLoaded()) {
                                try {
                                    for (var src : CuriosCompat.getCurioModifierSources(player)) {
                                        sourced.add(new net.spell_engine.api.item.set.EquipmentSet.SourcedItemStack(src.stack(), "curio"));
                                    }
                                } catch (Exception ignored) {}
                            }

                            var results = net.spell_engine.api.item.set.EquipmentSet.collectFrom(sourced, player.level());

                            for (var res : results) {
                                var setEntry = res.set();
                                if (setEntry.unwrapKey().isPresent()) {
                                    ResourceLocation setId = setEntry.unwrapKey().get().location();
                                    if (setId.getNamespace().equals(rawId.getNamespace())) {
                                        List<ItemStack> setItems = res.items();
                                        if (setItems != null && !setItems.isEmpty()) {
                                            int idx = (int) ((System.currentTimeMillis() / 1000L) % setItems.size());
                                            ItemStack chosen = setItems.get(idx);
                                            iconStack = chosen;

                                            Component setNameText;
                                            try {
                                                String tkey = net.spell_engine.api.item.set.EquipmentSet.translationKey(setEntry);
                                                setNameText = Component.translatable(tkey);
                                                if (setNameText.getString().equals(tkey)) {
                                                    String rawDefName = setEntry.value().name();
                                                    if (rawDefName != null && !rawDefName.isBlank()) {
                                                        setNameText = Component.literal(rawDefName);
                                                    } else {
                                                        setNameText = Component.literal(setId.getPath());
                                                    }
                                                }
                                            } catch (Exception e) {
                                                String rawDefName = "";
                                                try {
                                                    rawDefName = setEntry.value().name();
                                                } catch (Exception ignored) {}
                                                if (rawDefName != null && !rawDefName.isBlank()) {
                                                    setNameText = Component.literal(rawDefName);
                                                } else {
                                                    setNameText = Component.literal(setId.getPath());
                                                }
                                            }

                                            displayName = setNameText.copy().withStyle(ChatFormatting.AQUA)
                                                    .append(Component.literal(" Set").withStyle(ChatFormatting.AQUA));

                                            foundSource = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (customName != null && !customName.isBlank() && !"dungeon_difficulty".equals(rawId.getNamespace()) && !"morequesttypes".equals(rawId.getNamespace())) {
                    Set<String> ignoredArmorNames = Set.of("helmet", "chestplate", "leggings", "boots");
                    if (!ignoredArmorNames.contains(customName.toLowerCase(Locale.ROOT))) {
                        String pretty = Arrays.stream(customName.split("_"))
                                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(" "));
                        displayName = Component.literal(pretty).withStyle(ChatFormatting.LIGHT_PURPLE);
                        usedCustomName = true;
                    }
                }

                if (!foundSource) {
                    for (var it = unmatchedAccessorySources.iterator(); it.hasNext(); ) {
                        var source = it.next();

                        ItemStack sourceStack = null;
                        AttributeModifier sourceMod = null;
                        Holder<Attribute> sourceAttr = null;

                        if (source instanceof TrinketsCompat.TrinketModifierSource trinket) {
                            sourceStack = trinket.stack();
                            sourceMod = trinket.modifier();
                            sourceAttr = trinket.attribute();
                        } else if (source instanceof CuriosCompat.CurioModifierSource curio) {
                            sourceStack = curio.stack();
                            sourceMod = curio.modifier();
                            sourceAttr = curio.attribute();
                        }

                        if (sourceAttr != null && sourceAttr.value().equals(stat.attribute().value())
                                && sourceMod != null && sourceMod.operation() == mod.operation()
                                && Math.abs(sourceMod.amount() - mod.amount()) < 0.0001) {
                            iconStack = sourceStack;
                            try {
                                displayName = iconStack.getHoverName().copy().withStyle(iconStack.getRarity().color());
                            } catch (Exception e) {
                                displayName = Component.literal("Unknown Accessory").withStyle(ChatFormatting.GRAY);
                            }
                            foundSource = true;
                            it.remove();
                            break;
                        }
                    }
                }

                if (!foundSource) {
                    for (var se : player.getActiveEffects()) {
                        var effect = se.getEffect().value();
                        int amp = se.getAmplifier();
                        Map<Attribute, AttributeModifier> map = new HashMap<>();
                        effect.createModifiers(amp, (attribute, modifier) -> map.put(attribute.value(), modifier));
                        if (map.containsKey(stat.attribute().value())) {
                            AttributeModifier pm = map.get(stat.attribute().value());
                            if (pm.operation() == mod.operation() && Math.abs(pm.amount() - mod.amount()) < 0.0001) {
                                displayName = Component.translatable(effect.getDescriptionId());
                                iconStack = root.createColoredPotionItem(effect);
                                foundSource = true;
                                break;
                            }
                        }
                    }
                }

                if ("dungeon_difficulty".equals(rawId.getNamespace())) {
                    displayName = Component.literal("Power Boost").withStyle(ChatFormatting.AQUA);
                    lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(DD_POWER_ICON);
                    continue;
                }

                if ("morequesttypes".equals(rawId.getNamespace())) {
                    displayName = Component.literal("Quest Reward").withStyle(ChatFormatting.AQUA);
                    lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(FTB_QUEST_BOOK_ICON);
                    continue;
                }
                if ("reskillable".equals(rawId.getNamespace())) {
                    String[] skillNames = {"mining", "gathering", "attack", "defense", "building", "farming", "agility", "magic"};
                    String skillName = null;

                    for (String skill : skillNames) {
                        if (path.equals(skill)) {
                            skillName = skill;
                            break;
                        }
                    }

                    if (skillName != null) {
                        String prettySkill = skillName.substring(0, 1).toUpperCase(Locale.ROOT) + skillName.substring(1).toLowerCase(Locale.ROOT);
                        displayName = Component.literal(prettySkill + " Skill").withStyle(ChatFormatting.AQUA);

                        try {
                            ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel",
                                    "textures/reskillable_compat/" + skillName + ".png");
                            lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(iconLocation);
                            continue;
                        } catch (Exception ignored) {
                        }
                    }

                    if (path.equals("health_bonus")) {
                        displayName = Component.literal("Total Level Bonus").withStyle(ChatFormatting.AQUA);
                        lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                        icons.add(ItemStack.EMPTY);
                        texIcons.add(ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/reskillable_compat/health_bonus.png"));
                        continue;
                    }
                }
                if ("apotheosis".equals(rawId.getNamespace())) {
                    String apothPath = rawId.getPath();

                    String prettyName = null;

                    if (apothPath.contains("_modifier_apothic_attributes")) {
                        String beforeModifier = apothPath.split("_modifier_apothic_attributes")[0];

                        int lastSlash = beforeModifier.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            beforeModifier = beforeModifier.substring(lastSlash + 1);
                        }

                        prettyName = Arrays.stream(beforeModifier.split("_"))
                                .filter(s -> !s.isBlank())
                                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(" "));
                    }

                    if (prettyName != null) {
                        displayName = Component.literal(prettyName).withStyle(ChatFormatting.LIGHT_PURPLE);
                        lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                        icons.add(ItemStack.EMPTY);

                        try {
                            texIcon = ResourceLocation.fromNamespaceAndPath("apotheosis", "textures/items/sigils/socketing.png");
                            texIcons.add(texIcon);
                        } catch (Exception e) {
                            texIcons.add(null);
                        }

                        continue;
                    }
                }
                boolean printedCustom = false;

                if (rawId.getNamespace().equals("rpg-systems")) {
                    String p = rawId.getPath();
                    if (p.startsWith("title/")) {
                        String[] seg = p.split("/");

                        boolean perma = seg.length > 1 && "perma".equals(seg[1]);
                        String titlePathSlug;

                        if (perma) {
                            titlePathSlug = (seg.length >= 4) ? seg[3] : "unknown";
                        } else {
                            titlePathSlug = (seg.length >= 3) ? seg[2] : "unknown";
                        }

                        String prettyTitle = Arrays.stream(titlePathSlug.split("_"))
                                .filter(s -> !s.isBlank())
                                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(" "));

                        MutableComponent line = Component.literal(prettyTitle).withStyle(ChatFormatting.GOLD)
                                .append(Component.literal(" ").append(Component.literal(opText).withStyle(color)));

                        lines.add(line);
                        icons.add(ItemStack.EMPTY);
                        texIcons.add(perma ?  PERMA :  EQUIPPED);

                        printedCustom = true;
                    }
                }

                if (printedCustom) {
                    continue;
                } else if (rawId.getNamespace().equals("tiered")) {
                    String[] pp = rawId.getPath().split("_");
                    String tier = pp.length > 0 ? (pp[0].substring(0, 1).toUpperCase() + pp[0].substring(1).toLowerCase()) : "Tiered";
                    lines.add(Component.literal(tier + " Bonus:    ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(opText).withStyle(ChatFormatting.GREEN)));
                    icons.add(new ItemStack(Items.ANVIL));
                    texIcons.add(null);
                } else {
                    lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                    icons.add(iconStack);
                    texIcons.add(texIcon);
                }
            }

            if (hasPuffish) {
                lines.add(Component.translatable("attributepanel.tooltip.skill_tree_bonus").withStyle(ChatFormatting.AQUA));
                icons.add(ItemStack.EMPTY);
                texIcons.add(null);
                if (puffFlat != 0.0) {
                    lines.add(Component.literal(String.format("- %+,.2f", puffFlat)).withStyle(ChatFormatting.GREEN));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                }
                if (puffBase != 0.0) {
                    lines.add(Component.literal(String.format("- %+d%% Base", (int) (puffBase * 100))).withStyle(ChatFormatting.GREEN));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                }
                if (puffTotal != 0.0) {
                    lines.add(Component.literal(String.format("- %+d%% Total", (int) (puffTotal * 100))).withStyle(ChatFormatting.GREEN));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                }
            }
        }

        if (!hasAnyModifiers) {
            lines.add(Component.literal("None").withStyle(ChatFormatting.GRAY));
            icons.add(ItemStack.EMPTY);
            texIcons.add(null);
        }

        if (!isNaN) {
            boolean hasChanges = Math.abs(rawFinal - rawBase) > 0.001;

            if (hasChanges) {
                lines.add(Component.empty());
                icons.add(ItemStack.EMPTY);
                texIcons.add(null);
                if (shiftDown) {
                    lines.add(Component.translatable("attributepanel.tooltip.calculated").withStyle(ChatFormatting.DARK_GRAY));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);

                    if (isBase100Percent) {
                        if (Math.abs(rawBase) < 0.001) {
                            if (!flatParts.isEmpty()) {
                                String sum = flatParts.stream()
                                        .map(v -> fmtPct.apply(v * 100))
                                        .reduce((a, b) -> a + " + " + b)
                                        .orElse(fmtPct.apply(0.0));
                                lines.add(Component.literal(String.format("⟶ %s + (%s) = %s",
                                        fmtPct.apply(0.0), sum, fmtPct.apply(rawFinal * 100))).withStyle(ChatFormatting.GRAY));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                            }
                            lines.add(Component.literal("= " + fmtPct.apply(rawFinal * 100)).withStyle(ChatFormatting.GREEN));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                        } else {
                            double basePlusAdd = rawBase + flat;
                            double afterBaseMult = (multBase != 0.0) ? basePlusAdd * (1.0 + multBase) : basePlusAdd;
                            double finalValue = (multTotal != 0.0) ? afterBaseMult * (1.0 + multTotal) : afterBaseMult;

                            lines.add(Component.literal(String.format("Base: %.2f (%s)", rawBase, fmtPct.apply(0.0))).withStyle(ChatFormatting.GRAY));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);

                            if (!flatParts.isEmpty()) {
                                String sum = flatParts.stream().map(v -> String.format("%.2f", v)).reduce((a, b) -> a + " + " + b).orElse("0.00");
                                double pct = ((basePlusAdd - rawBase) / rawBase) * 100;
                                lines.add(Component.literal(String.format("⟶ %.2f + (%s) = %.2f (%s)",
                                        rawBase, sum, basePlusAdd, fmtPct.apply(pct))).withStyle(ChatFormatting.GRAY));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                            }

                            if (!baseMultParts.isEmpty()) {
                                String sum = baseMultParts.stream().map(v -> String.format("%.2f", v)).reduce((a, b) -> a + " + " + b).orElse("0.00");
                                double pct = ((afterBaseMult - rawBase) / rawBase) * 100;
                                lines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f (%s)",
                                        basePlusAdd, sum, afterBaseMult, fmtPct.apply(pct))).withStyle(ChatFormatting.GRAY));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                            }

                            if (!totalMultParts.isEmpty()) {
                                String sum = totalMultParts.stream().map(v -> String.format("%.2f", v)).reduce((a, b) -> a + " + " + b).orElse("0.00");
                                double pct = ((finalValue - rawBase) / rawBase) * 100;
                                lines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f (%s)",
                                        afterBaseMult, sum, finalValue, fmtPct.apply(pct))).withStyle(ChatFormatting.GRAY));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                            }

                            double finalPct = ((rawFinal - rawBase) / rawBase) * 100;
                            lines.add(Component.literal(String.format("= %.2f (%s)", rawFinal, fmtPct.apply(finalPct))).withStyle(ChatFormatting.GREEN));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                        }
                    } else {
                        double base = rawBase;
                        double basePlusAdd = base + flat;
                        double afterBaseMult = (multBase != 0.0) ? basePlusAdd * (1.0 + multBase) : basePlusAdd;
                        double finalValue = (multTotal != 0.0) ? afterBaseMult * (1.0 + multTotal) : afterBaseMult;

                        if (!flatParts.isEmpty()) {
                            String sum = flatParts.stream().map(v -> String.format("%.2f", v)).reduce((a, b) -> a + " + " + b).orElse("0.00");
                            lines.add(Component.literal(String.format("⟶ %.2f + (%s) = %.2f", base, sum, basePlusAdd)).withStyle(ChatFormatting.GRAY));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                        } else {
                            lines.add(Component.literal(String.format("= %.2f", base)).withStyle(ChatFormatting.GRAY));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                        }

                        if (!baseMultParts.isEmpty()) {
                            String sum = baseMultParts.stream().map(v -> String.format("%.2f", v)).reduce((a, b) -> a + " + " + b).orElse("0.00");
                            lines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", basePlusAdd, sum, afterBaseMult)).withStyle(ChatFormatting.GRAY));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                        }

                        if (!totalMultParts.isEmpty()) {
                            String sum = totalMultParts.stream().map(v -> String.format("%.2f", v)).reduce((a, b) -> a + " + " + b).orElse("0.00");
                            lines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterBaseMult, sum, finalValue)).withStyle(ChatFormatting.GRAY));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                        }

                        lines.add(Component.literal("= " + String.format("%.2f", finalValue)).withStyle(ChatFormatting.GREEN));
                        icons.add(ItemStack.EMPTY);
                        texIcons.add(null);

                        double actualFinal = rawFinal;
                        double delta = actualFinal - finalValue;
                        if (Math.abs(delta) > 0.001 && finalValue > 0.001) {
                            double pct = Math.abs(delta) / finalValue;
                            lines.add(Component.empty());
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                            if (delta > 0) {
                                lines.add(Component.translatable("attributepanel.tooltip.indirect_bonus", String.format("%.2f", delta)).withStyle(ChatFormatting.DARK_GREEN));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                                lines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %.2f) = %.2f", finalValue, pct, actualFinal)).withStyle(ChatFormatting.GRAY));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                            } else {
                                lines.add(Component.translatable("attributepanel.tooltip.indirect_decrease", String.format("%.2f", -delta)).withStyle(ChatFormatting.RED));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                                lines.add(Component.literal(String.format("⟶ %.2f × (1.00 - %.2f) = %.2f", finalValue, pct, actualFinal)).withStyle(ChatFormatting.GRAY));
                                icons.add(ItemStack.EMPTY);
                                texIcons.add(null);
                            }
                        }
                    }
                } else {
                    lines.add(Component.translatable("attributepanel.tooltip.hold_shift").withStyle(ChatFormatting.GRAY));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(null);
                }
            }
        }

        root.enqueueTooltipRich(lines, icons, texIcons, mouseX, mouseY);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int bgX = root.left() - 16;
        int bgY = root.top();
        int bgW = root.panelWidth() + 16;

        int iconsY = bgY + 9;
        int searchX = bgX + 7;
        if (mouseX >= searchX && mouseX <= searchX + SEARCH_ICON_SIZE &&
                mouseY >= iconsY && mouseY <= iconsY + SEARCH_ICON_SIZE) {
            searchVisible = !searchVisible;
            if (searchVisible) setFocused(true);
            else setFocused(false);
            return true;
        }

        if (searchVisible) {
            int barW = bgW;
            int barH = SEARCH_BAR_H;
            int barX = bgX;
            int barY = bgY - barH - SEARCH_BAR_MARGIN;

            if (mouseX >= barX && mouseX <= barX + barW &&
                    mouseY >= barY && mouseY <= barY + barH) {
                setFocused(true);
                return true;
            } else {
                setFocused(false);
            }
        }

        BarGeom g = computeBarGeom();
        if (!g.visible) return false;

        int left = (int) Math.floor(g.x);
        int right = (int) Math.ceil(g.x + g.w);

        if (mouseX >= left && mouseX <= right && mouseY >= g.y && mouseY <= g.y + g.h) {
            if (mouseY >= g.knobY && mouseY <= g.knobY + g.knobH) {
                draggingBar = true;
                dragGrabOffset = (int) (mouseY - g.knobY);
                return true;
            } else {
                int desired = (int) mouseY - g.y - g.knobH / 2;
                scrollPx = offsetFromBarPosition(desired, g);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!searchVisible || !searchFocused) return false;
        if (chr == '\n' || chr == '\r') return true;
        if (chr >= 32 && chr != 127) {
            replaceSelectionWith(String.valueOf(chr));            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!searchVisible || !searchFocused) return false;

        boolean ctrl = Screen.hasControlDown();
        boolean shift = Screen.hasShiftDown();

        if (keyCode == GLFW.GLFW_KEY_A && ctrl) {
            selectAll();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_V && ctrl) {
            String clip = root.mc().keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                replaceSelectionWith(clip);
                scrollPx = 0;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && ctrl) {
            if (hasSelection()) {
                replaceSelectionWith("");
            } else {
                int a = prevWord(caret);
                if (a != caret) {
                    searchText = searchText.substring(0, a) + searchText.substring(caret);
                    caret = a;
                    clearSelection();
                }
            }
            clampCaret();
            scrollPx = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE && ctrl) {
            if (hasSelection()) {
                replaceSelectionWith("");
            } else {
                int b = nextWord(caret);
                if (b != caret) {
                    searchText = searchText.substring(0, caret) + searchText.substring(b);
                    clearSelection();
                }
            }
            clampCaret();
            scrollPx = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                replaceSelectionWith("");
            } else if (caret > 0 && !searchText.isEmpty()) {
                searchText = searchText.substring(0, caret - 1) + searchText.substring(caret);
                caret--;
                clearSelection();
            }
            clampCaret();
            scrollPx = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) {
                replaceSelectionWith("");
            } else if (caret < searchText.length()) {
                searchText = searchText.substring(0, caret) + searchText.substring(caret + 1);
            }
            clampCaret();
            clearSelection();
            scrollPx = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int newPos = ctrl ? prevWord(caret) : Math.max(0, caret - 1);
            moveCaretTo(newPos, shift);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int newPos = ctrl ? nextWord(caret) : Math.min(searchText.length(), caret + 1);
            moveCaretTo(newPos, shift);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_HOME) {
            moveCaretTo(0, shift);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_END) {
            moveCaretTo(searchText.length(), shift);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!searchText.isEmpty()) {
                searchText = "";
                caret = 0;
                clearSelection();
                scrollPx = 0;
            } else {
                searchVisible = false;
                setFocused(false);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return true;
        }

        return false;
    }

    private int caret = 0;
    private int textScrollPx = 0;

    private int selStart = -1;
    private int selEnd = -1;

    private boolean hasSelection() {
        return selStart >= 0 && selEnd >= 0 && selStart != selEnd;
    }

    private void clearSelection() {
        selStart = selEnd = -1;
    }

    private void selectAll() {
        selStart = 0;
        selEnd = searchText.length();
        caret = searchText.length();
    }

    private int selA() {
        return Math.min(selStart, selEnd);
    }

    private int selB() {
        return Math.max(selStart, selEnd);
    }

    private void clampCaret() {
        caret = Math.max(0, Math.min(caret, searchText.length()));
    }

    private void moveCaretTo(int pos, boolean extend) {
        caret = Math.max(0, Math.min(pos, searchText.length()));
        if (extend) {
            if (!hasSelection()) {
                selStart = caret;
                selEnd = caret;
            }
            selEnd = caret;
        } else {
            clearSelection();
        }
    }

    private void replaceSelectionWith(String insert) {
        if (hasSelection()) {
            int a = selA(), b = selB();
            searchText = searchText.substring(0, a) + insert + searchText.substring(b);
            caret = a + insert.length();
            clearSelection();
        } else {
            searchText = searchText.substring(0, caret) + insert + searchText.substring(caret);
            caret += insert.length();
        }
        scrollPx = 0;
    }

    private int prevWord(int from) {
        int i = Math.max(0, Math.min(from, searchText.length()));
        while (i > 0 && searchText.charAt(i - 1) == ' ') i--;
        while (i > 0 && searchText.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int nextWord(int from) {
        int n = searchText.length();
        int i = Math.max(0, Math.min(from, n));
        while (i < n && searchText.charAt(i) != ' ') i++;
        while (i < n && searchText.charAt(i) == ' ') i++;
        return i;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingBar) {
            draggingBar = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!draggingBar || button != 0) return false;
        BarGeom g = computeBarGeom();
        if (!g.visible) return false;

        int desired = (int) (mouseY - g.y - dragGrabOffset);
        scrollPx = offsetFromBarPosition(desired, g);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int bgX = root.left() - 16;
        int bgY = root.top();
        int bgW = root.panelWidth() + 16;
        int bgH = root.panelHeight();

        int iconsY = bgY + 9;
        int infoX = bgX + bgW - INFO_ICON_SIZE - 7;

        if (hoveringInfoIcon && mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE &&
                mouseY >= iconsY && mouseY <= iconsY + INFO_ICON_SIZE) {
            int deltaPx = (verticalAmount > 0) ? -GLOBAL_TOOLTIP_SCROLL_STEP : GLOBAL_TOOLTIP_SCROLL_STEP;
            globalTooltipScrollOffset += deltaPx;
            if (globalTooltipScrollOffset < 0) globalTooltipScrollOffset = 0;
            return true;
        }

        int innerH = Math.max(0, bgH - (PADDING_TOP + PADDING_BOTTOM));

        BarGeom g = computeBarGeom();

        int left = (int) Math.floor(g.x);
        int right = (int) Math.ceil(g.x + g.w);

        boolean insidePanel =
                mouseX >= bgX && mouseX <= bgX + bgW &&
                        mouseY >= bgY && mouseY <= bgY + bgH;

        boolean insideBar = g.visible &&
                mouseX >= left && mouseX <= right &&
                mouseY >= g.y && mouseY <= g.y + g.h;

        if (!insidePanel && !insideBar) return false;

        int contentH = totalContentHeight();
        int maxOffset = Math.max(0, contentH - innerH);
        int deltaPx = (verticalAmount > 0) ? -SCROLL_STEP_PX : SCROLL_STEP_PX;

        int next = Math.max(0, Math.min(scrollPx + deltaPx, maxOffset));
        boolean changed = next != scrollPx;
        scrollPx = next;
        return changed;
    }

    private void buildLayout() {
        items.clear();
        prefixHeights.clear();

        List<StatEntry> stats = root.getCachedStats();
        if (stats.isEmpty()) return;

        if (AttributesPanelConfig.INSTANCE.compact == null) {
            AttributesPanelConfig.INSTANCE.compact = AttributesPanelConfig.CompactSettings.defaultPreset();
        }

        var cfg = AttributesPanelConfig.INSTANCE.compact;

        if (isSearching()) {
            String q = searchText.trim().toLowerCase(Locale.ROOT);

            AttributesPanelConfig.CompactSettings finalCfg = cfg;
            stats = stats.stream()
                    .filter(se -> {
                        String name = se.name().getString().toLowerCase(Locale.ROOT);
                        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(se.attribute().value());
                        String idStr = (id == null) ? "" : id.toString().toLowerCase(Locale.ROOT);
                        return name.contains(q) || idStr.contains(q);
                    })
                    .sorted(
                            Comparator
                                    .comparingInt((StatEntry se) -> orderScore(
                                            BuiltInRegistries.ATTRIBUTE.getKey(se.attribute().value()), finalCfg))
                                    .thenComparing(se -> se.name().getString())
                    )
                    .collect(Collectors.toList());

            for (var se : stats) {
                ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(se.attribute().value());
                ResourceLocation icon = resolveAttrIcon(attrId, cfg);

                if (se.isNaN()) {
                    boolean hasChanges = hasModifiers(se);
                    if (icon == null && hasChanges) {
                        icon = CHANGED;
                    }
                    items.add(icon != null ? Item.statBonus(se, icon) : Item.statBonusBullet(se));
                } else {
                    if (icon == null && hasModifiers(se)) {
                        icon = CHANGED;
                    }
                    items.add(icon != null ? Item.stat(se, icon) : Item.statBullet(se));
                }
            }

            int acc = 0;
            for (Item it : items) {
                prefixHeights.add(acc);
                acc += it.height;
            }
            return;
        }

        if (stats.isEmpty()) return;

        Map<ResourceLocation, StatEntry> byId = new HashMap<>();
        for (StatEntry s : stats) {
            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(s.attribute().value());
            if (id != null) byId.put(id, s);
        }

        cfg = AttributesPanelConfig.INSTANCE.compact;
        if (cfg == null) {
            cfg = AttributesPanelConfig.CompactSettings.defaultPreset();
        }
        List<Pattern> globalBlacklist = compilePatterns(cfg.globalBlacklist);
        Set<ResourceLocation> assigned = new HashSet<>();

        boolean firstSection = true;
        for (var headerDef : cfg.headers) {
            HeaderLayout headerLayout = resolveHeader(headerDef, byId, globalBlacklist, assigned);

            if (headerLayout.stats.isEmpty()) {
                continue;
            }

            if (!firstSection) items.add(Item.divider());
            firstSection = false;

            items.add(Item.header(headerLayout));
            for (var se : headerLayout.stats) {
                ResourceLocation icon = headerLayout.iconFor(se);
                if (icon == null && hasModifiers(se)) {
                    icon = CHANGED;
                }
                items.add(Item.stat(se, icon));
            }
        }

        List<StatEntry> remaining = byId.entrySet().stream()
                .filter(e -> !assigned.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .filter(se -> !matchesAny(globalBlacklist, BuiltInRegistries.ATTRIBUTE.getKey(se.attribute().value())))
                .sorted(Comparator.comparing(se -> se.name().getString()))
                .collect(Collectors.toList());

        List<StatEntry> bonuses = remaining.stream()
                .filter(StatEntry::isNaN)
                .collect(Collectors.toList());

        List<StatEntry> normal = remaining.stream()
                .filter(se -> !se.isNaN())
                .collect(Collectors.toList());

        if (!bonuses.isEmpty() && !cfg.disableBonusesHeader) {
            if (!firstSection) items.add(Item.divider());
            firstSection = false;

            HeaderLayout bonusHeader = new HeaderLayout();
            bonusHeader.headerText = (cfg.bonusesHeaderName == null || cfg.bonusesHeaderName.isBlank())
                    ? "Bonuses"
                    : cfg.bonusesHeaderName;
            bonusHeader.headerIcon = tryIdentifier(cfg.bonusesHeaderIcon);
            bonusHeader.perAttrIcon = Collections.emptyMap();
            bonusHeader.stats = Collections.emptyList();

            items.add(Item.header(bonusHeader));
            for (var se : bonuses) {
                ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(se.attribute().value());
                ResourceLocation icon = resolveAttrIcon(attrId, cfg);
                boolean hasChanges = hasModifiers(se);

                if (icon == null && hasChanges) {
                    icon = CHANGED;
                }

                items.add(icon != null ? Item.statBonus(se, icon) : Item.statBonusBullet(se));
            }
        }

        if (!normal.isEmpty() && !cfg.disableOtherHeader) {
            if (!firstSection) items.add(Item.divider());

            HeaderLayout other = new HeaderLayout();
            other.headerText = (cfg.otherHeaderName == null || cfg.otherHeaderName.isBlank()) ? "Other" : cfg.otherHeaderName;
            other.headerIcon = tryIdentifier(cfg.otherHeaderIcon);
            other.perAttrIcon = Collections.emptyMap();
            other.stats = normal;

            items.add(Item.header(other));
            for (var se : normal) {
                ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(se.attribute().value());
                ResourceLocation icon = resolveAttrIcon(id, cfg);

                if (icon == null && hasModifiers(se)) {
                    icon = CHANGED;
                }

                if (icon != null) {
                    items.add(Item.stat(se, icon));
                } else {
                    items.add(Item.statBullet(se));
                }
            }
        }

        int acc = 0;
        for (Item it : items) {
            prefixHeights.add(acc);
            acc += it.height;
        }
    }

    private boolean hasModifiers(StatEntry stat) {
        var client = root.mc();
        var player = client.player;
        if (player == null) return false;

        AttributeInstance instance = player.getAttribute(stat.attribute());
        if (instance == null) return false;

        return !instance.getModifiers().isEmpty();
    }

    public boolean wantsKeys() {
        return searchVisible && searchFocused;
    }

    public boolean handleCharTyped(char c, int mods) {
        return this.charTyped(c, mods);
    }

    public boolean handleKeyPressed(int key, int sc, int mods) {
        return this.keyPressed(key, sc, mods);
    }

    private static class HeaderLayout {
        String headerText;
        ResourceLocation headerIcon;
        List<StatEntry> stats = new ArrayList<>();
        Map<ResourceLocation, ResourceLocation> perAttrIcon = new HashMap<>();

        ResourceLocation iconFor(StatEntry s) {
            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(s.attribute().value());
            return id != null ? perAttrIcon.get(id) : null;
        }
    }

    private HeaderLayout resolveHeader(AttributesPanelConfig.HeaderDef def,
                                       Map<ResourceLocation, StatEntry> byId,
                                       List<Pattern> globalBlacklist,
                                       Set<ResourceLocation> assigned) {
        HeaderLayout out = new HeaderLayout();

        parseHeaderLabel(def.header, out);

        List<Pattern> localBlacklist = compilePatterns(def.blacklist);
        Set<ResourceLocation> addedHere = new HashSet<>();

        if (def.attributes != null) {
            for (var spec : def.attributes) {
                if (spec == null || spec.id == null || spec.id.isBlank()) continue;

                ResourceLocation iconId = tryIdentifier(spec.icon);

                if (spec.id.contains("*")) {
                    Pattern pat = globToPattern(spec.id);

                    List<Map.Entry<ResourceLocation, StatEntry>> matches = byId.entrySet().stream()
                            .filter(e -> !assigned.contains(e.getKey()))
                            .filter(e -> !addedHere.contains(e.getKey()))
                            .filter(e -> !matchesAny(globalBlacklist, e.getKey()))
                            .filter(e -> !matchesAny(localBlacklist, e.getKey()))
                            .filter(e -> pat.matcher(e.getKey().toString()).matches())
                            .sorted(Comparator.comparing(e -> e.getValue().name().getString()))
                            .collect(Collectors.toList());

                    for (var e : matches) {
                        out.stats.add(e.getValue());
                        addedHere.add(e.getKey());
                        assigned.add(e.getKey());
                        if (iconId != null) out.perAttrIcon.put(e.getKey(), iconId);
                    }
                } else {
                    ResourceLocation id = tryIdentifier(spec.id);
                    if (id != null && byId.containsKey(id)) {
                        if (!assigned.contains(id)
                                && !matchesAny(globalBlacklist, id)
                                && !matchesAny(localBlacklist, id)) {
                            out.stats.add(byId.get(id));
                            addedHere.add(id);
                            assigned.add(id);
                            if (iconId != null) out.perAttrIcon.put(id, iconId);
                        }
                    }
                }
            }
        }

        return out;
    }

    private int orderScore(ResourceLocation id, AttributesPanelConfig.CompactSettings cfg) {
        if (id == null || cfg == null || cfg.headers == null) return Integer.MAX_VALUE;

        int hIdx = 0;
        for (var def : cfg.headers) {
            if (def == null || def.attributes == null) {
                hIdx++;
                continue;
            }
            int sIdx = 0;
            for (var spec : def.attributes) {
                if (spec == null || spec.id == null || spec.id.isBlank()) {
                    sIdx++;
                    continue;
                }

                if (spec.id.contains("*")) {
                    Pattern p = globToPattern(spec.id);
                    if (p.matcher(id.toString()).matches()) {
                        return hIdx * 10000 + sIdx * 100;
                    }
                } else {
                    ResourceLocation exact = tryIdentifier(spec.id);
                    if (exact != null && exact.equals(id)) {
                        return hIdx * 10000 + sIdx * 100;
                    }
                }
                sIdx++;
            }
            hIdx++;
        }
        return Integer.MAX_VALUE - 1;
    }

    private static boolean matchesAny(List<Pattern> patterns, ResourceLocation id) {
        if (id == null || patterns == null) return false;
        String s = id.toString();
        for (Pattern p : patterns) {
            if (p.matcher(s).matches()) return true;
        }
        return false;
    }

    private static List<Pattern> compilePatterns(List<String> globs) {
        List<Pattern> out = new ArrayList<>();
        if (globs == null) return out;
        for (String g : globs) {
            if (g == null || g.isBlank()) continue;
            out.add(globToPattern(g));
        }
        return out;
    }

    private static Pattern globToPattern(String glob) {
        String escaped = Pattern.quote(glob).replace("\\*", ".*");
        return Pattern.compile("^" + escaped + "$");
    }

    private static ResourceLocation tryIdentifier(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return ResourceLocation.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void parseHeaderLabel(String header, HeaderLayout out) {
        if (header == null) {
            out.headerText = null;
            out.headerIcon = null;
            return;
        }
        String h = header.trim();
        int lb = h.indexOf('[');
        int rb = h.indexOf(']');
        if (lb >= 0 && rb > lb) {
            String before = h.substring(0, lb).trim();
            String inside = h.substring(lb + 1, rb).trim();
            out.headerText = before.isEmpty() ? null : before;
            out.headerIcon = tryIdentifier(inside);
        } else {
            if (h.startsWith("[") && h.endsWith("]") && h.length() > 2) {
                String inside = h.substring(1, h.length() - 1).trim();
                out.headerText = null;
                out.headerIcon = tryIdentifier(inside);
            } else {
                out.headerText = h.isBlank() ? null : h;
                out.headerIcon = null;
            }
        }
    }

    private void drawDivider(GuiGraphics ctx, int innerX, int innerW, int y) {
        int center = innerX + innerW / 2;
        int dividerX = center - (DIVIDER_TEXTURE_W / 2);
        dividerX = Math.max(innerX, Math.min(dividerX, innerX + innerW - DIVIDER_TEXTURE_W));
        int dividerY = y + (DIVIDER_TOTAL_H - DIVIDER_TEXTURE_H) / 2;
        ctx.blit(
                DIVIDER_TEXTURE,
                dividerX, dividerY,
                0, 0,
                DIVIDER_TEXTURE_W, DIVIDER_TEXTURE_H,
                DIVIDER_TEXTURE_W, DIVIDER_TEXTURE_H
        );
    }

    private void drawHeader(GuiGraphics ctx, Font font, int innerX, int innerW, int y, HeaderLayout header) {
        Minecraft mc = Minecraft.getInstance();
        var rm = mc.getResourceManager();

        boolean haveText = header.headerText != null && !header.headerText.isBlank();

        boolean iconRequested = header.headerIcon != null;
        boolean iconAvailable = false;
        if (iconRequested) {
            iconAvailable = rm.getResource(header.headerIcon).isPresent();
        }

        final String missingMarker = "[cant find texture]";

        int textW = haveText ? (int) Math.ceil(font.width(header.headerText) * HEADER_FONT_SCALE) : 0;
        int iconW = iconAvailable ? (HEADER_ICON_SIZE + 3) : 0;

        int missingW = 0;
        int missingGap = 0;
        if (iconRequested && !iconAvailable) {
            if (haveText) {
                missingGap = 6;
                missingW = (int) Math.ceil(font.width(missingMarker) * HEADER_FONT_SCALE);
            } else {
                missingW = (int) Math.ceil(font.width(missingMarker) * HEADER_FONT_SCALE);
            }
        }

        int totalW;
        if (iconAvailable) {
            totalW = iconW + textW;
        } else if (iconRequested) {
            totalW = (haveText ? textW + missingGap + missingW : missingW);
        } else {
            totalW = textW;
        }

        int startX = innerX + Math.max(0, (innerW - totalW) / 2);
        int x = startX;

        if (iconAvailable) {
            ctx.blit(header.headerIcon, x, y, 0, 0, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE);
            x += HEADER_ICON_SIZE + 3;
        }

        if (haveText) {
            ctx.pose().pushPose();
            ctx.pose().scale(HEADER_FONT_SCALE, HEADER_FONT_SCALE, 1f);
            int drawX = (int) (x / HEADER_FONT_SCALE);
            int drawY = (int) (y / HEADER_FONT_SCALE);
            ctx.drawString(font, header.headerText, drawX, drawY, colorHeader(), true);
            ctx.pose().popPose();
            x += (int) Math.ceil(font.width(header.headerText) * HEADER_FONT_SCALE);
        }

        if (iconRequested && !iconAvailable) {
            if (haveText) x += missingGap;
            ctx.pose().pushPose();
            ctx.pose().scale(HEADER_FONT_SCALE, HEADER_FONT_SCALE, 1f);
            int drawX = (int) (x / HEADER_FONT_SCALE);
            int drawY = (int) (y / HEADER_FONT_SCALE);
            ctx.drawString(font, missingMarker, drawX, drawY, colorMuted(), true);
            ctx.pose().popPose();
        }
    }

    private void drawStat(GuiGraphics ctx, Font font, int innerX, int y, StatRow statRow) {
        String rawName = statRow.stat.name().getString();
        String translationKey = Component.translatable(statRow.stat.attribute().value().getDescriptionId()).getString();

        IconLeadingCompat.IconSplit split = IconLeadingCompat.extractIconWithFallback(rawName, translationKey);

        final int iconLeft = innerX;
        float fs = fontScale();

        if (statRow.inBonusesGroup) {
            int glyphW = (int) Math.ceil(font.width(split.leadingIcon) * fs);
            int drawXpx = iconLeft + Math.max(0, (iconColW() - glyphW) / 2);

            if (split.hasIcon()) {
                ctx.pose().pushPose();
                ctx.pose().scale(fs, fs, 1f);
                int drawX = (int) (drawXpx / fs);
                int drawY = (int) (y / fs);
                ctx.drawString(font, split.leadingIcon, drawX, drawY, 0xFFFFFF, true);
                ctx.pose().popPose();
            } else if (statRow.icon != null) {
                float s = fs * (ATTR_ICON_SIZE / (float) TEX_ICON_SRC_PX);
                int iconDrawW = (int) Math.round(ATTR_ICON_SIZE * fs);
                int drawXpx2 = iconLeft + Math.max(0, (iconColW() - iconDrawW) / 2);

                ctx.pose().pushPose();
                ctx.pose().scale(s, s, 1f);
                int drawX = (int) Math.floor(drawXpx2 / s);
                int drawY = (int) Math.floor((y - 1) / s);
                ctx.blit(statRow.icon, drawX, drawY, 0, 0, TEX_ICON_SRC_PX, TEX_ICON_SRC_PX,
                        TEX_ICON_SRC_PX, TEX_ICON_SRC_PX);
                ctx.pose().popPose();
            } else if (statRow.bullet) {
                int bx = iconLeft + Math.max(0, (iconColW() - BULLET_SIZE) / 2);
                int by = y + Math.max(0, (ROW_H_TEXT - BULLET_SIZE) / 2) + BULLET_Y_NUDGE;
                ctx.fill(bx, by, bx + BULLET_SIZE, by + BULLET_SIZE, 0xFF000000 | colorBullet());
            }

            int nameLeft = iconLeft + iconColW() + ICON_VALUE_GAP;
            String bonusText = split.cleanName + " (" + statRow.stat.bonusCount() + " bonus" +
                    (statRow.stat.bonusCount() == 1 ? "" : "es") + ")";

            ctx.pose().pushPose();
            ctx.pose().scale(fs, fs, 1f);
            int nDrawX = (int) (nameLeft / fs);
            int drawY = (int) (y / fs);
            ctx.drawString(font, bonusText, nDrawX, drawY, colorBody(), true);
            ctx.pose().popPose();
        } else {
            final int valueLeft = iconLeft + iconColW() + ICON_VALUE_GAP;

            if (split.hasIcon()) {
                int glyphW = (int) Math.ceil(font.width(split.leadingIcon) * fs);
                int drawXpx = iconLeft + Math.max(0, (iconColW() - glyphW) / 2);

                ctx.pose().pushPose();
                ctx.pose().scale(fs, fs, 1f);
                int drawX = (int) (drawXpx / fs);
                int drawY = (int) (y / fs);
                ctx.drawString(font, split.leadingIcon, drawX, drawY, 0xFFFFFF, true);
                ctx.pose().popPose();

            } else if (statRow.icon != null) {
                float s = fs * (ATTR_ICON_SIZE / (float) TEX_ICON_SRC_PX);
                int iconDrawW = (int) Math.round(ATTR_ICON_SIZE * fs);
                int drawXpx = iconLeft + Math.max(0, (iconColW() - iconDrawW) / 2);

                ctx.pose().pushPose();
                ctx.pose().scale(s, s, 1f);
                int drawX = (int) Math.floor(drawXpx / s);
                int drawY = (int) Math.floor((y - 1) / s);
                ctx.blit(statRow.icon, drawX, drawY, 0, 0, TEX_ICON_SRC_PX, TEX_ICON_SRC_PX,
                        TEX_ICON_SRC_PX, TEX_ICON_SRC_PX);
                ctx.pose().popPose();
            } else if (statRow.bullet) {
                int bx = iconLeft + Math.max(0, (iconColW() - BULLET_SIZE) / 2);
                int by = y + Math.max(0, (ROW_H_TEXT - BULLET_SIZE) / 2) + BULLET_Y_NUDGE;
                ctx.fill(bx, by, bx + BULLET_SIZE, by + BULLET_SIZE, 0xFF000000 | colorBullet());
            }

            String valueStr = fmtValue(statRow.stat, statRow.inBonusesGroup);

            int valueW = (int) Math.ceil(font.width(valueStr) * fs);
            int spaceW = (int) Math.ceil(font.width(" ") * fs);
            int nameLeft = valueLeft + valueW + spaceW;

            ctx.pose().pushPose();
            ctx.pose().scale(fs, fs, 1f);

            int vDrawX = (int) (valueLeft / fs);
            int drawY = (int) (y / fs);
            ctx.drawString(font, valueStr, vDrawX, drawY, colorBody(), true);

            int nDrawX = (int) (nameLeft / fs);
            ctx.drawString(font, split.cleanName, nDrawX, drawY, colorBody(), true);

            ctx.pose().popPose();
        }
    }

    private static final class NameSplit {
        final String cleanName;
        final String leadingIcon;

        NameSplit(String cleanName, String leadingIcon) {
            this.cleanName = cleanName;
            this.leadingIcon = leadingIcon;
        }
    }

    private static boolean isIconGlyph(int cp) {
        return isBMPPUA(cp) || isCJKCompat(cp) || isPlanePUA(cp);
    }

    private static boolean isBMPPUA(int cp) {
        return cp >= 0xE000 && cp <= 0xF8FF;
    }

    private static boolean isCJKCompat(int cp) {
        return cp >= 0xF900 && cp <= 0xFAFF;
    }

    private static boolean isPlanePUA(int cp) {
        return (cp >= 0xF0000 && cp <= 0xFFFFD) || (cp >= 0x100000 && cp <= 0x10FFFD);
    }

    private static NameSplit splitLeadingIcon(String s) {
        if (s == null) return new NameSplit("", null);

        String noCodes = stripSectionCodes(s);

        StringBuilder clean = new StringBuilder(noCodes.length());
        String leadingIcon = null;
        boolean tookIcon = false;

        for (int i = 0; i < noCodes.length(); ) {
            int cp = noCodes.codePointAt(i);
            int len = Character.charCount(cp);

            if (isIconGlyph(cp)) {
                if (!tookIcon) leadingIcon = new String(Character.toChars(cp));
                tookIcon = true;
                i += len;
                while (i < noCodes.length()) {
                    int cp2 = noCodes.codePointAt(i);
                    if (Character.isWhitespace(cp2)) i += Character.charCount(cp2);
                    else break;
                }
                continue;
            }

            clean.appendCodePoint(cp);
            i += len;
        }

        return new NameSplit(clean.toString().stripLeading(), leadingIcon);
    }

    private static String stripSectionCodes(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            char ch = s.charAt(i);
            if (ch == '§') {
                if (i + 1 < s.length() && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
                    i += 2;
                    for (int k = 0; k < 6 && i + 1 < s.length(); k++) {
                        if (s.charAt(i) == '§') i += 2;
                        else break;
                    }
                } else {
                    i = Math.min(i + 2, s.length());
                }
                continue;
            }
            int cp = s.codePointAt(i);
            out.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    private int totalContentHeight() {
        if (items.isEmpty()) return 0;
        int last = prefixHeights.get(prefixHeights.size() - 1);
        return last + items.get(items.size() - 1).height;
    }

    private int prefixAt(int index) {
        if (index <= 0) return 0;
        if (index >= prefixHeights.size()) return totalContentHeight();
        return prefixHeights.get(index);
    }

    private int firstVisibleIndex(int offsetPx) {
        int lo = 0, hi = Math.max(0, prefixHeights.size() - 1);
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (prefixHeights.get(mid) <= offsetPx) lo = mid;
            else hi = mid - 1;
        }
        if (lo >= items.size()) lo = Math.max(0, items.size() - 1);
        if (!prefixHeights.isEmpty() && prefixHeights.get(lo) > offsetPx) return 0;
        return lo;
    }

    private void drawScrollbar(GuiGraphics ctx, int bgX, int bgY, int bgW, int innerY, int innerH, int contentH) {
        BarGeom g = computeBarGeom(bgX, bgY, bgW, innerY, innerH, contentH);
        if (!g.visible) return;

        int left = (int) Math.floor(g.x);
        int right = (int) Math.ceil(g.x + g.w);

        ctx.fill(left, g.y, right, g.y + g.h, 0x66000000);
        ctx.fill(left, g.knobY, right, g.knobY + g.knobH, 0xCCFFFFFF);
    }

    private BarGeom computeBarGeom() {
        int bgX = root.left() - 16;
        int bgY = root.top();
        int bgW = root.panelWidth() + 16;
        int bgH = root.panelHeight();

        int innerY = bgY + PADDING_TOP;
        int innerH = Math.max(0, bgH - (PADDING_TOP + PADDING_BOTTOM));
        int contentH = totalContentHeight();

        return computeBarGeom(bgX, bgY, bgW, innerY, innerH, contentH);
    }

    private BarGeom computeBarGeom(int bgX, int bgY, int bgW, int innerY, int innerH, int contentH) {
        BarGeom g = new BarGeom();
        g.x = (bgX + bgW + SCROLLBAR_GAP) - 6.3f;
        g.y = innerY - 8;
        g.w = SCROLLBAR_W;
        g.h = Math.max(1, innerH);

        if (contentH <= innerH || contentH <= 0) {
            g.visible = false;
            return g;
        }
        g.visible = true;

        g.knobH = Math.max(8, (int) (g.h * (innerH / (float) contentH)));
        int maxOffsetPx = contentH - innerH;
        int offsetPx = Math.max(0, Math.min(scrollPx, maxOffsetPx));
        float pos = (maxOffsetPx <= 0) ? 0f : (offsetPx / (float) maxOffsetPx);
        g.knobY = g.y + (int) ((g.h - g.knobH) * pos);
        g.maxOffsetPx = maxOffsetPx;
        return g;
    }

    private int offsetFromBarPosition(int desiredKnobTop, BarGeom g) {
        int track = Math.max(1, g.h - g.knobH);
        int clampedTop = Math.max(0, Math.min(desiredKnobTop, track));
        float ratio = clampedTop / (float) track;
        return (int) Math.round(ratio * g.maxOffsetPx);
    }

    private static final class BarGeom {
        float x, w;
        int y, h;
        int knobY, knobH;
        int maxOffsetPx;
        boolean visible;
    }

    private enum ItemType {
        DIVIDER, HEADER, STAT
    }

    private static class Item {
        final ItemType type;
        final int height;
        HeaderLayout header;
        StatRow stat;

        private Item(ItemType t, int height) {
            this.type = t;
            this.height = height;
        }

        static Item divider() {
            return new Item(ItemType.DIVIDER, DIVIDER_TOTAL_H);
        }

        static Item header(HeaderLayout h) {
            Item it = new Item(ItemType.HEADER, ROW_H_TEXT + HEADER_AFTER_GAP);
            it.header = h;
            return it;
        }

        static Item stat(StatEntry se, ResourceLocation icon) {
            Item it = new Item(ItemType.STAT, ROW_H_TEXT);
            it.stat = new StatRow(se, icon, icon == null, false);
            return it;
        }

        static Item statBullet(StatEntry se) {
            Item it = new Item(ItemType.STAT, ROW_H_TEXT);
            it.stat = new StatRow(se, null, true, false);
            return it;
        }

        static Item statBonus(StatEntry se, ResourceLocation icon) {
            Item it = new Item(ItemType.STAT, ROW_H_TEXT);
            it.stat = new StatRow(se, icon, icon == null, true);
            return it;
        }

        static Item statBonusBullet(StatEntry se) {
            Item it = new Item(ItemType.STAT, ROW_H_TEXT);
            it.stat = new StatRow(se, null, true, true);
            return it;
        }
    }

    private static class StatRow {
        final StatEntry stat;
        final ResourceLocation icon;
        final boolean bullet;
        final boolean inBonusesGroup;

        StatRow(StatEntry se, ResourceLocation icon) {
            this(se, icon, false, false);
        }

        StatRow(StatEntry se, ResourceLocation icon, boolean bullet, boolean inBonusesGroup) {
            this.stat = se;
            this.icon = icon;
            this.bullet = bullet;
            this.inBonusesGroup = inBonusesGroup;
        }
    }

    private void drawGlobalBonusTooltip(GuiGraphics ctx, int mouseX, int mouseY) {
        Player player = root.mc().player;
        if (player == null) return;

        if (cachedTooltipData == null) {
            Map<String, Double> flatMap = new TreeMap<>();
            Map<String, Double> baseMultMap = new TreeMap<>();
            Map<String, Double> totalMultMap = new TreeMap<>();

            for (Holder<Attribute> entry : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
                Attribute attr = entry.value();
                AttributeInstance instance = player.getAttribute(entry);
                if (instance == null || instance.getModifiers().isEmpty()) continue;

                String attrName = Component.translatable(attr.getDescriptionId()).getString();

                for (AttributeModifier mod : instance.getModifiers()) {
                    double value = mod.amount();
                    if (Math.abs(value) < 0.0001) continue;

                    switch (mod.operation()) {
                        case ADD_VALUE -> flatMap.merge(attrName, value, Double::sum);
                        case ADD_MULTIPLIED_BASE -> baseMultMap.merge(attrName, value, Double::sum);
                        case ADD_MULTIPLIED_TOTAL -> totalMultMap.merge(attrName, value, Double:: sum);
                    }
                }
            }

            List<Component> allLines = new ArrayList<>();
            List<ItemStack> allIcons = new ArrayList<>();

            allLines.add(Component.translatable("attributepanel.message.bonuses").withStyle(ChatFormatting.GOLD));
            allIcons.add(ItemStack.EMPTY);

            boolean addedAny = false;

            for (String attr : flatMap.keySet()) {
                double value = flatMap.get(attr);
                allLines.add(Component.literal(String.format("- %s: %+,.2f", attr, value)).withStyle(ChatFormatting.GREEN));
                allIcons.add(ItemStack.EMPTY);
                addedAny = true;
            }
            for (String attr : baseMultMap.keySet()) {
                double value = baseMultMap.get(attr);
                allLines.add(Component.literal(String.format("- %s: %+d%% Base", attr, (int) (value * 100))).withStyle(ChatFormatting.GREEN));
                allIcons.add(ItemStack.EMPTY);
                addedAny = true;
            }
            for (String attr : totalMultMap.keySet()) {
                double value = totalMultMap.get(attr);
                allLines.add(Component.literal(String.format("- %s: %+d%% Total", attr, (int) (value * 100))).withStyle(ChatFormatting.GREEN));
                allIcons.add(ItemStack.EMPTY);
                addedAny = true;
            }

            if (!addedAny) {
                allLines.add(Component.literal("No active modifiers").withStyle(ChatFormatting.GRAY));
                allIcons.add(ItemStack.EMPTY);
            }

            Font font = root.mc().font;
            int screenHeight = root.mc().getWindow().getGuiScaledHeight();
            int lineHeight = font.lineHeight + 2;

            int maxWidth = 0;
            for (Component line : allLines) {
                int w = font.width(line);
                if (w > maxWidth) maxWidth = w;
            }

            int tooltipWidth = maxWidth + 16;
            int maxVisibleLines = Math.min(20, (screenHeight - mouseY - 20) / lineHeight);
            int tooltipHeight = Math.min(allLines.size(), maxVisibleLines) * lineHeight;

            cachedTooltipData = new TooltipRenderData(allLines, allIcons, tooltipWidth, tooltipHeight, maxVisibleLines, allLines.size());
        }

        Font font = root.mc().font;
        int lineHeight = font.lineHeight + 2;

        if (cachedTooltipData.totalLines <= cachedTooltipData.maxVisibleLines) {
            globalTooltipScrollOffset = 0;
            root.enqueueTooltip(cachedTooltipData.allLines, cachedTooltipData.allIcons, mouseX, mouseY);
        } else {
            int maxScroll = Math.max(0, (cachedTooltipData.totalLines - cachedTooltipData.maxVisibleLines) * lineHeight);
            if (globalTooltipScrollOffset > maxScroll) globalTooltipScrollOffset = maxScroll;
            if (globalTooltipScrollOffset < 0) globalTooltipScrollOffset = 0;

            int startLine = globalTooltipScrollOffset / lineHeight;
            int endLine = Math.min(cachedTooltipData.totalLines, startLine + cachedTooltipData.maxVisibleLines);

            List<Component> visibleLines = new ArrayList<>();
            List<ItemStack> visibleIcons = new ArrayList<>(cachedTooltipData.allIcons.subList(startLine, endLine));

            int paddingNeeded = cachedTooltipData.tooltipWidth;
            for (int i = startLine; i < endLine; i++) {
                Component line = cachedTooltipData.allLines.get(i);
                int currentWidth = font.width(line);
                int neededPadding = paddingNeeded - currentWidth - 16;

                if (neededPadding > 0) {
                    int spaces = neededPadding / font.width(" ") + 1;
                    String padding = " ".repeat(spaces);
                    MutableComponent paddedLine = line.copy().append(Component.literal(padding));
                    visibleLines.add(paddedLine);
                } else {
                    visibleLines.add(line);
                }
            }

            root.enqueueTooltip(visibleLines, visibleIcons, mouseX, mouseY);

            int tooltipX = mouseX + 12;
            int tooltipY = mouseY - 12;

            int contentHeight = cachedTooltipData.totalLines * lineHeight;
            int visibleHeight = cachedTooltipData.tooltipHeight;

            if (contentHeight > visibleHeight) {
                float scrollbarH = visibleHeight;
                float knobH = Math.max(20, scrollbarH * (visibleHeight / (float) contentHeight));
                float scrollRatio = globalTooltipScrollOffset / (float) maxScroll;
                float knobY = tooltipY + (scrollbarH - knobH) * scrollRatio;

                int scrollbarX = tooltipX + cachedTooltipData.tooltipWidth + 2;

                ctx.fill(scrollbarX, tooltipY, scrollbarX + 3, tooltipY + (int) scrollbarH, 0x66000000);
                ctx.fill(scrollbarX, (int) knobY, scrollbarX + 3, (int) (knobY + knobH), 0xCCFFFFFF);
            }
        }
    }
    private boolean hasFocus = false;

    @Override
    public void setFocused(boolean focused) {
        this.hasFocus = focused;
        this.searchFocused = focused && searchVisible;
        if (this.searchFocused) {
            caret = searchText.length();
            clearSelection();
        }
    }

    @Override
    public boolean isFocused() {
        return hasFocus;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
    }
}