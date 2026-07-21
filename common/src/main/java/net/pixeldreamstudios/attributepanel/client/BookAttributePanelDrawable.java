package net.pixeldreamstudios.attributepanel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;
import net.pixeldreamstudios.attributepanel.compat.IconLeadingCompat;
import net.pixeldreamstudios.attributepanel.compat.TrinketsCompat;
import net.spell_engine.api.item.set.EquipmentSet;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

import static net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable.formatModifierId;
@Environment(EnvType.CLIENT)
class BookAttributePanelDrawable {
    private final AttributePanelDrawable root;

    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/book.png");
    private static final ResourceLocation INFO_ICON = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/attribute_book.png");
    private static final int INFO_ICON_SIZE = 8;
    private static final ResourceLocation DD_POWER_ICON = ResourceLocation.fromNamespaceAndPath("dungeon_difficulty", "textures/symbol/power_level.png");
    private static final ResourceLocation FTB_QUEST_BOOK_ICON = ResourceLocation.fromNamespaceAndPath("ftbquests", "textures/item/book.png");
    private static final ResourceLocation PERMA = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/perma.png");
    private static final ResourceLocation EQUIPPED = ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/equipped.png");

    BookAttributePanelDrawable(AttributePanelDrawable root) {
        this.root = root;
    }

    void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Font font = root.mc().font;
        int rowHeight = 20;
        int padding = 20;

        context.blit(BOOK_TEXTURE, root.left() - 25, root.top(), 0, 0, 240, 230, 240, 230);

        int visibleRows = AttributePanelDrawable.MAX_ROWS;
        int totalPages = (int) Math.ceil(root.getCachedStats().size() / (float) visibleRows);
        root.setCurrentPage(Math.min(root.getCurrentPage(), Math.max(totalPages - 1, 0)));

        if (root.getShowOnlyChanged() && root.getCachedStats().isEmpty()) {
            String noStatsText = "No changed attributes";
            int textWidth = font.width(noStatsText);
            drawBookButtons(context, font, mouseX, mouseY, root.top() + root.panelHeight() - 20);
            context.drawString(font, noStatsText, root.left() + (root.panelWidth() - textWidth) / 2 + 5, root.top() + 20, 0x3F3F3F, false);
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

            int maxNameWidth = root.panelWidth() / 2 - 7;
            context.fill(root.left() + 12, yOffset + rowHeight + 2, root.left() + root.panelWidth() - 12, yOffset + rowHeight + 1, 0xFFD6C4A3);

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

            if (bottomLine == null) {
                String displayText = split.hasIcon() ? split.leadingIcon + " " + topLine : topLine;
                context.drawString(font, displayText, nameX, nameY, 0x3A2F23, false);
            } else {
                String displayTop = split.hasIcon() ? split.leadingIcon + " " + topLine : topLine;
                context.drawString(font, displayTop, nameX, yOffset + 4, 0x3A2F23, false);
                context.drawString(font, bottomLine, nameX, yOffset + 12, 0x6D5C48, false);
            }

            String valueStr;
            if (stat.displayMode().isPercent()) {
                valueStr = String.format("%d%%", (int) (stat.current() * 100));
            } else if (stat.displayMode().isMultiplier()) {
                valueStr = String.format("%.2fx", stat.current());
            } else {
                valueStr = String.format("%.2f", stat.current());
            }
            int color = stat.isChanged() ? (stat.current() > stat.base() ? 0x55FF55 : 0xFF5555) : 0xAAAAAA;
            valueStr += stat.isChanged() ? (stat.current() > stat.base() ? " ↑" : " ↓") : "";

            context.drawString(font, valueStr, valueX - font.width(valueStr), nameY, color, false);

            if (mouseX >= root.left() && mouseX <= root.left() + root.panelWidth() && mouseY >= yOffset && mouseY <= yOffset + rowHeight) {
                hoverIndex = i;
            }
        }

        int infoX = root.left() + root.panelWidth() - INFO_ICON_SIZE - 95;
        int infoY = root.top() + 10;

        context.blit(INFO_ICON, infoX, infoY,
                0, 0, INFO_ICON_SIZE, INFO_ICON_SIZE,
                INFO_ICON_SIZE, INFO_ICON_SIZE);

        if (mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE
                && mouseY >= infoY && mouseY <= infoY + INFO_ICON_SIZE) {
            drawGlobalBonusTooltip(mouseX, mouseY);
        }
        drawBookTooltipButton(context, font, hoverIndex, mouseX, mouseY, rowHeight, padding);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        int btnY = root.top() + root.panelHeight() - 20;
        ButtonCoords coords = getBookButtonCoords(root.mc().font);

        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, coords.prevX(), btnY, coords.prevW(), 10)) {
            if (root.getCurrentPage() > 0) root.setCurrentPage(root.getCurrentPage() - 1);
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            return true;
        }
        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, coords.nextX(), btnY, coords.nextW(), 10)) {
            int totalPages = (int) Math.ceil(root.getCachedStats().size() / (float) AttributePanelDrawable.MAX_ROWS);
            if (root.getCurrentPage() < totalPages - 1) root.setCurrentPage(root.getCurrentPage() + 1);
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            return true;
        }
        if (AttributePanelDrawable.mouseIn((int) mouseX, (int) mouseY, coords.checkX(), btnY, coords.checkW(), 10)) {
            root.setShowOnlyChanged(! root.getShowOnlyChanged());
            root.setCurrentPage(0);
            root.cacheStats();
            if (root.mc().player != null) root.mc().player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            return true;
        }

        return false;
    }

    private record ButtonCoords(int prevX, int checkX, int nextX, int prevW, int checkW, int nextW) {}

    private ButtonCoords getBookButtonCoords(Font font) {
        String prevText = "««";
        String nextText = "»»";
        String checkLabel = "[ " + (root.getShowOnlyChanged() ? "✓" : " ") + " ]";

        int spacing = 24;

        int prevW = font.width(prevText);
        int checkW = font.width(checkLabel);
        int nextW = font.width(nextText);

        int totalWidth = prevW + spacing + checkW + spacing + nextW;
        int startX = root.left() + (root.panelWidth() - totalWidth) / 2;

        int prevX = startX;
        int checkX = prevX + prevW + spacing;
        int nextX = checkX + checkW + spacing;

        return new ButtonCoords(prevX, checkX, nextX, prevW, checkW, nextW);
    }

    private void drawBookButtons(GuiGraphics context, Font font, int mouseX, int mouseY, int btnY) {
        String prevText = "««";
        String nextText = "»»";
        String checkLabel = "[ " + (root.getShowOnlyChanged() ? "✓" : " ") + " ]";

        int spacing = 24;
        int buttonHeight = 10;

        int prevWidth = font.width(prevText);
        int checkWidth = font.width(checkLabel);
        int nextWidth = font.width(nextText);

        int totalWidth = prevWidth + spacing + checkWidth + spacing + nextWidth;
        int startX = root.left() + (root.panelWidth() - totalWidth) / 2;

        int prevX = startX;
        int checkX = prevX + prevWidth + spacing;
        int nextX = checkX + checkWidth + spacing;

        context.drawString(font, prevText, prevX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, prevX, btnY, prevWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);
        context.drawString(font, checkLabel, checkX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, checkX, btnY, checkWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);
        context.drawString(font, nextText, nextX, btnY,
                AttributePanelDrawable.mouseIn(mouseX, mouseY, nextX, btnY, nextWidth, buttonHeight) ? 0x3A2F23 : 0xAAAAAA, false);
    }

    private void drawBookTooltipButton(GuiGraphics context, Font font, int hoverIndex, int mouseX, int mouseY, int rowHeight, int padding) {
        if (hoverIndex >= 0 && hoverIndex < root.getCachedStats().size()) {
            StatEntry stat = root.getCachedStats().get(hoverIndex);
            showCalculationTooltip(stat, mouseX, mouseY);
        }
        int btnY = root.top() + root.panelHeight() - 20;
        drawBookButtons(context, font, mouseX, mouseY, btnY);
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

        List<Component> lines = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();
        List<ResourceLocation> texIcons = new ArrayList<>();

        lines.add(Component.translatable("attributepanel.tooltip.base", String.format("%.2f", stat.base())));
        icons.add(ItemStack.EMPTY);
        texIcons.add(null);

        if (instance != null) {
            lines.add(Component.translatable("attributepanel.tooltip.final", String.format("%.2f", instance.getValue())));
            icons.add(ItemStack.EMPTY);
            texIcons.add(null);
        }

        double flat = 0.0, multBase = 0.0, multTotal = 0.0;
        List<Double> flatParts = new ArrayList<>();
        List<Double> baseMultParts = new ArrayList<>();
        List<Double> totalMultParts = new ArrayList<>();

        double puffFlat = 0.0, puffBase = 0.0, puffTotal = 0.0;
        boolean hasPuffish = false;

        var unmatchedAccessorySources = new ArrayList<Object>();
        if (TrinketsCompat.isLoaded()) {
            unmatchedAccessorySources.addAll(TrinketsCompat.getTrinketModifierSources(player));
        }
        if (CuriosCompat.isLoaded()) {
            unmatchedAccessorySources.addAll(CuriosCompat.getCurioModifierSources(player));
        }

        if (instance != null && ! instance.getModifiers().isEmpty()) {
            lines.add(Component.empty());
            icons.add(ItemStack.EMPTY);
            texIcons.add(null);
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
                        color = p >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
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
                        opText = "?";
                    }
                }

                String fullPath = rawId.getPath();
                String[] idParts = fullPath.split("\\.", 2);
                ResourceLocation modId = ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), idParts[0]);
                String customName = (idParts.length > 1) ? idParts[1] : null;
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

                try {
                    String lowerPath = fullPath.toLowerCase(Locale.ROOT);
                    if (lowerPath.startsWith("enchantment.") || lowerPath.startsWith("enchantment/")
                            || lowerPath.contains("enchantment")) {
                        String possible = fullPath;
                        int idxDot = fullPath.indexOf('.');
                        int idxSlash = fullPath.indexOf('/');
                        if (idxDot >= 0 && fullPath.startsWith("enchantment.")) {
                            possible = fullPath.substring("enchantment.".length());
                        } else if (idxSlash >= 0 && fullPath.startsWith("enchantment/")) {
                            possible = fullPath.substring("enchantment/".length());
                        } else {
                            if (fullPath.startsWith("enchantment")) {
                                int sep = Math.max(fullPath.indexOf('.'), fullPath.indexOf('/'));
                                if (sep >= 0 && sep + 1 < fullPath.length()) possible = fullPath.substring(sep + 1);
                            }
                        }

                        int stop = possible.length();
                        int s1 = possible.indexOf('/');
                        int s2 = possible.indexOf('.');
                        if (s1 >= 0) stop = Math.min(stop, s1);
                        if (s2 >= 0) stop = Math.min(stop, s2);
                        String enchKey = (stop > 0 && stop <= possible.length()) ? possible.substring(0, stop) : possible;

                        if (enchKey != null && ! enchKey.isBlank() && root.mc().player != null && root.mc().player.level() != null) {
                            ResourceLocation enchId = ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), enchKey);

                            var drm = root.mc().player.level().registryAccess();
                            Registry<Enchantment> enchantmentRegistry = drm.registryOrThrow(Registries.ENCHANTMENT);

                            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchId);
                            var entryOpt = enchantmentRegistry.getHolder(key);

                            if (entryOpt.isPresent()) {
                                var enchEntry = entryOpt.get();
                                int lvl = Math.max(1, (int) Math.round(Math.abs(mod.amount())));
                                iconStack = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchEntry, lvl));
                                Enchantment enchVal = enchEntry.value();
                                displayName = Enchantment.getFullname(enchEntry, lvl).copy().withStyle(ChatFormatting.AQUA);
                                foundSource = true;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                if (foundSource) {
                    lines.add(displayName.copy().append(" ").append(Component.literal(opText).withStyle(color)));
                    icons.add(iconStack);
                    texIcons.add(texIcon);
                    continue;
                }

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = player.getSlot(slot.getIndex()).get();
                    if (stack.isEmpty()) continue;

                    final boolean[] matched = {false};
                    var comp = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                    if (comp != null) {
                        for (var entry : comp.modifiers()) {
                            if (entry.slot().test(slot) &&
                                    entry.modifier().id().equals(mod.id()) &&
                                    entry.attribute().value().equals(stat.attribute().value())) {
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
                                    entry.attribute().value().equals(stat.attribute().value())) {
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
                        break;
                    }
                }

                if (!foundSource) {
                    String[] pathParts = rawId.getPath().split("\\.", 2)[0].split("/");
                    if (pathParts.length > 0) {
                        ResourceLocation guess = ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), pathParts[pathParts.length - 1]);
                        if (BuiltInRegistries.ITEM.containsKey(guess)) {
                            Item item = BuiltInRegistries.ITEM.get(guess);
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
                            List<EquipmentSet.SourcedItemStack> sourced = new ArrayList<>();

                            for (EquipmentSlot slot :  EquipmentSlot.values()) {
                                ItemStack s = player.getItemBySlot(slot);
                                if (s != null && !s.isEmpty()) {
                                    sourced.add(new EquipmentSet.SourcedItemStack(s, slot.getName()));
                                }
                            }

                            if (TrinketsCompat.isLoaded()) {
                                try {
                                    for (var src : TrinketsCompat.getTrinketModifierSources(player)) {
                                        sourced.add(new EquipmentSet.SourcedItemStack(src.stack(), "trinket"));
                                    }
                                } catch (Exception ignored) {}
                            }

                            if (CuriosCompat.isLoaded()) {
                                try {
                                    for (var src : CuriosCompat.getCurioModifierSources(player)) {
                                        sourced.add(new EquipmentSet.SourcedItemStack(src.stack(), "curio"));
                                    }
                                } catch (Exception ignored) {}
                            }

                            var results = EquipmentSet.collectFrom(sourced, player.level());

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
                                                String tkey = EquipmentSet.translationKey(setEntry);
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

                if (customName != null && !customName.isBlank()) {
                    Set<String> ignoredArmorNames = Set.of("helmet", "chestplate", "leggings", "boots");
                    if (! ignoredArmorNames.contains(customName.toLowerCase(Locale.ROOT))) {
                        String pretty = Arrays.stream(customName.split("_"))
                                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                                .collect(Collectors.joining(" "));
                        displayName = Component.literal(pretty).withStyle(ChatFormatting.LIGHT_PURPLE);
                        usedCustomName = true;
                    }
                }

                if (! foundSource) {
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
                    lines.add(Component.literal(tier + " Bonus:  ").withStyle(ChatFormatting.AQUA)
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
                lines.add(Component.empty());
                icons.add(ItemStack.EMPTY);
                texIcons.add(null);
            }
        }

        if (stat.isChanged()) {
            lines.add(Component.empty());
            icons.add(ItemStack.EMPTY);
            texIcons.add(null);

            if (shiftDown) {
                double base = stat.base();
                double basePlusAdd = base + flat;
                double afterBaseMult = (multBase != 0.0) ? basePlusAdd * (1.0 + multBase) : basePlusAdd;
                double finalValue = (multTotal != 0.0) ? afterBaseMult * (1.0 + multTotal) : afterBaseMult;

                lines.add(Component.translatable("attributepanel.tooltip.calculated").withStyle(ChatFormatting.DARK_GRAY));
                icons.add(ItemStack.EMPTY);
                texIcons.add(null);

                if (! flatParts.isEmpty()) {
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

                if (instance != null) {
                    double actual = instance.getValue();
                    double delta = actual - finalValue;
                    if (Math.abs(delta) > 0.001 && finalValue > 0.001) {
                        double pct = Math.abs(delta) / finalValue;
                        lines.add(Component.empty());
                        icons.add(ItemStack.EMPTY);
                        texIcons.add(null);
                        if (delta > 0) {
                            lines.add(Component.translatable("attributepanel.tooltip.indirect_bonus", String.format("%.2f", delta)).withStyle(ChatFormatting.DARK_GREEN));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                            lines.add(Component.literal(String.format("⟶ %.2f × (1.00 + %.2f) = %.2f", finalValue, pct, actual)).withStyle(ChatFormatting.GRAY));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                        } else {
                            lines.add(Component.translatable("attributepanel.tooltip.indirect_decrease", String.format("%.2f", -delta)).withStyle(ChatFormatting.RED));
                            icons.add(ItemStack.EMPTY);
                            texIcons.add(null);
                            lines.add(Component.literal(String.format("⟶ %.2f × (1.00 - %.2f) = %.2f", finalValue, pct, actual)).withStyle(ChatFormatting.GRAY));
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

        root.enqueueTooltipRich(lines, icons, texIcons, mouseX, mouseY);
    }

    private void drawGlobalBonusTooltip(int mouseX, int mouseY) {
        Player player = root.mc().player;
        if (player == null) return;

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
                    case ADD_MULTIPLIED_TOTAL -> totalMultMap.merge(attrName, value, Double::sum);
                }
            }
        }

        List<Component> lines = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();

        lines.add(Component.translatable("attributepanel.message.bonuses").withStyle(ChatFormatting.GOLD));
        icons.add(ItemStack.EMPTY);

        boolean addedAny = false;

        for (String attr : flatMap.keySet()) {
            double value = flatMap.get(attr);
            lines.add(Component.literal(String.format("- %s: %+,.2f", attr, value)).withStyle(ChatFormatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }
        for (String attr : baseMultMap.keySet()) {
            double value = baseMultMap.get(attr);
            lines.add(Component.literal(String.format("- %s: %+d%% Base", attr, (int) (value * 100))).withStyle(ChatFormatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }
        for (String attr :  totalMultMap.keySet()) {
            double value = totalMultMap.get(attr);
            lines.add(Component.literal(String.format("- %s: %+d%% Total", attr, (int) (value * 100))).withStyle(ChatFormatting.GREEN));
            icons.add(ItemStack.EMPTY);
            addedAny = true;
        }

        if (! addedAny) {
            lines.add(Component.literal("No active modifiers").withStyle(ChatFormatting.GRAY));
            icons.add(ItemStack.EMPTY);
        }

        root.enqueueTooltip(lines, icons, mouseX, mouseY);
    }
}