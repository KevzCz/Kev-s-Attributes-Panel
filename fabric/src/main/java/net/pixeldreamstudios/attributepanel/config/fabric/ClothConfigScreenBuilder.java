package net.pixeldreamstudios.attributepanel.config.fabric;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Environment(EnvType.CLIENT)
public class ClothConfigScreenBuilder {

    public static Screen buildConfigScreen(Screen parent) {
        return buildScreen(parent, deepCopyHeaders(safeSettings().headers));
    }

    private static Screen buildScreen(Screen parent, List<AttributesPanelConfig.HeaderDef> workingHeaders) {
        final var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.attributepanel.title"));
        final var eb = builder.entryBuilder();

        final ConfigCategory cat = builder.getOrCreateCategory(
                Component.translatable("config.attributepanel.category.general"));

        cat.addEntry(eb.startIntField(Component.translatable("config.attributepanel.x_offset"),
                        AttributesPanelConfig.INSTANCE.xOffset)
                .setDefaultValue(-61)
                .setTooltip(Component.translatable("config.attributepanel.x_offset.tooltip"))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.xOffset = v)
                .build());

        cat.addEntry(eb.startIntField(Component.translatable("config.attributepanel.y_offset"),
                        AttributesPanelConfig.INSTANCE.yOffset)
                .setDefaultValue(10)
                .setTooltip(Component.translatable("config.attributepanel.y_offset.tooltip"))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.yOffset = v)
                .build());

        cat.addEntry(eb.startIntField(Component.literal("Panel X offset (relative to inventory)"),
                        AttributesPanelConfig.INSTANCE.panelOffsetX)
                .setDefaultValue(-130)
                .setTooltip(Component.literal("Horizontal offset from the inventory's top-left (negative = left of it)."))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.panelOffsetX = v)
                .build());

        cat.addEntry(eb.startIntField(Component.literal("Panel Y offset (relative to inventory)"),
                        AttributesPanelConfig.INSTANCE.panelOffsetY)
                .setDefaultValue(0)
                .setTooltip(Component.literal("Vertical offset from the inventory's top-left."))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.panelOffsetY = v)
                .build());

        AbstractConfigListEntry<AttributesPanelConfig.GuiStyle> guiStyleEntry =
                eb.startEnumSelector(Component.translatable("config.attributepanel.gui_style"),
                                AttributesPanelConfig.GuiStyle.class,
                                AttributesPanelConfig.INSTANCE.guiStyle)
                        .setDefaultValue(AttributesPanelConfig.GuiStyle.BOOK)
                        .setTooltip(Component.translatable("config.attributepanel.gui_style.tooltip"))
                        .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.guiStyle = v)
                        .build();
        cat.addEntry(guiStyleEntry);

        cat.addEntry(eb.startBooleanToggle(Component.literal("Animate book icon on hover"),
                        AttributesPanelConfig.INSTANCE.hoverIconAnimation)
                .setDefaultValue(true)
                .setTooltip(Component.literal("If disabled, the book icon will only change transparency when hovered."))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.hoverIconAnimation = v)
                .build());

        cat.addEntry(eb.startBooleanToggle(Component.literal("Enable color-coded values"),
                        AttributesPanelConfig.INSTANCE.enableColorCodedValues)
                .setDefaultValue(false)
                .setTooltip(Component.literal("When enabled, attribute values change color based on whether they increase or decrease."))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.enableColorCodedValues = v)
                .build());

        cat.addEntry(eb.startBooleanToggle(Component.literal("Enable glow effects"),
                        AttributesPanelConfig.INSTANCE.enableGlowEffects)
                .setDefaultValue(false)
                .setTooltip(Component.literal("When enabled, values briefly glow when they change."))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.enableGlowEffects = v)
                .build());

        cat.addEntry(eb.startBooleanToggle(Component.literal("Enable smooth value transitions"),
                        AttributesPanelConfig.INSTANCE.enableSmoothValueTransition)
                .setDefaultValue(false)
                .setTooltip(Component.literal("When enabled, values animate with a rolling slot machine effect when they change."))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.enableSmoothValueTransition = v)
                .build());

        cat.addEntry(eb.startIntField(Component.literal("Value transition duration (ms)"),
                        AttributesPanelConfig.INSTANCE.valueTransitionDurationMs)
                .setDefaultValue(300)
                .setTooltip(Component.literal("Duration in milliseconds for value animations."))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.valueTransitionDurationMs = Math.max(50, v))
                .build());

        cat.addEntry(eb.startStrList(
                        Component.literal("Positive when higher (color coding)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.positiveWhenHigher))
                .setTooltip(Component.literal("Attribute IDs that show green when increased, red when decreased.\nExample: minecraft:generic.attack_damage"))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.positiveWhenHigher = list)
                .build());

        cat.addEntry(eb.startStrList(
                        Component.literal("Positive when lower (color coding)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.positiveWhenLower))
                .setTooltip(Component.literal("Attribute IDs that show green when decreased, red when increased.\nExample: minecraft:generic.gravity"))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.positiveWhenLower = list)
                .build());

        cat.addEntry(eb.startStrList(
                        Component.literal("Percent attributes (0..1)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentAttributes))
                .setTooltip(Component.literal("Attribute IDs shown as percentages when values are in the 0..1 range, e.g.0.12 → 12%.\nExample: kevslibrary:multistrike_chance"))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentAttributes = list)
                .build());

        cat.addEntry(eb.startStrList(
                        Component.literal("Percent attributes (start at 100%)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentAttributesBase100))
                .setTooltip(Component.literal("Attribute IDs shown as percentages where 100 = 0% and 200 = 100%, e.g.110 → 10%."))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentAttributesBase100 = list)
                .build());

        cat.addEntry(eb.startStrList(
                        Component.literal("Percent keywords (0..1)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentKeywords))
                .setTooltip(Component.literal("If an attribute ID isn't listed above, match these substrings in the translation key.\nExample: chance, movement_speed"))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentKeywords = list)
                .build());

        cat.addEntry(eb.startStrList(
                        Component.literal("Percent keywords (start at 100%)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentBase100Keywords))
                .setTooltip(Component.literal("If an attribute ID isn't listed above, match these substrings in the translation key (100 = 0%)."))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentBase100Keywords = list)
                .build());

        cat.addEntry(eb.startBooleanToggle(Component.literal("Force track visible attributes"),
                        AttributesPanelConfig.INSTANCE.forceTrackVisibleAttributes)
                .setTooltip(Component.literal("Attributes shown on the panel will be force synced from the server.Requires restart to take effect! "))
                .build());

        final var compactSC = eb.startSubCategory(Component.literal("Compact options"));
        compactSC.setExpanded(true);
        compactSC.setRequirement(() -> guiStyleEntry.getValue() == AttributesPanelConfig.GuiStyle.COMPACT);

        compactSC.add(eb.startStrField(Component.literal("Other header name"),
                        nullToEmpty(safeSettings().otherHeaderName))
                .setDefaultValue("Other")
                .setTooltip(Component.literal("Title used for the automatic group that collects unmatched attributes."))
                .setSaveConsumer(v -> safeSettings().otherHeaderName = emptyToNull(trimOrNull(v)))
                .build());

        compactSC.add(eb.startStrField(Component.literal("Other header icon (optional)"),
                        nullToEmpty(safeSettings().otherHeaderIcon))
                .setTooltip(Component.literal("Texture path like modid:textures/gui/other.png; leave blank for none."))
                .setSaveConsumer(v -> safeSettings().otherHeaderIcon = emptyToNull(trimOrNull(v)))
                .build());

        compactSC.add(eb.startStrField(Component.literal("Bonuses header name"),
                        nullToEmpty(safeSettings().bonusesHeaderName))
                .setDefaultValue("Bonuses")
                .setTooltip(Component.literal("Title used for the group that shows NaN attributes with modifiers."))
                .setSaveConsumer(v -> safeSettings().bonusesHeaderName = emptyToNull(trimOrNull(v)))
                .build());

        compactSC.add(eb.startStrField(Component.literal("Bonuses header icon (optional)"),
                        nullToEmpty(safeSettings().bonusesHeaderIcon))
                .setTooltip(Component.literal("Texture path like modid:textures/gui/bonuses.png; leave blank for none."))
                .setSaveConsumer(v -> safeSettings().bonusesHeaderIcon = emptyToNull(trimOrNull(v)))
                .build());

        compactSC.add(eb.startBooleanToggle(Component.literal("Hide \"Bonuses\" header"),
                        safeSettings().disableBonusesHeader)
                .setDefaultValue(false)
                .setTooltip(Component.literal("If enabled, NaN attributes with modifiers won't be shown in a separate Bonuses group."))
                .setSaveConsumer(v -> safeSettings().disableBonusesHeader = v)
                .build());

        compactSC.add(eb.startStrList(Component.literal("Global blacklist (globs)"),
                        new ArrayList<>(safeSettings().globalBlacklist))
                .setTooltip(Component.literal("Any matching attributes are hidden entirely."))
                .setSaveConsumer(list -> safeSettings().globalBlacklist = list)
                .build());

        compactSC.add(eb.startBooleanToggle(Component.literal("Hide automatic \"Other\" header"),
                        safeSettings().disableOtherHeader)
                .setDefaultValue(false)
                .setTooltip(Component.literal("If enabled, unmatched attributes are listed without the \"Other\" header."))
                .setSaveConsumer(v -> safeSettings().disableOtherHeader = v)
                .build());

        compactSC.add(eb.startIntField(Component.literal("Side padding"),
                        safeSettings().sidePadding)
                .setDefaultValue(15)
                .setTooltip(Component.literal("Horizontal padding between the panel texture and attribute content."))
                .setSaveConsumer(v -> safeSettings().sidePadding = Math.max(0, v))
                .build());

        compactSC.add(eb.startFloatField(Component.literal("Text scale"),
                        safeSettings().textScale)
                .setDefaultValue(0.55f)
                .setTooltip(Component.literal("Scales the attribute value and name text in the compact view."))
                .setSaveConsumer(v -> safeSettings().textScale = Math.max(0.01f, v))
                .build());

        compactSC.add(
                eb.startEnumSelector(
                                Component.literal("Text theme"),
                                AttributesPanelConfig.TextTheme.class,
                                safeSettings().textTheme
                        )
                        .setDefaultValue(AttributesPanelConfig.TextTheme.DARK)
                        .setTooltip(Component.literal("Choose LIGHT for dark text, or DARK for white text in the Compact panel."))
                        .setSaveConsumer(v -> safeSettings().textTheme = v)
                        .build()
        );

        for (int i = 0; i < workingHeaders.size(); i++) {
            final int idx = i;
            final var h = workingHeaders.get(i);
            final List<AbstractConfigListEntry<? >> rows = new ArrayList<>();

            rows.add(eb.startStrField(Component.literal("Header label (Text [path])"), nullToEmpty(h.header))
                    .setTooltip(Component.literal("""
                            Examples:
                            • Offense [modid:textures/gui/offense.png]
                            • [modid:textures/gui/defense.png]
                            • Utility
                            If an icon can't be found at runtime, "[cant find texture]" is shown."""))
                    .setSaveConsumer(v -> h.header = emptyToNull(trimOrNull(v)))
                    .build());

            final List<String> attrRows = h.attributes.stream()
                    .map(a -> isBlank(a.icon) ? a.id : (a.id + " | " + a.icon))
                    .collect(Collectors.toList());

            rows.add(eb.startStrList(Component.literal("Attributes (id or id | icon)"), attrRows)
                    .setTooltip(Component.literal("""
                            Each line: 
                            • Exact id:   minecraft:generic.attack_damage
                            Optional icon after a pipe: 
                            • spell_engine:crit_chance | kevs_attributes_panel:textures/icons/crit.png
                            """))
                    .setSaveConsumer(list -> {
                        h.attributes.clear();
                        for (String row : list) {
                            String r = trimOrNull(row);
                            if (r == null) continue;
                            String id = r;
                            String icon = null;
                            int p = r.indexOf('|');
                            if (p >= 0) {
                                id = r.substring(0, p).trim();
                                icon = trimOrNull(r.substring(p + 1));
                            }
                            AttributesPanelConfig.AttributeSpec spec = new AttributesPanelConfig.AttributeSpec();
                            spec.id = id;
                            spec.icon = icon;
                            h.attributes.add(spec);
                        }
                    })
                    .build());

            rows.add(eb.startStrList(Component.literal("Header blacklist (globs)"), new ArrayList<>(h.blacklist))
                    .setTooltip(Component.literal("Hide matches from this header only."))
                    .setSaveConsumer(list -> h.blacklist = list)
                    .build());

            rows.add(new InlineButtonEntry(
                    Component.literal(""),
                    Component.literal("Remove this header"),
                    () -> {
                        if (idx >= 0 && idx < workingHeaders.size()) {
                            workingHeaders.remove(idx);
                            Minecraft.getInstance().setScreen(buildScreen(parent, workingHeaders));
                        }
                    }
            ));

            final var headerSC = eb.startSubCategory(Component.literal("Header " + (i + 1)));
            headerSC.addAll(rows);
            compactSC.add(headerSC.build());
        }

        compactSC.add(new InlineButtonEntry(
                Component.literal(""),
                Component.literal("Add Header +"),
                () -> {
                    AttributesPanelConfig.HeaderDef def = new AttributesPanelConfig.HeaderDef();
                    def.header = "New Header";
                    workingHeaders.add(def);
                    Minecraft.getInstance().setScreen(buildScreen(parent, workingHeaders));
                }
        ));

        cat.addEntry(compactSC.build());

        builder.setSavingRunnable(() -> {
            AttributesPanelConfig.INSTANCE.compact.headers = workingHeaders;
            AttributesPanelConfig.save();
            AttributesPanelConfig.apply();
        });

        return builder.build();
    }

    static final class InlineButtonEntry extends AbstractConfigListEntry<Void> {
        private final Button button;
        private final Component label;

        InlineButtonEntry(Component label, Component buttonText, Runnable onPress) {
            super(label, false);
            this.label = label;
            this.button = Button.builder(buttonText, b -> onPress.run())
                    .width(120).build();
        }

        @Override public int getItemHeight() { return 24; }
        @Override public Void getValue() { return null; }
        @Override public void save() {}
        @Override public Component getFieldName() { return label; }

        @Override
        public Optional<Void> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public void render(GuiGraphics ctx, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            var tr = Minecraft.getInstance().font;
            if (label != null && !label.getString().isEmpty()) {
                ctx.drawString(tr, label, x, y + (entryHeight - tr.lineHeight) / 2, 0xFFFFFF, false);
            }

            int w = 120, h = entryHeight - 2;
            button.setX(x + entryWidth - w);
            button.setY(y + 1);
            button.setWidth(w);
            button.setHeight(h);
            button.render(ctx, mouseX, mouseY, delta);
        }

        @Override public List<?  extends GuiEventListener> children() { return List.of(button); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(button); }
    }

    private static AttributesPanelConfig.CompactSettings safeSettings() {
        if (AttributesPanelConfig.INSTANCE.compact == null) {
            AttributesPanelConfig.INSTANCE.compact = AttributesPanelConfig.CompactSettings.defaultPreset();
        }
        return AttributesPanelConfig.INSTANCE.compact;
    }

    private static List<AttributesPanelConfig.HeaderDef> deepCopyHeaders(List<AttributesPanelConfig.HeaderDef> in) {
        List<AttributesPanelConfig.HeaderDef> out = new ArrayList<>();
        if (in == null) return out;
        for (AttributesPanelConfig.HeaderDef h : in) {
            AttributesPanelConfig.HeaderDef c = new AttributesPanelConfig.HeaderDef();
            c.header = h.header;
            c.blacklist = new ArrayList<>(h.blacklist == null ? List.of() : h.blacklist);
            c.attributes = new ArrayList<>();
            if (h.attributes != null) {
                for (AttributesPanelConfig.AttributeSpec a : h.attributes) {
                    AttributesPanelConfig.AttributeSpec cc = new AttributesPanelConfig.AttributeSpec();
                    cc.id = a.id;
                    cc.icon = a.icon;
                    c.attributes.add(cc);
                }
            }
            out.add(c);
        }
        return out;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String emptyToNull(String s) { return (s == null || s.isEmpty()) ? null : s; }
}