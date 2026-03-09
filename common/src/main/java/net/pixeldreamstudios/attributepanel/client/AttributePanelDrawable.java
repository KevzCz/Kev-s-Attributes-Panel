package net.pixeldreamstudios.attributepanel.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;
import net.pixeldreamstudios.attributepanel.compat.TrinketsCompat;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;
@Environment(EnvType.CLIENT)
public class AttributePanelDrawable implements Renderable, GuiEventListener, NarratableEntry {

    protected final Minecraft client = Minecraft.getInstance();
    protected final int x, y, width;
    protected int height;

    protected boolean expanded = false;
    protected int currentPage = 0;
    protected boolean showOnlyChanged = true;
    public static final int MAX_ROWS = 6;

    protected final List<StatEntry> cachedStats = new ArrayList<>();

    protected List<Component> queuedTooltip = null;
    protected List<ItemStack> queuedTooltipIcons = null;
    protected List<ResourceLocation> queuedTooltipTextures = null;
    protected int tooltipX, tooltipY;

    private final BookAttributePanelDrawable bookGui;
    private final VanillaAttributePanelDrawable vanillaGui;
    private final CompactAttributePanelDrawable compactGui;
    
    protected final AttributeAnimationState animationState = new AttributeAnimationState();
    private final Map<ResourceLocation, Double> lastKnownValues = new HashMap<>();

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
        if (expanded) {
            cacheStats();
        } else {
            animationState.reset();
        }
    }

    public void tick() {
        if (expanded) {
            cacheStats();
            animationState.tick();
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
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
        return switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK -> false;
            case VANILLA -> false;
            case COMPACT -> compactGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        };
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!expanded) return false;
        return switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK -> false;
            case VANILLA -> false;
            case COMPACT -> compactGui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        };
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!expanded) return false;
        return switch (AttributesPanelConfig.INSTANCE.guiStyle) {
            case BOOK -> false;
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

    public void renderTooltip(GuiGraphics context) {
        if (queuedTooltip == null || queuedTooltip.isEmpty()) return;
        Font font = client.font;
        int zOffset = 400;

        PoseStack pose = context.pose();
        pose.pushPose();
        pose.translate(0, 0, zOffset);

        List<Component> lines = queuedTooltip;
        List<ItemStack> icons = queuedTooltipIcons;

        int maxWidth = 0;
        for (Component line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        int tooltipWidth = maxWidth + 28;
        int tooltipHeight = lines.size() * (font.lineHeight + 4) + 12;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int drawX = this.tooltipX + 12;
        int drawY = this.tooltipY + 12;

        if (drawX + tooltipWidth > screenWidth) drawX = screenWidth - tooltipWidth - 8;
        if (drawY + tooltipHeight > screenHeight) drawY = screenHeight - tooltipHeight - 8;
        drawX = Math.max(drawX, 4);
        drawY = Math.max(drawY, 4);

        int backgroundColor = 0xF0131313;
        int borderColor = 0xFF5A5A5A;

        context.fillGradient(drawX - 4, drawY - 4, drawX + tooltipWidth + 4, drawY + tooltipHeight, backgroundColor, backgroundColor);
        context.fill(drawX - 4, drawY - 4, drawX + tooltipWidth + 4, drawY - 3, borderColor);
        context.fill(drawX - 4, drawY + tooltipHeight, drawX + tooltipWidth + 4, drawY + tooltipHeight + 1, borderColor);
        context.fill(drawX - 4, drawY - 4, drawX - 3, drawY + tooltipHeight + 1, borderColor);
        context.fill(drawX + tooltipWidth + 3, drawY - 4, drawX + tooltipWidth + 4, drawY + tooltipHeight + 1, borderColor);

        for (int i = 0; i < lines.size(); i++) {
            int lineY = drawY + i * (font.lineHeight + 4);

            ItemStack icon = (queuedTooltipIcons != null && i < queuedTooltipIcons.size())
                    ? queuedTooltipIcons.get(i) : ItemStack.EMPTY;

            ResourceLocation tex = (queuedTooltipTextures != null && i < queuedTooltipTextures.size())
                    ? queuedTooltipTextures.get(i) : null;

            int iconOffset = 0;

            if (tex != null) {
                final int texSize = 12;
                context.blit(tex, drawX, lineY, 0, 0, texSize, texSize, texSize, texSize);
                iconOffset = 18;
            } else if (! icon.isEmpty()) {
                pose.pushPose();
                pose.translate(drawX, lineY, 0);
                pose.scale(0.85f, 0.85f, 1f);
                context.renderItem(icon, 0, 0);
                pose.popPose();
                iconOffset = 18;
            }

            int textX = drawX + iconOffset;
            context.drawString(font, lines.get(i), textX, lineY + 2, 0xFFFFFF, false);
        }

        pose.popPose();
        queuedTooltip = null;
        queuedTooltipIcons = null;
    }

    protected void cacheStats() {
        cachedStats.clear();
        Player player = client.player;
        if (player == null) return;

        for (Holder<Attribute> entry : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            Attribute attr = entry.value();
            AttributeInstance instance = player.getAttribute(entry);
            if (instance == null) continue;

            ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attr);
            String idStr = attrId == null ? "" : attrId.toString();
            String tkey = attr.getDescriptionId().toLowerCase(Locale.ROOT);

            double rawBase = instance.getBaseValue();
            double rawValue = instance.getValue();
            
            DisplayMode displayMode = DisplayMode.NONE;

            // Check explicit attribute ID lists first
            if (AttributesPanelConfig.INSTANCE.percentAttributesBase100.contains(idStr)) {
                displayMode = DisplayMode.BASE_100;
            } else if (AttributesPanelConfig.INSTANCE.percentAttributes.contains(idStr)) {
                displayMode = DisplayMode.FRACTION_0_TO_1;
            } else if (AttributesPanelConfig.INSTANCE.multiplierAttributesBase100.contains(idStr)) {
                displayMode = DisplayMode.MULTIPLIER_BASE_100;
            } else if (AttributesPanelConfig.INSTANCE.multiplierAttributesBase1.contains(idStr)) {
                displayMode = DisplayMode.MULTIPLIER_BASE_1;
            } else if (AttributesPanelConfig.INSTANCE.multiplierAttributesBase0.contains(idStr)) {
                displayMode = DisplayMode.MULTIPLIER_BASE_0;
            } else {
                // Otherwise check keywords in translation key
                if (containsAny(tkey, AttributesPanelConfig.INSTANCE.percentBase100Keywords)) {
                    displayMode = DisplayMode.BASE_100;
                } else if (containsAny(tkey, AttributesPanelConfig.INSTANCE.percentKeywords)) {
                    displayMode = DisplayMode.FRACTION_0_TO_1;
                } else if (containsAny(tkey, AttributesPanelConfig.INSTANCE.multiplierBase100Keywords)) {
                    displayMode = DisplayMode.MULTIPLIER_BASE_100;
                } else if (containsAny(tkey, AttributesPanelConfig.INSTANCE.multiplierBase1Keywords)) {
                    displayMode = DisplayMode.MULTIPLIER_BASE_1;
                } else if (containsAny(tkey, AttributesPanelConfig.INSTANCE.multiplierBase0Keywords)) {
                    displayMode = DisplayMode.MULTIPLIER_BASE_0;
                }
            }

            double baseForDisplay = rawBase;
            double valueForDisplay = rawValue;

            switch (displayMode) {
                case BASE_100:
                    if (Math.abs(rawBase) < 0.001) {
                        baseForDisplay = 0.0;
                        valueForDisplay = rawValue;
                    } else {
                        baseForDisplay = 0.0;
                        valueForDisplay = (rawValue - rawBase) / rawBase;
                    }
                    break;
                    
                case MULTIPLIER_BASE_100:
                    baseForDisplay = rawBase / 100.0;
                    valueForDisplay = rawValue / 100.0;
                    break;
                    
                case MULTIPLIER_BASE_1:
                    baseForDisplay = rawBase;
                    valueForDisplay = rawValue;
                    break;
                    
                case MULTIPLIER_BASE_0:
                    baseForDisplay = rawBase;
                    valueForDisplay = rawValue;
                    break;
                    
                default:
                    break;
            }

            int bonusCount = 0;
            if (Double.isNaN(rawBase) || Double.isNaN(rawValue)) {
                if (instance.getModifiers() != null) {
                    bonusCount = instance.getModifiers().size();
                }
            }

            if (showOnlyChanged && AttributesPanelConfig.INSTANCE.guiStyle != AttributesPanelConfig.GuiStyle.COMPACT) {
                if (! Double.isNaN(valueForDisplay) && Math.abs(baseForDisplay - valueForDisplay) < 0.001) continue;
                if (Double.isNaN(valueForDisplay)) continue;
            }

            if (attrId != null && AttributesPanelConfig.INSTANCE.enableSmoothValueTransition) {
                Double lastValue = lastKnownValues.get(attrId);
                if (lastValue != null && Math.abs(lastValue - rawValue) > 0.001) {
                    animationState.updateValue(entry, lastValue, rawValue);
                }
                lastKnownValues.put(attrId, rawValue);
            }

            cachedStats.add(new StatEntry(
                    Component.translatable(attr.getDescriptionId()),
                    baseForDisplay,
                    valueForDisplay,
                    displayMode,
                    entry,
                    bonusCount,
                    rawBase,
                    rawValue
            ));
        }
        cachedStats.sort((a, b) -> a.name().getString().compareToIgnoreCase(b.name().getString()));
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        if (needles == null || needles.isEmpty()) return false;
        for (String n : needles) {
            if (n != null && ! n.isBlank() && haystack.contains(n.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    protected void enqueueTooltip(List<Component> lines, List<ItemStack> icons, int mouseX, int mouseY) {
        enqueueTooltipRich(lines, icons, null, mouseX, mouseY);
    }

    protected void enqueueTooltipRich(List<Component> lines, List<ItemStack> icons, List<ResourceLocation> textures, int mouseX, int mouseY) {
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
        return GuiEventListener.super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (compactGui.wantsKeys()) {
            if (compactGui.handleKeyPressed(keyCode, scanCode, modifiers)) return true;
            return true;
        }
        return GuiEventListener.super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void drawTooltipContent(GuiGraphics context,
                                   Font font,
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

        if (! onValue) return;

        boolean shiftDown = GLFW.glfwGetKey(
                Minecraft.getInstance().getWindow().getWindow(),
                client.options.keyShift.getDefaultKey().getValue()
        ) == GLFW.GLFW_PRESS;

        List<Double> flatComponents = new ArrayList<>();
        List<Double> baseMultComponents = new ArrayList<>();
        List<Double> totalMultComponents = new ArrayList<>();
        AttributeInstance instance = client.player.getAttribute(stat.attribute());

        List<Component> tooltipLines = new ArrayList<>();
        List<ItemStack> iconStacks = new ArrayList<>();

        tooltipLines.add(Component.translatable("attributepanel.tooltip.base", String.format("%.2f", stat.base())));
        iconStacks.add(ItemStack.EMPTY);

        if (instance != null) {
            tooltipLines.add(Component.translatable("attributepanel.tooltip.final", String.format("%.2f", instance.getValue())));
            iconStacks.add(ItemStack.EMPTY);
        }

        double flat = 0.0, multBase = 0.0, multTotal = 0.0;
        double puffFlat = 0.0, puffBase = 0.0, puffTotal = 0.0;
        boolean hasPuffish = false;

        List<Object> unmatchedAccessorySources = new ArrayList<>();
        if (TrinketsCompat.isLoaded()) {
            unmatchedAccessorySources.addAll(TrinketsCompat.getTrinketModifierSources(client.player));
        }
        if (CuriosCompat.isLoaded()) {
            unmatchedAccessorySources.addAll(CuriosCompat.getCurioModifierSources(client.player));
        }

        if (instance != null && !instance.getModifiers().isEmpty()) {
            tooltipLines.add(Component.empty());
            iconStacks.add(ItemStack.EMPTY);
            tooltipLines.add(Component.translatable("attributepanel.message.modifiers").withStyle(ChatFormatting.YELLOW));
            iconStacks.add(ItemStack.EMPTY);

            for (AttributeModifier mod : instance.getModifiers()) {
                ResourceLocation rawId = mod.id();

                if (rawId.getNamespace().equals("tiered")) {
                    String fullPath = rawId.getPath();
                    String[] tieredParts = fullPath.split("/");

                    if (tieredParts.length >= 3 && tieredParts[tieredParts.length - 1].contains("_")) {
                        fullPath = tieredParts[tieredParts.length - 1];
                        rawId = ResourceLocation.fromNamespaceAndPath("tiered", fullPath);
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
                        double value = mod.amount();
                        flat += value;
                        flatComponents.add(value);

                        color = value >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                        String sign = value >= 0 ? "+" : "";
                        opText = sign + String.format("%.2f", value);
                    }

                    case ADD_MULTIPLIED_BASE -> {
                        double value = mod.amount();
                        multBase += value;
                        baseMultComponents.add(value);

                        int percent = (int) Math.round(value * 100);
                        color = percent >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                        String sign = percent >= 0 ? "+" :  "";
                        opText = sign + percent + "% Base";
                    }

                    case ADD_MULTIPLIED_TOTAL -> {
                        double value = mod.amount();
                        multTotal += value;
                        totalMultComponents.add(value);

                        int percent = (int) Math.round(value * 100);
                        color = percent >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                        String sign = percent >= 0 ? "+" : "";
                        opText = sign + percent + "% Total";
                    }

                    default -> {
                        opText = "? ";
                        color = ChatFormatting.GRAY;
                    }
                }

                String fullPath = rawId.getPath();
                String[] parts = fullPath.split("\\.", 2);
                ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), parts[0]);
                String customName = parts.length > 1 ? parts[1] : null;
                boolean usedCustomName = false;
                ItemStack matchingStack = new ItemStack(BuiltInRegistries.ITEM.get(modId));
                Component displayName = Component.literal(formatModifierId(modId));
                boolean foundSource = false;

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = client.player.getSlot(slot.getIndex()).get();
                    if (stack.isEmpty()) continue;

                    final boolean[] matched = {false};
                    var component = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                    if (component != null) {
                        component.modifiers().forEach(entry -> {
                            if (entry.slot().test(slot) &&
                                    entry.modifier().id().equals(mod.id()) &&
                                    entry.attribute().equals(stat.attribute())) {
                                matched[0] = true;
                            }
                        });
                    }

                    if (!matched[0]) {
                        var defaultMods = stack.getItem().getDefaultAttributeModifiers();
                        defaultMods.modifiers().forEach(entry -> {
                            if (entry.slot().test(slot) &&
                                    entry.modifier().id().equals(mod.id()) &&
                                    entry.attribute().equals(stat.attribute())) {
                                matched[0] = true;
                            }
                        });
                    }

                    if (matched[0]) {
                        matchingStack = stack;
                        if (! stack.isEmpty() && stack.getItem() != Items.AIR && ! usedCustomName) {
                            try {
                                displayName = stack.getHoverName().copy();
                            } catch (Exception e) {
                                displayName = Component.literal(stack.getItem().toString()).withStyle(ChatFormatting.GRAY);
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
                        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(modId.getNamespace(), itemGuess);
                        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                            Item item = BuiltInRegistries.ITEM.get(itemId);
                            matchingStack = new ItemStack(item);
                            displayName = matchingStack.getHoverName().copy().withStyle(matchingStack.getRarity().color());
                            foundSource = true;
                        }
                    }
                }

                if (customName != null && !customName.isBlank()) {
                    Set<String> ignoredArmorNames = Set.of("helmet", "chestplate", "leggings", "boots");
                    if (! ignoredArmorNames.contains(customName.toLowerCase())) {
                        String pretty = Arrays.stream(customName.split("_"))
                                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                                .collect(Collectors.joining(" "));
                        displayName = Component.literal(pretty).withStyle(ChatFormatting.LIGHT_PURPLE);
                        usedCustomName = true;
                    }
                }

                if (! foundSource) {
                    for (Iterator<Object> iter = unmatchedAccessorySources.iterator(); iter.hasNext(); ) {
                        var source = iter.next();

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

                        if (sourceAttr != null && sourceAttr.value().equals(stat.attribute().value()) &&
                                sourceMod != null && sourceMod.operation() == mod.operation() &&
                                Math.abs(sourceMod.amount() - mod.amount()) < 0.0001) {
                            matchingStack = sourceStack;

                            if (!matchingStack.isEmpty() && matchingStack.getItem() != Items.AIR) {
                                try {
                                    displayName = matchingStack.getHoverName().copy()
                                            .withStyle(matchingStack.getRarity().color());
                                } catch (Exception e) {
                                    displayName = Component.literal("Unknown Accessory").withStyle(ChatFormatting.GRAY);
                                }
                            } else {
                                displayName = Component.literal("Unknown Accessory").withStyle(ChatFormatting.GRAY);
                            }

                            foundSource = true;
                            iter.remove();
                            break;
                        }
                    }
                }

                if (!foundSource) {
                    for (var entry : client.player.getActiveEffects()) {
                        var effect = entry.getEffect().value();
                        int amplifier = entry.getAmplifier();
                        Attribute attr = stat.attribute().value();

                        Map<Attribute, AttributeModifier> effectMods = new HashMap<>();
                        effect.createModifiers(amplifier, (attribute, modifier) -> {
                            effectMods.put(attribute.value(), modifier);
                        });

                        if (effectMods.containsKey(attr)) {
                            AttributeModifier potionMod = effectMods.get(attr);
                            if (potionMod.operation() == mod.operation() &&
                                    Math.abs(potionMod.amount() - mod.amount()) < 0.0001) {
                                displayName = Component.translatable(effect.getDescriptionId());
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

                    Component tierLine = Component.literal(tierName + " Bonus:  ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(opText).withStyle(ChatFormatting.GREEN));
                    tooltipLines.add(tierLine);
                    iconStacks.add(new ItemStack(Items.ANVIL));
                } else {
                    Component displayLine = displayName.copy()
                            .append(" ")
                            .append(Component.literal(opText).withStyle(color));
                    tooltipLines.add(displayLine);
                    iconStacks.add(matchingStack);
                }
            }

            if (hasPuffish) {
                tooltipLines.add(Component.translatable("attributepanel.tooltip.skill_tree_bonus").withStyle(ChatFormatting.AQUA));
                iconStacks.add(ItemStack.EMPTY);

                if (puffFlat != 0.0) {
                    tooltipLines.add(Component.literal(String.format("- %+,.2f", puffFlat)).withStyle(ChatFormatting.GREEN));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (puffBase != 0.0) {
                    tooltipLines.add(Component.literal(String.format("- %+d%% Base", (int)(puffBase * 100))).withStyle(ChatFormatting.GREEN));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (puffTotal != 0.0) {
                    tooltipLines.add(Component.literal(String.format("- %+d%% Total", (int)(puffTotal * 100))).withStyle(ChatFormatting.GREEN));
                    iconStacks.add(ItemStack.EMPTY);
                }

                tooltipLines.add(Component.empty());
                iconStacks.add(ItemStack.EMPTY);
            }
        }

        tooltipLines.add(Component.empty());
        iconStacks.add(ItemStack.EMPTY);

        if (stat.isChanged()) {
            if (shiftDown) {
                double base = stat.base();
                double flatTotal = flat;
                double tieredMultTotal = 0.0;
                double nonTieredMultTotal = 0.0;

                instance = client.player.getAttribute(stat.attribute());
                if (instance != null && ! instance.getModifiers().isEmpty()) {
                    for (AttributeModifier mod : instance.getModifiers()) {
                        if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                            if (mod.id().getNamespace().equals("tiered")) {
                                tieredMultTotal += mod.amount();
                            } else {
                                nonTieredMultTotal += mod.amount();
                            }
                        }
                    }
                }

                double basePlusAdditive = base + flatTotal;
                double afterTiered = (tieredMultTotal != 0.0) ? basePlusAdditive * (1.0 + tieredMultTotal) : basePlusAdditive;
                double afterBaseMult = (multBase != 0.0) ? afterTiered * (1.0 + multBase) : afterTiered;
                double finalValue = (nonTieredMultTotal != 0.0) ? afterBaseMult * (1.0 + nonTieredMultTotal) : afterBaseMult;

                tooltipLines.add(Component.translatable("attributepanel.tooltip.calculated").withStyle(ChatFormatting.DARK_GRAY));
                iconStacks.add(ItemStack.EMPTY);

                if (flatTotal != 0.0) {
                    String flatBreakdown = flatComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");
                    tooltipLines.add(Component.literal(String.format("⟶ %.2f + (%s) = %.2f", base, flatBreakdown, basePlusAdditive)).withStyle(ChatFormatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                } else {
                    tooltipLines.add(Component.literal(String.format("= %.2f", base)).withStyle(ChatFormatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (tieredMultTotal != 0.0) {
                    tooltipLines.add(Component.literal(String.format("⟶ %.2f × %.2f = %.2f", basePlusAdditive, 1.0 + tieredMultTotal, afterTiered)).withStyle(ChatFormatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (multBase != 0.0) {
                    String baseMultBreakdown = baseMultComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");
                    tooltipLines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterTiered, baseMultBreakdown, afterBaseMult)).withStyle(ChatFormatting.GRAY));
                    iconStacks.add(ItemStack.EMPTY);
                }

                if (nonTieredMultTotal != 0.0) {
                    List<Double> nonTieredTotalComponents = new ArrayList<>();
                    for (AttributeModifier mod : instance.getModifiers()) {
                        if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL &&
                                ! mod.id().getNamespace().equals("tiered")) {
                            nonTieredTotalComponents.add(mod.amount());
                        }
                    }

                    String totalMultBreakdown = nonTieredTotalComponents.stream()
                            .map(v -> String.format("%.2f", v))
                            .reduce((a, b) -> a + " + " + b).orElse("0.00");

                    if (! nonTieredTotalComponents.isEmpty()) {
                        tooltipLines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterBaseMult, totalMultBreakdown, finalValue)).withStyle(ChatFormatting.GRAY));
                    }
                }

                tooltipLines.add(Component.literal("= " + String.format("%.2f", finalValue)).withStyle(ChatFormatting.GREEN));
                iconStacks.add(ItemStack.EMPTY);

                if (instance != null) {
                    double actualFinal = instance.getValue();
                    double indirectFlatBonus = actualFinal - finalValue;
                    if (Math.abs(indirectFlatBonus) > 0.001 && finalValue > 0.001) {
                        double indirectPercent = indirectFlatBonus / finalValue;

                        tooltipLines.add(Component.empty());
                        iconStacks.add(ItemStack.EMPTY);
                        if (indirectFlatBonus > 0) {
                            tooltipLines.add(Component.translatable("attributepanel.tooltip.indirect_bonus", String.format("%.2f", indirectFlatBonus)).withStyle(ChatFormatting.DARK_GREEN));
                            iconStacks.add(ItemStack.EMPTY);
                            tooltipLines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %.2f) = %.2f", finalValue, indirectPercent, actualFinal)).withStyle(ChatFormatting.GRAY));
                            iconStacks.add(ItemStack.EMPTY);
                        } else {
                            tooltipLines.add(Component.translatable("attributepanel.tooltip.indirect_decrease", String.format("%.2f", Math.abs(indirectFlatBonus))).withStyle(ChatFormatting.RED));
                            iconStacks.add(ItemStack.EMPTY);
                            tooltipLines.add(Component.literal(String.format("⟶ %.2f × (1.00 - %.2f) = %.2f", finalValue, Math.abs(indirectPercent), actualFinal)).withStyle(ChatFormatting.GRAY));
                            iconStacks.add(ItemStack.EMPTY);
                        }
                    }
                }

            } else {
                tooltipLines.add(Component.translatable("attributepanel.tooltip.hold_shift").withStyle(ChatFormatting.GRAY));
                iconStacks.add(ItemStack.EMPTY);
            }
        }

        this.queuedTooltip = tooltipLines;
        this.queuedTooltipIcons = iconStacks;
        this.tooltipX = mouseX;
        this.tooltipY = mouseY;
    }

    protected ItemStack createColoredPotionItem(net.minecraft.world.effect.MobEffect effect) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(effect.getDescriptionId()));
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("CustomPotionColor", 0xFF0000);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return stack;
    }

    protected static String formatModifierId(ResourceLocation id) {
        String path = id.getPath();
        if (path.contains("/")) path = path.substring(0, path.indexOf('/'));
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    protected static boolean mouseIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public Minecraft mc() { return client; }
    public int left() { return x; }
    public int top() { return y; }
    public int panelWidth() { return width; }
    public int panelHeight() { return height; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int p) { currentPage = p; }
    public boolean getShowOnlyChanged() { return showOnlyChanged; }
    public void setShowOnlyChanged(boolean v) { showOnlyChanged = v; }

    public List<StatEntry> getCachedStats() { return cachedStats; }
    
    public AttributeAnimationState getAnimationState() { return animationState; }

    @Override public void setFocused(boolean focused) {}
    @Override public boolean isFocused() { return false; }
    @Override public NarratableEntry.NarrationPriority narrationPriority() { return NarratableEntry.NarrationPriority.NONE; }
    @Override public void updateNarration(NarrationElementOutput builder) {}
}