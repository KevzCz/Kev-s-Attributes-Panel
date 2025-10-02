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
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.attributepanel.compat.TrinketCompat;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;

import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class AttributePanelDrawable implements Drawable, Element, Selectable {

    protected final MinecraftClient client = MinecraftClient.getInstance();
    protected final int x, y, width;
    protected int height;

    protected boolean expanded = false;
    protected int currentPage = 0;
    protected boolean showOnlyChanged = true;
    public static final int MAX_ROWS = 6;

    protected final List<StatEntry> cachedStats = new ArrayList<>();

    protected List<Text> queuedTooltip = null;
    protected List<ItemStack> queuedTooltipIcons = null;
    protected List<Identifier> queuedTooltipTextures = null;
    protected int tooltipX, tooltipY;

    private final BookAttributePanelDrawable bookGui;
    private final VanillaAttributePanelDrawable vanillaGui;
    private final CompactAttributePanelDrawable compactGui;

    public AttributePanelDrawable(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.bookGui = new BookAttributePanelDrawable(this);
        this.vanillaGui = new VanillaAttributePanelDrawable(this);
        this.compactGui = new CompactAttributePanelDrawable(this);
    }

    public void setHeightFromInventory(int inventoryHeight) {
        this.height = inventoryHeight;
    }

    public void toggle() {
        expanded = !expanded;
        currentPage = 0;
        if (expanded) cacheStats();
    }

    public void tick() {
        if (expanded) cacheStats();
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!expanded) return;
        switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK -> bookGui.render(context, mouseX, mouseY, delta);
            case VANILLA -> vanillaGui.render(context, mouseX, mouseY, delta);
            case COMPACT -> compactGui.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!expanded) return false;
        return switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK -> bookGui.mouseClicked(mouseX, mouseY, button);
            case VANILLA -> vanillaGui.mouseClicked(mouseX, mouseY, button);
            case COMPACT -> compactGui.mouseClicked(mouseX, mouseY, button);
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!expanded) return false;
        boolean handled = switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK -> false;
            case VANILLA -> false;
            case COMPACT -> compactGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        };
        return handled;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!expanded) return false;
        return switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK    -> false;
            case VANILLA -> false;
            case COMPACT -> compactGui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        };
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!expanded) return false;
        return switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK    -> false;
            case VANILLA -> false;
            case COMPACT -> compactGui.mouseReleased(mouseX, mouseY, button);
        };
    }


    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!expanded) return false;
        int bgX = left() - 16;
        int bgY = top();
        int bgWidth = panelWidth() + 16;
        int bgHeight = panelHeight();
        return mouseX >= bgX && mouseX <= bgX + bgWidth && mouseY >= bgY && mouseY <= bgY + bgHeight;
    }

    public void renderTooltip(DrawContext context) {
        if (queuedTooltip == null || queuedTooltip.isEmpty()) return;
        TextRenderer tr = client.textRenderer;
        int zOffset = 400;
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, zOffset);

        List<Text> lines = queuedTooltip;
        List<ItemStack> icons = queuedTooltipIcons;

        int maxWidth = 0;
        for (Text line : lines) {
            maxWidth = Math.max(maxWidth, tr.getWidth(line));
        }

        int tooltipWidth = maxWidth + 28;
        int tooltipHeight = lines.size() * (tr.fontHeight + 4) + 12;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int drawX = this.tooltipX + 12;
        int drawY = this.tooltipY + 12;

        if (drawX + tooltipWidth > screenWidth) drawX = screenWidth - tooltipWidth - 8;
        if (drawY + tooltipHeight > screenHeight) drawY = screenHeight - tooltipHeight - 8;
        drawX = Math.max(drawX, 4);
        drawY = Math.max(drawY, 4);

        int backgroundColor = 0xF0131313;
        int borderColor = 0xFF5A5A5A;

        context.fillGradient(drawX - 4, drawY - 4, drawX + tooltipWidth + 4, drawY + tooltipHeight, backgroundColor, backgroundColor);
        context.drawBorder(drawX - 4, drawY - 4, tooltipWidth + 8, tooltipHeight + 1, borderColor);

        for (int i = 0; i < lines.size(); i++) {
            int lineY = drawY + i * (tr.fontHeight + 4);

            ItemStack icon = (queuedTooltipIcons != null && i < queuedTooltipIcons.size())
                    ? queuedTooltipIcons.get(i) : ItemStack.EMPTY;

            Identifier tex = (queuedTooltipTextures != null && i < queuedTooltipTextures.size())
                    ? queuedTooltipTextures.get(i) : null;

            int iconOffset = 0;

            if (tex != null) {
                final int texSize = 12;
                context.drawTexture(tex, drawX, lineY, 0, 0, texSize, texSize, texSize, texSize);
                iconOffset = 18;
            } else if (!icon.isEmpty()) {
                context.getMatrices().push();
                context.getMatrices().translate(drawX, lineY, 0);
                context.getMatrices().scale(0.85f, 0.85f, 1f);
                context.drawItem(icon, 0, 0);
                context.getMatrices().pop();
                iconOffset = 18;
            }

            int textX = drawX + iconOffset;
            context.drawText(tr, lines.get(i), textX, lineY + 2, 0xFFFFFF, false);
        }

        context.getMatrices().pop();
        queuedTooltip = null;
        queuedTooltipIcons = null;
    }

    protected void cacheStats() {
        cachedStats.clear();
        PlayerEntity player = client.player;
        if (player == null) return;

        for (RegistryEntry<EntityAttribute> entry : Registries.ATTRIBUTE.streamEntries().toList()) {
            EntityAttribute attr = entry.value();
            EntityAttributeInstance instance = player.getAttributeInstance(entry);
            if (instance == null) continue;

            Identifier attrId = Registries.ATTRIBUTE.getId(attr);
            String idStr = attrId == null ? "" : attrId.toString();
            String tkey  = attr.getTranslationKey().toLowerCase(java.util.Locale.ROOT);

            double rawBase  = instance.getBaseValue();
            double rawValue = instance.getValue();

            enum Mode { NONE, FRACTION_0_TO_1, BASE_100 }
            Mode mode = Mode.NONE;

            if (AttributesPanelConfig.INSTANCE.percentAttributesBase100.contains(idStr)) {
                mode = Mode.BASE_100;
            } else if (AttributesPanelConfig.INSTANCE.percentAttributes.contains(idStr)) {
                mode = Mode.FRACTION_0_TO_1;
            } else {
                if (containsAny(tkey, AttributesPanelConfig.INSTANCE.percentBase100Keywords)) {
                    mode = Mode.BASE_100;
                } else if (containsAny(tkey, AttributesPanelConfig.INSTANCE.percentKeywords)) {
                    mode = Mode.FRACTION_0_TO_1;
                }
            }

            double baseForDisplay  = rawBase;
            double valueForDisplay = rawValue;
            boolean isPercent = mode != Mode.NONE;

            if (mode == Mode.BASE_100) {
                baseForDisplay  = 0;
                valueForDisplay = (rawValue - rawBase) / rawBase;
            }

            if (showOnlyChanged && AttributesPanelConfig.INSTANCE.guiStyle != AttributesPanelConfig.GuiStyle.COMPACT) {
                if (Double.isNaN(valueForDisplay) || Math.abs(baseForDisplay - valueForDisplay) < 0.001) continue;
            }

            cachedStats.add(new StatEntry(
                    Text.translatable(attr.getTranslationKey()),
                    baseForDisplay,
                    valueForDisplay,
                    isPercent,
                    entry
            ));
        }
        cachedStats.sort((a, b) -> a.name().getString().compareToIgnoreCase(b.name().getString()));
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        if (needles == null || needles.isEmpty()) return false;
        for (String n : needles) {
            if (n != null && !n.isBlank() && haystack.contains(n.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
    protected void enqueueTooltip(List<Text> lines, List<ItemStack> icons, int mouseX, int mouseY) {
        enqueueTooltipRich(lines, icons, null, mouseX, mouseY);
    }

    protected void enqueueTooltipRich(List<Text> lines, List<ItemStack> icons, List<Identifier> textures, int mouseX, int mouseY) {
        this.queuedTooltip = lines;
        this.queuedTooltipIcons = icons;
        this.queuedTooltipTextures = textures;
        this.tooltipX = mouseX;
        this.tooltipY = mouseY;
    }
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (compactGui.wantsKeys()) {

            if (compactGui.handleCharTyped(chr, modifiers)) return true;
            return true;
        }
        return Element.super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (compactGui.wantsKeys()) {
            if (compactGui.handleKeyPressed(keyCode, scanCode, modifiers)) return true;

            return true;
        }
        return Element.super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void drawTooltipContent(DrawContext context,
                                    TextRenderer tr,
                                    int hoverIndex,
                                    int mouseX,
                                    int mouseY,
                                    int rowHeight,
                                    int padding) {
        if (hoverIndex == -1 || hoverIndex >= cachedStats.size()) return;

        StatEntry stat = cachedStats.get(hoverIndex);
        int rowIndex = hoverIndex % MAX_ROWS;
        int yOffset = y + padding + rowIndex * rowHeight;
        int midX = x + width / 2;

        boolean onName = mouseX >= x && mouseX <= midX;
        boolean onValue = mouseX > midX && mouseX <= x + width;

        if (onName) {
            AttributeDescriptionProvider.TooltipContents tooltip = AttributeDescriptionProvider.getTooltip(stat.attribute().value());
            this.queuedTooltip = tooltip.lines();
            this.queuedTooltipIcons = tooltip.icons();
            this.tooltipX = mouseX;
            this.tooltipY = mouseY;
            return;
        }

        if (!onValue) return;

        boolean shiftDown = InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
                client.options.sneakKey.getDefaultKey().getCode()
        );
        List<Double> flatComponents = new ArrayList<>();
        List<Double> baseMultComponents = new ArrayList<>();
        List<Double> totalMultComponents = new ArrayList<>();
        EntityAttributeInstance instance = client.player.getAttributeInstance(stat.attribute());

        List<Text> tooltipLines = new ArrayList<>();
        List<ItemStack> iconStacks = new ArrayList<>();

        tooltipLines.add(Text.translatable("attributepanel.tooltip.base", String.format("%.2f", stat.base())));
        iconStacks.add(ItemStack.EMPTY);

        if (instance != null) {
            tooltipLines.add(Text.translatable("attributepanel.tooltip.final", String.format("%.2f", instance.getValue())));
            iconStacks.add(ItemStack.EMPTY);
        }

        double flat = 0.0, multBase = 0.0, multTotal = 0.0;
        double puffFlat = 0.0, puffBase = 0.0, puffTotal = 0.0;
        boolean hasPuffish = false;

        List<TrinketCompat.TrinketModifierSource> unmatchedTrinketSources = new ArrayList<>();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            unmatchedTrinketSources.addAll(TrinketCompat.getTrinketModifierSources(client.player));
        }

        if (instance != null && !instance.getModifiers().isEmpty()) {
            tooltipLines.add(Text.empty());
            iconStacks.add(ItemStack.EMPTY);
            tooltipLines.add(Text.translatable("attributepanel.message.modifiers").formatted(Formatting.YELLOW));
            iconStacks.add(ItemStack.EMPTY);

            for (EntityAttributeModifier mod : instance.getModifiers()) {
                Identifier rawId = mod.id();

                if (rawId.getNamespace().equals("tiered")) {
                    String fullPath = rawId.getPath();
                    String[] tieredParts = fullPath.split("/");

                    if (tieredParts.length >= 3 && tieredParts[tieredParts.length - 1].contains("_")) {
                        fullPath = tieredParts[tieredParts.length - 1];
                        rawId = Identifier.of("tiered", fullPath);
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
                        double value = mod.value();
                        flat += value;
                        flatComponents.add(value);

                        color = value >= 0 ? Formatting.GREEN : Formatting.RED;
                        String sign = value >= 0 ? "+" : "";
                        opText = sign + String.format("%.2f", value);
                    }

                    case ADD_MULTIPLIED_BASE -> {
                        double value = mod.value();
                        multBase += value;
                        baseMultComponents.add(value);

                        int percent = (int) Math.round(value * 100);
                        color = percent >= 0 ? Formatting.GREEN : Formatting.RED;
                        String sign = percent >= 0 ? "+" : "";
                        opText = sign + percent + "% Base";
                    }

                    case ADD_MULTIPLIED_TOTAL -> {
                        double value = mod.value();
                        multTotal += value;
                        totalMultComponents.add(value);

                        int percent = (int) Math.round(value * 100);
                        color = percent >= 0 ? Formatting.GREEN : Formatting.RED;
                        String sign = percent >= 0 ? "+" : "";
                        opText = sign + percent + "% Total";
                    }

                    default -> {
                        opText = "?";
                        color = Formatting.GRAY;
                    }
                }

                String fullPath = rawId.getPath();
                String[] parts = fullPath.split("\\.", 2);
                Identifier modId = Identifier.of(rawId.getNamespace(), parts[0]);
                String customName = parts.length > 1 ? parts[1] : null;
                boolean usedCustomName = false;
                ItemStack matchingStack = new ItemStack(Registries.ITEM.get(modId));
                Text displayName = Text.literal(formatModifierId(modId));
                boolean foundSource = false;
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = client.player.getEquippedStack(slot);
                    if (stack.isEmpty()) continue;

                    final boolean[] matched = {false};
                    var component = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                    if (component != null) {
                        component.applyModifiers(slot, (attr, entryMod) -> {
                            if (entryMod.id().equals(mod.id()) && attr.equals(stat.attribute())) {
                                matched[0] = true;
                            }
                        });
                    }

                    if (!matched[0]) {
                        stack.getItem().getAttributeModifiers().applyModifiers(slot, (attr, entryMod) -> {
                            if (entryMod.id().equals(mod.id()) && attr.equals(stat.attribute())) {
                                matched[0] = true;
                            }
                        });
                    }

                    if (matched[0]) {
                        matchingStack = stack;
                        if (!stack.isEmpty() && stack.getItem() != Items.AIR && !usedCustomName) {
                            try {
                                displayName = stack.getName().copy();
                            } catch (Exception e) {
                                displayName = Text.literal(stack.getItem().toString()).formatted(Formatting.GRAY);
                            }
                        }
                        foundSource = true;
                        break;
                    }
                }

                if (!foundSource) {
                    String[] pathParts = modId.getPath().split("/");
                    if (pathParts.length > 0) {
                        String itemGuess = pathParts[pathParts.length - 1];
                        Identifier itemId = Identifier.of(modId.getNamespace(), itemGuess);
                        if (Registries.ITEM.containsId(itemId)) {
                            Item item = Registries.ITEM.get(itemId);
                            matchingStack = new ItemStack(item);
                            displayName = matchingStack.getName().copy().formatted(matchingStack.getRarity().getFormatting());
                            foundSource = true;
                        }
                    }
                }

                if (customName != null && !customName.isBlank()) {
                    Set<String> ignoredArmorNames = Set.of("helmet", "chestplate", "leggings", "boots");
                    if (!ignoredArmorNames.contains(customName.toLowerCase())) {
                        String pretty = Arrays.stream(customName.split("_"))
                                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                                .collect(Collectors.joining(" "));
                        displayName = Text.literal(pretty).formatted(Formatting.LIGHT_PURPLE);
                        usedCustomName = true;
                    }
                }

                if (!foundSource && FabricLoader.getInstance().isModLoaded("trinkets")) {
                    for (Iterator<TrinketCompat.TrinketModifierSource> iter = unmatchedTrinketSources.iterator(); iter.hasNext(); ) {
                        var source = iter.next();

                        if (
                                source.id().value().equals(stat.attribute().value()) &&
                                        source.modifier().operation() == mod.operation() &&
                                        Math.abs(source.modifier().value() - mod.value()) < 0.0001
                        ) {
                            matchingStack = source.stack();

                            if (!matchingStack.isEmpty() && matchingStack.getItem() != Items.AIR) {
                                try {
                                    displayName = matchingStack.getName().copy()
                                            .formatted(matchingStack.getRarity().getFormatting());
                                } catch (Exception e) {
                                    displayName = Text.literal("Unknown Trinket").formatted(Formatting.GRAY);
                                }
                            } else {
                                displayName = Text.literal("Unknown Trinket").formatted(Formatting.GRAY);
                            }

                            foundSource = true;
                            iter.remove();
                            break;
                        }
                    }
                }





                if (!foundSource) {
                    for (var entry : client.player.getStatusEffects()) {
                        StatusEffect effect = entry.getEffectType().value();
                        int amplifier = entry.getAmplifier();
                        EntityAttribute attr = stat.attribute().value();

                        Map<EntityAttribute, EntityAttributeModifier> effectMods = new HashMap<>();
                        effect.forEachAttributeModifier(amplifier, (attribute, modifier) -> {
                            effectMods.put(attribute.value(), modifier);
                        });

                        if (effectMods.containsKey(attr)) {
                            EntityAttributeModifier potionMod = effectMods.get(attr);
                            if (potionMod.operation() == mod.operation() &&
                                    Math.abs(potionMod.value() - mod.value()) < 0.0001) {
                                displayName = Text.translatable(effect.getTranslationKey());
                                matchingStack = createColoredPotionItem(effect);
                                foundSource = true;
                                break;
                            }
                        }
                    }
                }

                if (modId.getNamespace().equals("tiered")) {
                    String path = modId.getPath();
                    String[] pathParts = path.split("_");
                    String tierRarity = pathParts.length > 0 ? pathParts[0] : "Tiered";
                    String tierName = tierRarity.substring(0, 1).toUpperCase() + tierRarity.substring(1).toLowerCase();

                    Text tierLine = Text.literal(tierName + " Bonus: ").formatted(Formatting.AQUA)
                            .append(Text.literal(opText).formatted(Formatting.GREEN));
                    tooltipLines.add(tierLine);
                    iconStacks.add(new ItemStack(Items.ANVIL));
                } else {
                    Text displayLine = displayName.copy()
                            .append(" ")
                            .append(Text.literal(opText).formatted(color));
                    tooltipLines.add(displayLine);
                    iconStacks.add(matchingStack);
                }
            }

            if (hasPuffish) {
                tooltipLines.add(Text.translatable("attributepanel.tooltip.skill_tree_bonus").formatted(Formatting.AQUA));
                iconStacks.add(ItemStack.EMPTY);

                if (puffFlat != 0.0) {
                    tooltipLines.add(Text.literal(String.format("- %+,.2f", puffFlat)).formatted(Formatting.GREEN));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (puffBase != 0.0) {
                    tooltipLines.add(Text.literal(String.format("- %+d%% Base", (int)(puffBase * 100))).formatted(Formatting.GREEN));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (puffTotal != 0.0) {
                    tooltipLines.add(Text.literal(String.format("- %+d%% Total", (int)(puffTotal * 100))).formatted(Formatting.GREEN));
                    iconStacks.add(ItemStack.EMPTY);
                }

                tooltipLines.add(Text.empty());
                iconStacks.add(ItemStack.EMPTY);
            }
        }

        tooltipLines.add(Text.empty());
        iconStacks.add(ItemStack.EMPTY);
        if (stat.isChanged()) {
            if (shiftDown) {
                double base = stat.base();
                double flatTotal = flat;
                double tieredMultTotal = 0.0;
                double nonTieredMultTotal = 0.0;

                instance = client.player.getAttributeInstance(stat.attribute());
                if (instance != null && !instance.getModifiers().isEmpty()) {
                    for (EntityAttributeModifier mod : instance.getModifiers()) {
                        if (mod.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                            if (mod.id().getNamespace().equals("tiered")) {
                                tieredMultTotal += mod.value();
                            } else {
                                nonTieredMultTotal += mod.value();
                            }
                        }
                    }
                }

                double basePlusAdditive = base + flatTotal;
                double afterTiered = (tieredMultTotal != 0.0) ? basePlusAdditive * (1.0 + tieredMultTotal) : basePlusAdditive;
                double afterBaseMult = (multBase != 0.0) ? afterTiered * (1.0 + multBase) : afterTiered;
                double finalValue = (nonTieredMultTotal != 0.0) ? afterBaseMult * (1.0 + nonTieredMultTotal) : afterBaseMult;

                tooltipLines.add(Text.translatable("attributepanel.tooltip.calculated").formatted(Formatting.DARK_GRAY));
                iconStacks.add(ItemStack.EMPTY);

                if (flatTotal != 0.0) {
                    String flatBreakdown = flatComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");
                    tooltipLines.add(Text.literal(String.format("⟶ %.2f + (%s) = %.2f", base, flatBreakdown, basePlusAdditive)).formatted(Formatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                } else {
                    tooltipLines.add(Text.literal(String.format("= %.2f", base)).formatted(Formatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (tieredMultTotal != 0.0) {
                    tooltipLines.add(Text.literal(String.format("⟶ %.2f × %.2f = %.2f", basePlusAdditive, 1.0 + tieredMultTotal, afterTiered)).formatted(Formatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (multBase != 0.0) {
                    String baseMultBreakdown = baseMultComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");
                    tooltipLines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterTiered, baseMultBreakdown, afterBaseMult)).formatted(Formatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (nonTieredMultTotal != 0.0) {
                    List<Double> nonTieredTotalComponents = new ArrayList<>();
                    for (EntityAttributeModifier mod : instance.getModifiers()) {
                        if (mod.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL &&
                                !mod.id().getNamespace().equals("tiered")) {
                            nonTieredTotalComponents.add(mod.value());
                        }
                    }

                    String totalMultBreakdown = nonTieredTotalComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");

                    if (!nonTieredTotalComponents.isEmpty()) {
                        tooltipLines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterBaseMult, totalMultBreakdown, finalValue)).formatted(Formatting.GRAY));
                    }
                }

                tooltipLines.add(Text.literal("= " + String.format("%.2f", finalValue)).formatted(Formatting.GREEN));
                iconStacks.add(ItemStack.EMPTY);
                if (instance != null) {
                    double actualFinal = instance.getValue();
                    double indirectFlatBonus = actualFinal - finalValue;
                    if (Math.abs(indirectFlatBonus) > 0.001 && finalValue > 0.001) {
                        double indirectPercent = indirectFlatBonus / finalValue;

                        tooltipLines.add(Text.empty());
                        iconStacks.add(ItemStack.EMPTY);
                        if (indirectFlatBonus > 0) {
                            tooltipLines.add(Text.translatable("attributepanel.tooltip.indirect_bonus", String.format("%.2f", indirectFlatBonus)).formatted(Formatting.DARK_GREEN));
                            iconStacks.add(ItemStack.EMPTY);
                            tooltipLines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %.2f) = %.2f", finalValue, indirectPercent, actualFinal)).formatted(Formatting.GRAY));
                            iconStacks.add(ItemStack.EMPTY);
                        } else {
                            tooltipLines.add(Text.translatable("attributepanel.tooltip.indirect_decrease", String.format("%.2f", Math.abs(indirectFlatBonus))).formatted(Formatting.RED));
                            iconStacks.add(ItemStack.EMPTY);
                            tooltipLines.add(Text.literal(String.format("⟶ %.2f × (1.00 - %.2f) = %.2f", finalValue, Math.abs(indirectPercent), actualFinal)).formatted(Formatting.GRAY));
                            iconStacks.add(ItemStack.EMPTY);
                        }

                    }
                }

            } else {
                tooltipLines.add(Text.translatable("attributepanel.tooltip.hold_shift").formatted(Formatting.GRAY));
                iconStacks.add(ItemStack.EMPTY);
            }
        }

        this.queuedTooltip = tooltipLines;
        this.queuedTooltipIcons = iconStacks;
        this.tooltipX = mouseX;
        this.tooltipY = mouseY;
    }


    protected ItemStack createColoredPotionItem(StatusEffect effect) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable(effect.getTranslationKey()));
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("CustomPotionColor", 0xFF0000);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return stack;
    }

    protected static String formatModifierId(Identifier id) {
        String path = id.getPath();
        if (path.contains("/")) path = path.substring(0, path.indexOf('/'));
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    protected static boolean mouseIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public MinecraftClient mc() { return client; }
    public int left() { return x; }
    public int top() { return y; }
    public int panelWidth() { return width; }
    public int panelHeight() { return height; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int p) { currentPage = p; }
    public boolean getShowOnlyChanged() { return showOnlyChanged; }
    public void setShowOnlyChanged(boolean v) { showOnlyChanged = v; }

    public List<StatEntry> getCachedStats() { return cachedStats; }

    @Override public void setFocused(boolean focused) {}
    @Override public boolean isFocused() { return false; }
    @Override public SelectionType getType() { return SelectionType.NONE; }
    @Override public void appendNarrations(NarrationMessageBuilder builder) {}
}
