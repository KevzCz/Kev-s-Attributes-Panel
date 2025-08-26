package net.pixeldreamstudios.attributepanel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.attributepanel.compat.TrinketCompat;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import org.joml.RoundingMode;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable.formatModifierId;

@Environment(EnvType.CLIENT)
public class CompactAttributePanelDrawable implements Drawable, Element, Selectable {
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

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("kevs-attributes-panel", "textures/gui/gui.png");
    private static final Identifier DIVIDER_TEXTURE = Identifier.of("kevs-attributes-panel", "textures/gui/divider.png");

    private static final Identifier INFO_ICON = Identifier.of("kevs-attributes-panel", "textures/gui/info.png");
    private static final int INFO_ICON_SIZE = 12;
    private static final Identifier SEARCH_ICON = Identifier.of("kevs-attributes-panel", "textures/gui/search.png");
    private static final int SEARCH_ICON_SIZE = 12;
    private static final int VALUE_DECIMALS = 3;
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

    public CompactAttributePanelDrawable(AttributePanelDrawable root) {
        this.root = root;
    }

    private boolean isSearching() {
        return searchText != null && !searchText.isBlank();
    }

    private static String fmtValue(StatEntry stat) {
        double v = stat.percent() ? stat.current() * 100.0 : stat.current();
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
        if (cfg != null && cfg.sidePadding >= 0) return cfg.sidePadding;
        return DEFAULT_SIDE_PADDING;
    }

    private int iconColW() {
        return (int) Math.ceil(ATTR_ICON_SIZE * fontScale());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        TextRenderer tr = root.mc().textRenderer;

        int bgX = root.left() - 16;
        int bgY = root.top();
        int bgW = root.panelWidth() + 16;
        int bgH = root.panelHeight();

        ctx.drawTexture(BACKGROUND_TEXTURE, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);

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
        ctx.drawTexture(SEARCH_ICON, searchX, iconsY, 0, 0, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
        if (mouseX >= searchX && mouseX <= searchX + SEARCH_ICON_SIZE &&
                mouseY >= iconsY && mouseY <= iconsY + SEARCH_ICON_SIZE) {
            root.enqueueTooltip(List.of(Text.translatable("attributepanel.search.tooltip", "Search Attributes")),
                    List.of(ItemStack.EMPTY), mouseX, mouseY);
        }

        int infoX = bgX + bgW - INFO_ICON_SIZE - 7;
        ctx.drawTexture(INFO_ICON, infoX, iconsY, 0, 0, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE);
        if (mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE &&
                mouseY >= iconsY && mouseY <= iconsY + INFO_ICON_SIZE) {
            drawGlobalBonusTooltip(mouseX, mouseY);
        }

        if (searchVisible) {
            int barW = bgW;
            int barH = SEARCH_BAR_H;
            int barX = bgX;
            int barY = bgY - barH - SEARCH_BAR_MARGIN;

            ctx.fill(barX, barY, barX + barW, barY + barH, 0xCC000000);
            ctx.drawBorder(barX, barY, barW, barH, 0xFFAAAAAA);

            tr = root.mc().textRenderer;
            int textX = barX + 6;
            int textY = barY + (barH - tr.fontHeight) / 2;
            int visibleW = Math.max(1, barW - 12);

            int caretClamped = Math.max(0, Math.min(caret, searchText.length()));
            if (searchText.isEmpty()) textScrollPx = 0;

            int textW = tr.getWidth(searchText);
            int caretPx = tr.getWidth(searchText.substring(0, caretClamped));
            int maxScroll = Math.max(0, textW - visibleW);

            if (caretPx - textScrollPx > visibleW) textScrollPx = caretPx - visibleW;
            if (caretPx - textScrollPx < 0)         textScrollPx = caretPx;
            if (textScrollPx < 0)                   textScrollPx = 0;
            if (textScrollPx > maxScroll)           textScrollPx = maxScroll;

            String toShow = searchText.isBlank() ? "Search…" : searchText;
            int color = searchText.isBlank() ? 0xCCCCCC : 0xFFFFFF;

            ctx.enableScissor(barX + 1, barY + 1, barX + barW - 1, barY + barH - 1);

            if (!searchText.isBlank() && hasSelection()) {
                int a = Math.min(selA(), searchText.length());
                int b = Math.min(selB(), searchText.length());
                if (a != b) {
                    int beforeW = tr.getWidth(searchText.substring(0, a));
                    int selW    = tr.getWidth(searchText.substring(a, b));
                    int sx0 = textX + beforeW - textScrollPx;
                    int sx1 = sx0 + selW;
                    ctx.fill(sx0, barY + 3, sx1, barY + barH - 3, 0x66FFFFFF);
                }
            }

            int drawX = textX - textScrollPx;
            ctx.drawText(tr, toShow, drawX, textY, color, false);

            if (searchFocused) {
                int caretDrawX = textX + caretPx - textScrollPx;
                ctx.fill(caretDrawX, barY + 3, caretDrawX + 1, barY + barH - 3, 0xFFFFFFFF);
            }

            ctx.disableScissor();
        }

        if (items.isEmpty()) {
            drawEmptyState(ctx, tr, innerX, innerW, innerY);
            return;
        }

        ctx.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        int idx = firstVisibleIndex(scrollPx);
        int y = innerY - (scrollPx - prefixAt(idx)) + (isSearching() ? 5: 0);

        boolean tooltipQueued = false;

        while (idx < items.size()) {
            Item it = items.get(idx);
            if (y + it.height > innerY + innerH) break;

            switch (it.type) {
                case DIVIDER -> drawDivider(ctx, innerX, innerW, y);
                case HEADER  -> drawHeader(ctx, tr, innerX, innerW, y, it.header);
                case STAT -> {
                    drawStat(ctx, tr, innerX, y, it.stat);

                    if (mouseX >= innerX && mouseX <= innerX + innerW &&
                            mouseY >= y && mouseY <= y + ROW_H_TEXT) {
                        handleStatHover(tr, innerX, y, it.stat, mouseX, mouseY);
                    }
                }
            }

            y += it.height;
            idx++;
        }

        ctx.disableScissor();

        drawScrollbar(ctx, bgX, bgY, bgW, innerY, innerH, contentH);
    }

    private void drawEmptyState(DrawContext ctx, TextRenderer tr, int innerX, int innerW, int innerY) {
        String msg = isSearching()
                ? "No attributes match \"" + searchText + "\""
                : "No attributes";

        int wrapWidth = Math.max(1, innerW - 8);
        int x = innerX + 4;
        int y = innerY + 7;

        for (OrderedText line : tr.wrapLines(Text.literal(msg), wrapWidth)) {
            ctx.drawTextWithShadow(tr, line, x, y, 0xAAAAAA);
            y += tr.fontHeight + 2;
        }
    }

    private Identifier resolveAttrIcon(Identifier attrId, AttributesPanelConfig.CompactSettings cfg) {
        if (attrId == null || cfg == null) return null;

        for (var headerDef : cfg.headers) {
            if (headerDef == null || headerDef.attributes == null) continue;

            for (var spec : headerDef.attributes) {
                if (spec == null || spec.id == null || spec.id.isBlank()) continue;

                Identifier icon = tryIdentifier(spec.icon);
                if (icon == null) continue;

                if (spec.id.contains("*")) {
                    if (globToPattern(spec.id).matcher(attrId.toString()).matches()) {
                        return icon;
                    }
                } else {
                    Identifier id = tryIdentifier(spec.id);
                    if (id != null && id.equals(attrId)) {
                        return icon;
                    }
                }
            }
        }
        return null;
    }

    private void handleStatHover(TextRenderer tr, int innerX, int y, StatRow row, int mouseX, int mouseY) {
        String rawName = row.stat.name().getString();
        NameSplit split = splitLeadingIcon(rawName);

        String valueStr = fmtValue(row.stat);

        final int iconLeft  = innerX;
        final int valueLeft = iconLeft + iconColW() + ICON_VALUE_GAP;

        float fs = fontScale();
        int valueW = (int)Math.ceil(tr.getWidth(valueStr) * fs);
        int spaceW = (int)Math.ceil(tr.getWidth(" ") * fs);
        int nameW  = (int)Math.ceil(tr.getWidth(split.cleanName) * fs);
        int nameLeft = valueLeft + valueW + spaceW;

        int rowY0 = y, rowY1 = y + ROW_H_TEXT;

        int iconX0 = iconLeft,           iconX1 = iconLeft + iconColW();
        int valueX0 = valueLeft,         valueX1 = valueLeft + valueW;
        int nameX0  = nameLeft,          nameX1  = nameLeft  + nameW;

        boolean overIcon  = mouseX >= iconX0  && mouseX <= iconX1  && mouseY >= rowY0 && mouseY <= rowY1;
        boolean overValue = mouseX >= valueX0 && mouseX <= valueX1 && mouseY >= rowY0 && mouseY <= rowY1;
        boolean overName  = mouseX >= nameX0  && mouseX <= nameX1  && mouseY >= rowY0 && mouseY <= rowY1;

        if (overIcon || overName) {
            showDescriptionTooltip(row.stat, mouseX, mouseY);
        } else if (overValue) {
            showCalculationTooltip(row.stat, mouseX, mouseY);
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

        boolean shiftDown = InputUtil.isKeyPressed(
                client.getWindow().getHandle(),
                client.options.sneakKey.getDefaultKey().getCode()
        );

        EntityAttributeInstance instance = player.getAttributeInstance(stat.attribute());

        List<Text> lines = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();

        lines.add(Text.translatable("attributepanel.tooltip.base", String.format("%.2f", stat.base())));
        icons.add(ItemStack.EMPTY);
        if (instance != null) {
            lines.add(Text.translatable("attributepanel.tooltip.final", String.format("%.2f", instance.getValue())));
            icons.add(ItemStack.EMPTY);
        }

        double flat = 0.0, multBase = 0.0, multTotal = 0.0;
        double puffFlat = 0.0, puffBase = 0.0, puffTotal = 0.0;
        boolean hasPuffish = false;
        List<Double> flatParts = new ArrayList<>();
        List<Double> baseMultParts = new ArrayList<>();
        List<Double> totalMultParts = new ArrayList<>();

        var unmatchedTrinketSources = new ArrayList<TrinketCompat.TrinketModifierSource>();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            unmatchedTrinketSources.addAll(TrinketCompat.getTrinketModifierSources(player));
        }

        if (instance != null && !instance.getModifiers().isEmpty()) {
            lines.add(Text.empty()); icons.add(ItemStack.EMPTY);
            lines.add(Text.translatable("attributepanel.message.modifiers").formatted(Formatting.YELLOW));
            icons.add(ItemStack.EMPTY);

            for (EntityAttributeModifier mod : instance.getModifiers()) {
                var rawId = mod.id();

                if (rawId.getNamespace().equals("tiered")) {
                    String fullPath = rawId.getPath();
                    String[] parts = fullPath.split("/");
                    if (parts.length >= 3 && parts[parts.length - 1].contains("_")) {
                        rawId = Identifier.of("tiered", parts[parts.length - 1]);
                    }
                }

                if (rawId.getNamespace().equals("puffish_skills")) {
                    hasPuffish = true;
                    switch (mod.operation()) {
                        case ADD_VALUE -> puffFlat += mod.value();
                        case ADD_MULTIPLIED_BASE -> puffBase += mod.value();
                        case ADD_MULTIPLIED_TOTAL -> puffTotal += mod.value();
                    }
                    continue;
                }

                String opText;
                Formatting color;
                switch (mod.operation()) {
                    case ADD_VALUE -> {
                        double v = mod.value();
                        flat += v; flatParts.add(v);
                        color = v >= 0 ? Formatting.GREEN : Formatting.RED;
                        opText = (v >= 0 ? "+" : "") + String.format("%.2f", v);
                    }
                    case ADD_MULTIPLIED_BASE -> {
                        double v = mod.value();
                        multBase += v; baseMultParts.add(v);
                        int p = (int) Math.round(v * 100);
                        color = p >= 0 ? Formatting.GREEN : Formatting.RED;
                        opText = (p >= 0 ? "+" : "") + p + "% Base";
                    }
                    case ADD_MULTIPLIED_TOTAL -> {
                        double v = mod.value();
                        multTotal += v; totalMultParts.add(v);
                        int p = (int) Math.round(v * 100);
                        color = p >= 0 ? Formatting.GREEN : Formatting.RED;
                        opText = (p >= 0 ? "+" : "") + p + "% Total";
                    }
                    default -> {
                        color = Formatting.GRAY;
                        opText = "?";
                    }
                }

                String fullPath = rawId.getPath();
                String[] idParts = fullPath.split("\\.", 2);
                Identifier modId = Identifier.of(rawId.getNamespace(), idParts[0]);
                String customName = (idParts.length > 1) ? idParts[1] : null;
                boolean usedCustomName = false;

                Text displayName = Text.literal(formatModifierId(modId));
                ItemStack iconStack = ItemStack.EMPTY;
                boolean foundSource = false;

                SEARCH_EQUIPPED:
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = player.getEquippedStack(slot);
                    if (stack.isEmpty()) continue;
                    final boolean[] matched = {false};
                    var comp = stack.get(net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS);
                    if (comp != null) {
                        comp.applyModifiers(slot, (attr, entryMod) -> {
                            if (entryMod.id().equals(mod.id()) && attr.equals(stat.attribute())) matched[0] = true;
                        });
                    }
                    if (!matched[0]) {
                        stack.getItem().getAttributeModifiers().applyModifiers(slot, (attr, entryMod) -> {
                            if (entryMod.id().equals(mod.id()) && attr.equals(stat.attribute())) matched[0] = true;
                        });
                    }
                    if (matched[0]) {
                        iconStack = stack;
                        try { if (!usedCustomName) displayName = stack.getName().copy(); } catch (Exception ignored) {}
                        foundSource = true;
                        break SEARCH_EQUIPPED;
                    }
                }

                if (!foundSource) {
                    String[] pathParts = rawId.getPath().split("\\.",2)[0].split("/");
                    if (pathParts.length > 0) {
                        Identifier guess = Identifier.of(rawId.getNamespace(), pathParts[pathParts.length-1]);
                        if (Registries.ITEM.containsId(guess)) {
                            net.minecraft.item.Item item = Registries.ITEM.get(guess);
                            iconStack = new ItemStack(item);
                            displayName = iconStack.getName().copy().formatted(iconStack.getRarity().getFormatting());
                            foundSource = true;
                        }
                    }
                }
                if (customName != null && !customName.isBlank()) {
                    Set<String> ignoredArmorNames = Set.of("helmet", "chestplate", "leggings", "boots");
                    if (!ignoredArmorNames.contains(customName.toLowerCase(Locale.ROOT))) {
                        String pretty = Arrays.stream(customName.split("_"))
                                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(" "));
                        displayName = Text.literal(pretty).formatted(Formatting.LIGHT_PURPLE);
                        usedCustomName = true;
                    }
                }

                if (!foundSource && FabricLoader.getInstance().isModLoaded("trinkets")) {
                    for (var it = unmatchedTrinketSources.iterator(); it.hasNext();) {
                        var src = it.next();
                        if (src.id().value().equals(stat.attribute().value())
                                && src.modifier().operation()==mod.operation()
                                && Math.abs(src.modifier().value()-mod.value())<0.0001) {
                            iconStack = src.stack();
                            try { displayName = iconStack.getName().copy().formatted(iconStack.getRarity().getFormatting()); }
                            catch (Exception e) { displayName = Text.literal("Unknown Trinket").formatted(Formatting.GRAY); }
                            foundSource = true;
                            it.remove();
                            break;
                        }
                    }
                }

                if (!foundSource) {
                    for (var se : player.getStatusEffects()) {
                        StatusEffect effect = se.getEffectType().value();
                        int amp = se.getAmplifier();
                        Map<EntityAttribute, EntityAttributeModifier> map = new HashMap<>();
                        effect.forEachAttributeModifier(amp, (attribute, modifier) -> map.put(attribute.value(), modifier));
                        if (map.containsKey(stat.attribute().value())) {
                            EntityAttributeModifier pm = map.get(stat.attribute().value());
                            if (pm.operation()==mod.operation() && Math.abs(pm.value()-mod.value())<0.0001) {
                                displayName = Text.translatable(effect.getTranslationKey());
                                iconStack = root.createColoredPotionItem(effect);
                                foundSource = true; break;
                            }
                        }
                    }
                }

                if (rawId.getNamespace().equals("tiered")) {
                    String[] pp = rawId.getPath().split("_");
                    String tier = pp.length>0 ? (pp[0].substring(0,1).toUpperCase()+pp[0].substring(1).toLowerCase()) : "Tiered";
                    lines.add(Text.literal(tier+" Bonus: ").formatted(Formatting.AQUA)
                            .append(Text.literal(opText).formatted(Formatting.GREEN)));
                    icons.add(new ItemStack(Items.ANVIL));
                } else {
                    lines.add(displayName.copy().append(" ").append(Text.literal(opText).formatted(color)));
                    icons.add(iconStack);
                }
            }

            if (hasPuffish) {
                lines.add(Text.translatable("attributepanel.tooltip.skill_tree_bonus").formatted(Formatting.AQUA));
                icons.add(ItemStack.EMPTY);
                if (puffFlat != 0.0)  { lines.add(Text.literal(String.format("- %+,.2f", puffFlat)).formatted(Formatting.GREEN));  icons.add(ItemStack.EMPTY); }
                if (puffBase != 0.0)  { lines.add(Text.literal(String.format("- %+d%% Base",  (int)(puffBase*100))).formatted(Formatting.GREEN)); icons.add(ItemStack.EMPTY); }
                if (puffTotal != 0.0) { lines.add(Text.literal(String.format("- %+d%% Total", (int)(puffTotal*100))).formatted(Formatting.GREEN)); icons.add(ItemStack.EMPTY); }
                lines.add(Text.empty()); icons.add(ItemStack.EMPTY);
            }
        }

        if (stat.isChanged()) {
            lines.add(Text.empty()); icons.add(ItemStack.EMPTY);
            if (shiftDown) {
                double base = stat.base();
                double basePlusAdd = base + flat;
                double afterBaseMult = (multBase!=0.0) ? basePlusAdd * (1.0+multBase) : basePlusAdd;
                double finalValue = (multTotal!=0.0) ? afterBaseMult * (1.0+multTotal) : afterBaseMult;

                lines.add(Text.translatable("attributepanel.tooltip.calculated").formatted(Formatting.DARK_GRAY)); icons.add(ItemStack.EMPTY);

                if (!flatParts.isEmpty()) {
                    String sum = flatParts.stream().map(v->String.format("%.2f",v)).reduce((a,b)->a+" + "+b).orElse("0.00");
                    lines.add(Text.literal(String.format("⟶ %.2f + (%s) = %.2f", base, sum, basePlusAdd)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY);
                } else {
                    lines.add(Text.literal(String.format("= %.2f", base)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY);
                }

                if (!baseMultParts.isEmpty()) {
                    String sum = baseMultParts.stream().map(v->String.format("%.2f",v)).reduce((a,b)->a+" + "+b).orElse("0.00");
                    lines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", basePlusAdd, sum, afterBaseMult)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY);
                }
                if (!totalMultParts.isEmpty()) {
                    String sum = totalMultParts.stream().map(v->String.format("%.2f",v)).reduce((a,b)->a+" + "+b).orElse("0.00");
                    lines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterBaseMult, sum, finalValue)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY);
                }

                lines.add(Text.literal("= " + String.format("%.2f", finalValue)).formatted(Formatting.GREEN)); icons.add(ItemStack.EMPTY);

                if (instance != null) {
                    double actual = instance.getValue();
                    double delta = actual - finalValue;
                    if (Math.abs(delta) > 0.001 && finalValue > 0.001) {
                        double pct = Math.abs(delta)/finalValue;
                        lines.add(Text.empty()); icons.add(ItemStack.EMPTY);
                        if (delta > 0) {
                            lines.add(Text.translatable("attributepanel.tooltip.indirect_bonus", String.format("%.2f", delta)).formatted(Formatting.DARK_GREEN)); icons.add(ItemStack.EMPTY);
                            lines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %.2f) = %.2f", finalValue, pct, actual)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY);
                        } else {
                            lines.add(Text.translatable("attributepanel.tooltip.indirect_decrease", String.format("%.2f", -delta)).formatted(Formatting.RED)); icons.add(ItemStack.EMPTY);
                            lines.add(Text.literal(String.format("⟶ %.2f × (1.00 - %.2f) = %.2f", finalValue, pct, actual)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY);
                        }
                    }
                }
            } else {
                lines.add(Text.translatable("attributepanel.tooltip.hold_shift").formatted(Formatting.GRAY));
                icons.add(ItemStack.EMPTY);
            }
        }

        root.enqueueTooltip(lines, icons, mouseX, mouseY);
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
            if (searchVisible) setFocused(true); else setFocused(false);
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

        int left = (int)Math.floor(g.x);
        int right = (int)Math.ceil(g.x + g.w);

        if (mouseX >= left && mouseX <= right && mouseY >= g.y && mouseY <= g.y + g.h) {
            if (mouseY >= g.knobY && mouseY <= g.knobY + g.knobH) {
                draggingBar = true;
                dragGrabOffset = (int)(mouseY - g.knobY);
                return true;
            } else {
                int desired = (int)mouseY - g.y - g.knobH / 2;
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
            replaceSelectionWith(String.valueOf(chr));
            return true;
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
            String clip = root.mc().keyboard.getClipboard();
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
    private int selEnd   = -1;

    private boolean hasSelection() {
        return selStart >= 0 && selEnd >= 0 && selStart != selEnd;
    }
    private void clearSelection() { selStart = selEnd = -1; }
    private void selectAll() { selStart = 0; selEnd = searchText.length(); caret = searchText.length(); }
    private int selA() { return Math.min(selStart, selEnd); }
    private int selB() { return Math.max(selStart, selEnd); }

    private void clampCaret() {
        caret = Math.max(0, Math.min(caret, searchText.length()));
    }
    private void moveCaretTo(int pos, boolean extend) {
        caret = Math.max(0, Math.min(pos, searchText.length()));
        if (extend) {
            if (!hasSelection()) { selStart = caret; selEnd = caret; }
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
        while (i > 0 && searchText.charAt(i-1) == ' ') i--;
        while (i > 0 && searchText.charAt(i-1) != ' ') i--;
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

        var cfg = AttributesPanelConfig.INSTANCE.compact;
        if (cfg == null) {
            cfg = AttributesPanelConfig.CompactSettings.defaultPreset();
        }

        if (isSearching()) {
            String q = searchText.trim().toLowerCase(Locale.ROOT);

            AttributesPanelConfig.CompactSettings finalCfg = cfg;
            stats = stats.stream()
                    .filter(se -> {
                        String name = se.name().getString().toLowerCase(Locale.ROOT);
                        Identifier id = Registries.ATTRIBUTE.getId(se.attribute().value());
                        String idStr = (id == null) ? "" : id.toString().toLowerCase(Locale.ROOT);
                        return name.contains(q) || idStr.contains(q);
                    })
                    .sorted(
                            Comparator
                                    .comparingInt((StatEntry se) -> orderScore(
                                            Registries.ATTRIBUTE.getId(se.attribute().value()), finalCfg))
                                    .thenComparing(se -> se.name().getString())
                    )
                    .collect(Collectors.toList());

            for (var se : stats) {
                Identifier attrId = Registries.ATTRIBUTE.getId(se.attribute().value());
                Identifier icon = resolveAttrIcon(attrId, cfg);
                items.add(icon != null ? Item.stat(se, icon) : Item.statBullet(se));
            }

            int acc = 0;
            for (Item it : items) {
                prefixHeights.add(acc);
                acc += it.height;
            }
            return;
        }

        if (stats.isEmpty()) return;

        if (isSearching()) {
            for (var se : stats) {
                Identifier attrId = Registries.ATTRIBUTE.getId(se.attribute().value());
                Identifier icon = resolveAttrIcon(attrId, cfg);
                items.add(Item.stat(se, icon));
            }
            int acc = 0;
            for (Item it : items) {
                prefixHeights.add(acc);
                acc += it.height;
            }
            return;
        }

        Map<Identifier, StatEntry> byId = new HashMap<>();
        for (StatEntry s : stats) {
            Identifier id = Registries.ATTRIBUTE.getId(s.attribute().value());
            if (id != null) byId.put(id, s);
        }

        cfg = AttributesPanelConfig.INSTANCE.compact;
        if (cfg == null) {
            cfg = AttributesPanelConfig.CompactSettings.defaultPreset();
        }
        List<Pattern> globalBlacklist = compilePatterns(cfg.globalBlacklist);
        Set<Identifier> assigned = new HashSet<>();

        boolean firstSection = true;
        for (var headerDef : cfg.headers) {
            HeaderLayout headerLayout = resolveHeader(headerDef, byId, globalBlacklist, assigned);
            if (headerLayout.stats.isEmpty() && headerLayout.headerText == null && headerLayout.headerIcon == null) {
                continue;
            }
            if (!firstSection) items.add(Item.divider());
            firstSection = false;

            items.add(Item.header(headerLayout));
            for (var se : headerLayout.stats) items.add(Item.stat(se, headerLayout.iconFor(se)));
        }

        List<StatEntry> remaining = byId.entrySet().stream()
                .filter(e -> !assigned.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .filter(se -> !matchesAny(globalBlacklist, Registries.ATTRIBUTE.getId(se.attribute().value())))
                .sorted(Comparator.comparing(se -> se.name().getString()))
                .collect(Collectors.toList());

        if (!remaining.isEmpty() && !cfg.disableOtherHeader) {
            if (!firstSection) items.add(Item.divider());

            HeaderLayout other = new HeaderLayout();
            other.headerText = (cfg.otherHeaderName == null || cfg.otherHeaderName.isBlank()) ? "Other" : cfg.otherHeaderName;
            other.headerIcon = tryIdentifier(cfg.otherHeaderIcon);
            other.perAttrIcon = Collections.emptyMap();
            other.stats = remaining;

            items.add(Item.header(other));
            for (var se : remaining) {
                Identifier id = Registries.ATTRIBUTE.getId(se.attribute().value());
                Identifier icon = resolveAttrIcon(id, cfg);
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
        Identifier headerIcon;
        List<StatEntry> stats = new ArrayList<>();
        Map<Identifier, Identifier> perAttrIcon = new HashMap<>();

        Identifier iconFor(StatEntry s) {
            Identifier id = Registries.ATTRIBUTE.getId(s.attribute().value());
            return id != null ? perAttrIcon.get(id) : null;
        }
    }

    private HeaderLayout resolveHeader(AttributesPanelConfig.HeaderDef def,
                                       Map<Identifier, StatEntry> byId,
                                       List<Pattern> globalBlacklist,
                                       Set<Identifier> assigned) {
        HeaderLayout out = new HeaderLayout();

        parseHeaderLabel(def.header, out);

        List<Pattern> localBlacklist = compilePatterns(def.blacklist);
        Set<Identifier> addedHere = new HashSet<>();

        if (def.attributes != null) {
            for (var spec : def.attributes) {
                if (spec == null || spec.id == null || spec.id.isBlank()) continue;

                Identifier iconId = tryIdentifier(spec.icon);

                if (spec.id.contains("*")) {
                    Pattern pat = globToPattern(spec.id);

                    List<Map.Entry<Identifier, StatEntry>> matches = byId.entrySet().stream()
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
                    Identifier id = tryIdentifier(spec.id);
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

    private int orderScore(Identifier id, AttributesPanelConfig.CompactSettings cfg) {
        if (id == null || cfg == null || cfg.headers == null) return Integer.MAX_VALUE;

        int hIdx = 0;
        for (var def : cfg.headers) {
            if (def == null || def.attributes == null) { hIdx++; continue; }
            int sIdx = 0;
            for (var spec : def.attributes) {
                if (spec == null || spec.id == null || spec.id.isBlank()) { sIdx++; continue; }

                if (spec.id.contains("*")) {
                    Pattern p = globToPattern(spec.id);
                    if (p.matcher(id.toString()).matches()) {
                        return hIdx * 10000 + sIdx * 100;
                    }
                } else {
                    Identifier exact = tryIdentifier(spec.id);
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

    private record WildcardSpec(String pattern, Identifier icon) {}

    private static boolean matchesAny(List<Pattern> patterns, Identifier id) {
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

    private static Identifier tryIdentifier(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Identifier.of(s);
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

    private void drawDivider(DrawContext ctx, int innerX, int innerW, int y) {
        int center = innerX + innerW / 2;
        int dividerX = center - (DIVIDER_TEXTURE_W / 2);
        dividerX = Math.max(innerX, Math.min(dividerX, innerX + innerW - DIVIDER_TEXTURE_W));
        int dividerY = y + (DIVIDER_TOTAL_H - DIVIDER_TEXTURE_H) / 2;
        ctx.drawTexture(
                DIVIDER_TEXTURE,
                dividerX, dividerY,
                0, 0,
                DIVIDER_TEXTURE_W, DIVIDER_TEXTURE_H,
                DIVIDER_TEXTURE_W, DIVIDER_TEXTURE_H
        );
    }

    private void drawHeader(DrawContext ctx, TextRenderer tr, int innerX, int innerW, int y, HeaderLayout header) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var rm = mc.getResourceManager();

        boolean haveText = header.headerText != null && !header.headerText.isBlank();

        boolean iconRequested = header.headerIcon != null;
        boolean iconAvailable = false;
        if (iconRequested) {
            iconAvailable = rm.getResource(header.headerIcon).isPresent();
        }

        final String missingMarker = "[cant find texture]";

        int textW = haveText ? (int) Math.ceil(tr.getWidth(header.headerText) * HEADER_FONT_SCALE) : 0;
        int iconW = iconAvailable ? (HEADER_ICON_SIZE + 3) : 0;

        int missingW = 0;
        int missingGap = 0;
        if (iconRequested && !iconAvailable) {
            if (haveText) {
                missingGap = 6;
                missingW = (int) Math.ceil(tr.getWidth(missingMarker) * HEADER_FONT_SCALE);
            } else {
                missingW = (int) Math.ceil(tr.getWidth(missingMarker) * HEADER_FONT_SCALE);
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
            ctx.drawTexture(header.headerIcon, x, y, 0, 0, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE, HEADER_ICON_SIZE);
            x += HEADER_ICON_SIZE + 3;
        }

        if (haveText) {
            ctx.getMatrices().push();
            ctx.getMatrices().scale(HEADER_FONT_SCALE, HEADER_FONT_SCALE, 1f);
            int drawX = (int) (x / HEADER_FONT_SCALE);
            int drawY = (int) (y / HEADER_FONT_SCALE);
            ctx.drawTextWithShadow(tr, header.headerText, drawX, drawY, 0xFFFFFF);
            ctx.getMatrices().pop();
            x += (int) Math.ceil(tr.getWidth(header.headerText) * HEADER_FONT_SCALE);
        }

        if (iconRequested && !iconAvailable) {
            if (haveText) x += missingGap;
            ctx.getMatrices().push();
            ctx.getMatrices().scale(HEADER_FONT_SCALE, HEADER_FONT_SCALE, 1f);
            int drawX = (int) (x / HEADER_FONT_SCALE);
            int drawY = (int) (y / HEADER_FONT_SCALE);
            ctx.drawTextWithShadow(tr, missingMarker, drawX, drawY, 0xAAAAAA);
            ctx.getMatrices().pop();
        }
    }

    private void drawStat(DrawContext ctx, TextRenderer tr, int innerX, int y, StatRow statRow) {
        String rawName = statRow.stat.name().getString();
        NameSplit split = splitLeadingIcon(rawName);

        final int iconLeft  = innerX;
        final int valueLeft = iconLeft + iconColW() + ICON_VALUE_GAP;

        float fs = fontScale();

        if (split.leadingIcon != null) {
            int glyphW = (int)Math.ceil(tr.getWidth(split.leadingIcon) * fs);
            int drawXpx = iconLeft + Math.max(0, (iconColW() - glyphW) / 2);

            ctx.getMatrices().push();
            ctx.getMatrices().scale(fs, fs, 1f);
            int drawX = (int)(drawXpx / fs);
            int drawY = (int)(y / fs);
            ctx.drawTextWithShadow(tr, split.leadingIcon, drawX, drawY, 0xFFFFFF);
            ctx.getMatrices().pop();

        } else if (statRow.icon != null) {
            float s = fs * (ATTR_ICON_SIZE / (float)TEX_ICON_SRC_PX);
            int iconDrawW = (int)Math.round(ATTR_ICON_SIZE * fs);
            int drawXpx = iconLeft + Math.max(0, (iconColW() - iconDrawW) / 2);

            ctx.getMatrices().push();
            ctx.getMatrices().scale(s, s, 1f);
            int drawX = (int)Math.floor(drawXpx / s);
            int drawY = (int)Math.floor((y - 1) / s);
            ctx.drawTexture(statRow.icon, drawX, drawY, 0, 0, TEX_ICON_SRC_PX, TEX_ICON_SRC_PX,
                    TEX_ICON_SRC_PX, TEX_ICON_SRC_PX);
            ctx.getMatrices().pop();
        } else if (statRow.bullet) {
            int bx = iconLeft + Math.max(0, (iconColW() - BULLET_SIZE) / 2);
            int by = y + Math.max(0, (ROW_H_TEXT - BULLET_SIZE) / 2) + BULLET_Y_NUDGE;
            ctx.fill(bx, by, bx + BULLET_SIZE, by + BULLET_SIZE, 0xFFFFFFFF);
        }

        String valueStr = fmtValue(statRow.stat);

        int valueW = (int)Math.ceil(tr.getWidth(valueStr) * fs);
        int spaceW = (int)Math.ceil(tr.getWidth(" ") * fs);
        int nameLeft = valueLeft + valueW + spaceW;

        ctx.getMatrices().push();
        ctx.getMatrices().scale(fs, fs, 1f);

        int vDrawX = (int)(valueLeft / fs);
        int drawY  = (int)(y / fs);
        ctx.drawTextWithShadow(tr, valueStr, vDrawX, drawY, 0xFFFFFF);

        int nDrawX = (int)(nameLeft / fs);
        ctx.drawTextWithShadow(tr, split.cleanName, nDrawX, drawY, 0xFFFFFF);

        ctx.getMatrices().pop();
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

    private static boolean isBMPPUA(int cp) { return cp >= 0xE000 && cp <= 0xF8FF; }
    private static boolean isCJKCompat(int cp) { return cp >= 0xF900 && cp <= 0xFAFF; }
    private static boolean isPlanePUA(int cp) { return (cp >= 0xF0000 && cp <= 0xFFFFD) || (cp >= 0x100000 && cp <= 0x10FFFD); }

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
                    if (Character.isWhitespace(cp2)) i += Character.charCount(cp2); else break;
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
                        if (s.charAt(i) == '§') i += 2; else break;
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

    private static boolean isPrivateUse(int cp) {
        return (cp >= 0xE000 && cp <= 0xF8FF) || (cp >= 0xF0000 && cp <= 0xFFFFD) || (cp >= 0x100000 && cp <= 0x10FFFD);
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
            if (prefixHeights.get(mid) <= offsetPx) lo = mid; else hi = mid - 1;
        }
        if (lo >= items.size()) lo = Math.max(0, items.size() - 1);
        if (!prefixHeights.isEmpty() && prefixHeights.get(lo) > offsetPx) return 0;
        return lo;
    }

    private void drawScrollbar(DrawContext ctx, int bgX, int bgY, int bgW, int innerY, int innerH, int contentH) {
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

    private enum ItemType { DIVIDER, HEADER, STAT }

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

        static Item stat(StatEntry se, Identifier icon) {
            Item it = new Item(ItemType.STAT, ROW_H_TEXT);
            it.stat = new StatRow(se, icon);
            return it;
        }

        static Item statBullet(StatEntry se) {
            Item it = new Item(ItemType.STAT, ROW_H_TEXT);
            it.stat = new StatRow(se, null, true);
            return it;
        }
    }

    private static class StatRow {
        final StatEntry stat;
        final Identifier icon;
        final boolean bullet;
        StatRow(StatEntry se, Identifier icon) {
            this(se, icon, false);
        }
        StatRow(StatEntry se, Identifier icon, boolean bullet) {
            this.stat = se;
            this.icon = icon;
            this.bullet = bullet;
        }
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
            lines.add(Text.literal(String.format("- %s: %+d%% Base", attr, (int) (value * 100))).formatted(Formatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }
        for (String attr : totalMultMap.keySet()) {
            double value = totalMultMap.get(attr);
            lines.add(Text.literal(String.format("- %s: %+d%% Total", attr, (int) (value * 100))).formatted(Formatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }

        if (!addedAny) {
            lines.add(Text.literal("No active modifiers").formatted(Formatting.GRAY));
            icons.add(ItemStack.EMPTY);
        }

        root.enqueueTooltip(lines, icons, mouseX, mouseY);
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

    @Override public SelectionType getType() { return SelectionType.NONE; }
    @Override public void appendNarrations(NarrationMessageBuilder builder) {}
}
