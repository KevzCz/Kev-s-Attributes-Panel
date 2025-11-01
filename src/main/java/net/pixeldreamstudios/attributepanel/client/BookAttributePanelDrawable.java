package net.pixeldreamstudios.attributepanel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.attributepanel.compat.TrinketCompat;

import java.util.*;
import java.util.stream.Collectors;

import static net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable.formatModifierId;

@Environment(EnvType.CLIENT)
class BookAttributePanelDrawable {
    private final AttributePanelDrawable root;

    private static final Identifier BOOK_TEXTURE = Identifier.of("minecraft", "textures/gui/book.png");
    private static final Identifier INFO_ICON   = Identifier.of("kevs-attributes-panel", "textures/gui/attribute_book.png");
    private static final int INFO_ICON_SIZE = 8;
    private static final Identifier DD_POWER_ICON = Identifier.of("dungeon_difficulty", "textures/symbol/power_level.png");
    private static final Identifier FTB_QUEST_BOOK_ICON = Identifier.of("ftbquests", "textures/item/book.png");
    private static final Identifier PERMA    = Identifier.of("kevs-attributes-panel", "textures/gui/perma.png");
    private static final Identifier EQUIPPED = Identifier.of("kevs-attributes-panel", "textures/gui/equipped.png");

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
        if (hoverIndex >= 0 && hoverIndex < root.getCachedStats().size()) {
            StatEntry stat = root.getCachedStats().get(hoverIndex);
            showCalculationTooltip(stat, mouseX, mouseY);
        }
        int btnY = root.top() + root.panelHeight() - 20;
        drawBookButtons(context, tr, mouseX, mouseY, btnY);
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
        List<Identifier> texIcons = new ArrayList<>();

        lines.add(Text.translatable("attributepanel.tooltip.base", String.format("%.2f", stat.base())));
        icons.add(ItemStack.EMPTY); texIcons.add(null);
        if (instance != null) {
            lines.add(Text.translatable("attributepanel.tooltip.final", String.format("%.2f", instance.getValue())));
            icons.add(ItemStack.EMPTY); texIcons.add(null);
        }

        double flat = 0.0, multBase = 0.0, multTotal = 0.0;
        List<Double> flatParts = new ArrayList<>();
        List<Double> baseMultParts = new ArrayList<>();
        List<Double> totalMultParts = new ArrayList<>();

        double puffFlat = 0.0, puffBase = 0.0, puffTotal = 0.0;
        boolean hasPuffish = false;

        var unmatchedTrinketSources = new ArrayList<TrinketCompat.TrinketModifierSource>();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            unmatchedTrinketSources.addAll(TrinketCompat.getTrinketModifierSources(player));
        }

        if (instance != null && !instance.getModifiers().isEmpty()) {
            lines.add(Text.empty()); icons.add(ItemStack.EMPTY); texIcons.add(null);
            lines.add(Text.translatable("attributepanel.message.modifiers").formatted(Formatting.YELLOW));
            icons.add(ItemStack.EMPTY); texIcons.add(null);

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
                        double v = mod.value(); flat += v; flatParts.add(v);
                        color = v >= 0 ? Formatting.GREEN : Formatting.RED;
                        opText = (v >= 0 ? "+" : "") + String.format("%.2f", v);
                    }
                    case ADD_MULTIPLIED_BASE -> {
                        double v = mod.value(); multBase += v; baseMultParts.add(v);
                        int p = (int) Math.round(v * 100);
                        color = p >= 0 ? Formatting.GREEN : Formatting.RED;
                        opText = (p >= 0 ? "+" : "") + p + "% Base";
                    }
                    case ADD_MULTIPLIED_TOTAL -> {
                        double v = mod.value(); multTotal += v; totalMultParts.add(v);
                        int p = (int) Math.round(v * 100);
                        color = p >= 0 ? Formatting.GREEN : Formatting.RED;
                        opText = (p >= 0 ? "+" : "") + p + "% Total";
                    }
                    default -> { color = Formatting.GRAY; opText = "?"; }
                }

                String fullPath = rawId.getPath();
                String[] idParts = fullPath.split("\\.", 2);
                Identifier modId = Identifier.of(rawId.getNamespace(), idParts[0]);
                String customName = (idParts.length > 1) ? idParts[1] : null;
                boolean usedCustomName = false;

                Text displayName = Text.literal(formatModifierId(modId));
                ItemStack iconStack = ItemStack.EMPTY;
                boolean foundSource = false;

                if ("dungeon_difficulty".equals(rawId.getNamespace())) {
                    displayName = Text.literal("Power Boost").formatted(Formatting.AQUA);
                    lines.add(displayName.copy().append(" ").append(Text.literal(opText).formatted(color)));
                    icons.add(ItemStack.EMPTY);
                    texIcons.add(DD_POWER_ICON);
                    continue;
                }
                if ("morequesttypes".equals(rawId.getNamespace())) {
                    displayName = Text.literal("Quest Reward").formatted(Formatting.AQUA);
                    lines.add(displayName.copy().append(" ").append(Text.literal(opText).formatted(color)));
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

                        if (enchKey != null && !enchKey.isBlank() && root.mc().player != null && root.mc().player.getWorld() != null) {
                            Identifier enchId = Identifier.of(rawId.getNamespace(), enchKey);

                            var drm = root.mc().player.getWorld().getRegistryManager();
                            Registry<net.minecraft.enchantment.Enchantment> enchantmentRegistry = drm.get(RegistryKeys.ENCHANTMENT);

                            RegistryKey<net.minecraft.enchantment.Enchantment> key = RegistryKey.of(RegistryKeys.ENCHANTMENT, enchId);
                            var entryOpt = enchantmentRegistry.getEntry(key);

                            if (entryOpt.isPresent()) {
                                var enchEntry = entryOpt.get();
                                int lvl = Math.max(1, (int) Math.round(Math.abs(mod.value())));
                                iconStack = EnchantedBookItem.forEnchantment(new net.minecraft.enchantment.EnchantmentLevelEntry(enchEntry, lvl));
                                net.minecraft.enchantment.Enchantment enchVal = enchEntry.value();
                                displayName = enchVal.description().copy().formatted(Formatting.AQUA);
                                foundSource = true;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (foundSource) {
                    lines.add(displayName.copy().append(" ").append(Text.literal(opText).formatted(color)));
                    icons.add(iconStack); texIcons.add(null);
                    continue;
                }

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
                    String[] pathParts = rawId.getPath().split("\\.", 2)[0].split("/");
                    if (pathParts.length > 0) {
                        Identifier guess = Identifier.of(rawId.getNamespace(), pathParts[pathParts.length - 1]);
                        if (Registries.ITEM.containsId(guess)) {
                            net.minecraft.item.Item item = Registries.ITEM.get(guess);
                            iconStack = new ItemStack(item);
                            displayName = iconStack.getName().copy().formatted(iconStack.getRarity().getFormatting());
                            foundSource = true;
                        }
                    }
                }

                if (!foundSource) {
                    try {
                        String rawPath = rawId.getPath().toLowerCase(Locale.ROOT);
                        if (rawPath.contains("set_bonus")) {
                            List<net.spell_engine.api.item.set.EquipmentSet.SourcedItemStack> sourced = new ArrayList<>();

                            for (EquipmentSlot slot : EquipmentSlot.values()) {
                                ItemStack s = player.getEquippedStack(slot);
                                if (s != null && !s.isEmpty()) {
                                    sourced.add(new net.spell_engine.api.item.set.EquipmentSet.SourcedItemStack(s, slot.getName()));
                                }
                            }

                            if (FabricLoader.getInstance().isModLoaded("trinkets")) {
                                try {
                                    for (var src : TrinketCompat.getTrinketModifierSources(player)) {
                                        sourced.add(new net.spell_engine.api.item.set.EquipmentSet.SourcedItemStack(src.stack(), "trinket"));
                                    }
                                } catch (Exception ignored) {}
                            }

                            var results = net.spell_engine.api.item.set.EquipmentSet.collectFrom(sourced, player.getWorld());

                            for (var res : results) {
                                var setEntry = res.set();
                                if (setEntry.getKey().isPresent()) {
                                    Identifier setId = setEntry.getKey().get().getValue();
                                    if (setId.getNamespace().equals(rawId.getNamespace())) {
                                        List<ItemStack> setItems = res.items();
                                        if (setItems != null && !setItems.isEmpty()) {
                                            int idx = (int) ((System.currentTimeMillis() / 1000L) % setItems.size());
                                            ItemStack chosen = setItems.get(idx);
                                            iconStack = chosen;

                                            Text setNameText;
                                            try {
                                                String tkey = net.spell_engine.api.item.set.EquipmentSet.translationKey(setEntry);
                                                setNameText = Text.translatable(tkey);
                                                if (setNameText.getString().equals(tkey)) {
                                                    String rawDefName = setEntry.value().name();
                                                    if (rawDefName != null && !rawDefName.isBlank()) {
                                                        setNameText = Text.literal(rawDefName);
                                                    } else {
                                                        setNameText = Text.literal(setId.getPath());
                                                    }
                                                }
                                            } catch (Exception e) {
                                                String rawDefName = "";
                                                try { rawDefName = setEntry.value().name(); } catch (Exception ignored) {}
                                                if (rawDefName != null && !rawDefName.isBlank()) {
                                                    setNameText = Text.literal(rawDefName);
                                                } else {
                                                    setNameText = Text.literal(setId.getPath());
                                                }
                                            }

                                            displayName = setNameText.copy().formatted(Formatting.AQUA)
                                                    .append(Text.literal(" Set").formatted(Formatting.AQUA));

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

                        MutableText line = Text.literal(prettyTitle).formatted(Formatting.GOLD)
                                .append(Text.literal(" ").append(Text.literal(opText).formatted(color)));

                        lines.add(line);
                        icons.add(ItemStack.EMPTY);
                        texIcons.add(perma ? PERMA : EQUIPPED);

                        printedCustom = true;
                    }
                }

                if (printedCustom) {
                    continue;
                } else if (rawId.getNamespace().equals("tiered")) {
                    String[] pp = rawId.getPath().split("_");
                    String tier = pp.length>0 ? (pp[0].substring(0,1).toUpperCase()+pp[0].substring(1).toLowerCase()) : "Tiered";
                    lines.add(Text.literal(tier+" Bonus: ").formatted(Formatting.AQUA)
                            .append(Text.literal(opText).formatted(Formatting.GREEN)));
                    icons.add(new ItemStack(Items.ANVIL)); texIcons.add(null);
                } else {
                    lines.add(displayName.copy().append(" ").append(Text.literal(opText).formatted(color)));
                    icons.add(iconStack); texIcons.add(null);
                }
            }

            if (hasPuffish) {
                lines.add(Text.translatable("attributepanel.tooltip.skill_tree_bonus").formatted(Formatting.AQUA));
                icons.add(ItemStack.EMPTY); texIcons.add(null);
                if (puffFlat != 0.0)  { lines.add(Text.literal(String.format("- %+,.2f", puffFlat)).formatted(Formatting.GREEN));  icons.add(ItemStack.EMPTY); texIcons.add(null); }
                if (puffBase != 0.0)  { lines.add(Text.literal(String.format("- %+d%% Base",  (int)(puffBase*100))).formatted(Formatting.GREEN)); icons.add(ItemStack.EMPTY); texIcons.add(null); }
                if (puffTotal != 0.0) { lines.add(Text.literal(String.format("- %+d%% Total", (int)(puffTotal*100))).formatted(Formatting.GREEN)); icons.add(ItemStack.EMPTY); texIcons.add(null); }
                lines.add(Text.empty()); icons.add(ItemStack.EMPTY); texIcons.add(null);
            }
        }

        if (stat.isChanged()) {
            lines.add(Text.empty()); icons.add(ItemStack.EMPTY); texIcons.add(null);

            if (shiftDown) {
                double base = stat.base();
                double basePlusAdd = base + flat;
                double afterBaseMult = (multBase!=0.0) ? basePlusAdd * (1.0+multBase) : basePlusAdd;
                double finalValue = (multTotal!=0.0) ? afterBaseMult * (1.0+multTotal) : afterBaseMult;

                lines.add(Text.translatable("attributepanel.tooltip.calculated").formatted(Formatting.DARK_GRAY)); icons.add(ItemStack.EMPTY); texIcons.add(null);

                if (!flatParts.isEmpty()) {
                    String sum = flatParts.stream().map(v->String.format("%.2f",v)).reduce((a,b)->a+" + "+b).orElse("0.00");
                    lines.add(Text.literal(String.format("⟶ %.2f + (%s) = %.2f", base, sum, basePlusAdd)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                } else {
                    lines.add(Text.literal(String.format("= %.2f", base)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                }

                if (!baseMultParts.isEmpty()) {
                    String sum = baseMultParts.stream().map(v->String.format("%.2f",v)).reduce((a,b)->a+" + "+b).orElse("0.00");
                    lines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", basePlusAdd, sum, afterBaseMult)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                }
                if (!totalMultParts.isEmpty()) {
                    String sum = totalMultParts.stream().map(v->String.format("%.2f",v)).reduce((a,b)->a+" + "+b).orElse("0.00");
                    lines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %s) = %.2f", afterBaseMult, sum, finalValue)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                }

                lines.add(Text.literal("= " + String.format("%.2f", finalValue)).formatted(Formatting.GREEN)); icons.add(ItemStack.EMPTY); texIcons.add(null);

                if (instance != null) {
                    double actual = instance.getValue();
                    double delta = actual - finalValue;
                    if (Math.abs(delta) > 0.001 && finalValue > 0.001) {
                        double pct = Math.abs(delta)/finalValue;
                        lines.add(Text.empty()); icons.add(ItemStack.EMPTY); texIcons.add(null);
                        if (delta > 0) {
                            lines.add(Text.translatable("attributepanel.tooltip.indirect_bonus", String.format("%.2f", delta)).formatted(Formatting.DARK_GREEN)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                            lines.add(Text.literal(String.format("⟶ %.2f × (1.00 + %.2f) = %.2f", finalValue, pct, actual)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                        } else {
                            lines.add(Text.translatable("attributepanel.tooltip.indirect_decrease", String.format("%.2f", -delta)).formatted(Formatting.RED)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                            lines.add(Text.literal(String.format("⟶ %.2f × (1.00 - %.2f) = %.2f", finalValue, pct, actual)).formatted(Formatting.GRAY)); icons.add(ItemStack.EMPTY); texIcons.add(null);
                        }
                    }
                }
            } else {
                lines.add(Text.translatable("attributepanel.tooltip.hold_shift").formatted(Formatting.GRAY));
                icons.add(ItemStack.EMPTY); texIcons.add(null);
            }
        }

        root.enqueueTooltipRich(lines, icons, texIcons, mouseX, mouseY);
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
