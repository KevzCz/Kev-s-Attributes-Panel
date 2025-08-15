package net.pixeldreamstudios.attributepanel.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class AttributesPanelModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> buildScreen(parent, deepCopyHeaders(safeSettings().headers));
    }

    private Screen buildScreen(Screen parent, List<AttributesPanelConfig.HeaderDef> workingHeaders) {
        final var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.attributepanel.title"));
        final var eb = builder.entryBuilder();


        final ConfigCategory cat = builder.getOrCreateCategory(
                Text.translatable("config.attributepanel.category.general"));


        cat.addEntry(eb.startIntField(Text.translatable("config.attributepanel.x_offset"),
                        AttributesPanelConfig.INSTANCE.xOffset)
                .setDefaultValue(-61)
                .setTooltip(Text.translatable("config.attributepanel.x_offset.tooltip"))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.xOffset = v)
                .build());

        cat.addEntry(eb.startIntField(Text.translatable("config.attributepanel.y_offset"),
                        AttributesPanelConfig.INSTANCE.yOffset)
                .setDefaultValue(66)
                .setTooltip(Text.translatable("config.attributepanel.y_offset.tooltip"))
                .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.yOffset = v)
                .build());

        AbstractConfigListEntry<AttributesPanelConfig.GuiStyle> guiStyleEntry =
                eb.startEnumSelector(Text.translatable("config.attributepanel.gui_style"),
                                AttributesPanelConfig.GuiStyle.class,
                                AttributesPanelConfig.INSTANCE.guiStyle)
                        .setDefaultValue(AttributesPanelConfig.GuiStyle.BOOK)
                        .setTooltip(Text.translatable("config.attributepanel.gui_style.tooltip"))
                        .setSaveConsumer(v -> AttributesPanelConfig.INSTANCE.guiStyle = v)
                        .build();
        cat.addEntry(guiStyleEntry);
        cat.addEntry(eb.startStrList(
                        Text.literal("Percent attributes (0..1)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentAttributes))
                .setTooltip(Text.literal("Attribute IDs shown as percentages when values are in the 0..1 range, e.g. 0.12 → 12%.\nExample: kevslibrary:multistrike_chance"))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentAttributes = list)
                .build());

        cat.addEntry(eb.startStrList(
                        Text.literal("Percent attributes (start at 100%)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentAttributesBase100))
                .setTooltip(Text.literal("Attribute IDs shown as percentages where 100 = 0% and 200 = 100%, e.g. 110 → 10%."))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentAttributesBase100 = list)
                .build());
        cat.addEntry(eb.startStrList(
                        Text.literal("Percent keywords (0..1)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentKeywords))
                .setTooltip(Text.literal("If an attribute ID isn’t listed above, match these substrings in the translation key.\nExample: chance, movement_speed"))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentKeywords = list)
                .build());

        cat.addEntry(eb.startStrList(
                        Text.literal("Percent keywords (start at 100%)"),
                        new ArrayList<>(AttributesPanelConfig.INSTANCE.percentBase100Keywords))
                .setTooltip(Text.literal("If an attribute ID isn’t listed above, match these substrings in the translation key (100 = 0%)."))
                .setSaveConsumer(list -> AttributesPanelConfig.INSTANCE.percentBase100Keywords = list)
                .build());

        final var compactSC = eb.startSubCategory(Text.literal("Compact options"));
        compactSC.setExpanded(true);
        compactSC.setRequirement(() -> guiStyleEntry.getValue() == AttributesPanelConfig.GuiStyle.COMPACT);

        compactSC.add(eb.startStrField(Text.literal("Other header name"),
                        nullToEmpty(safeSettings().otherHeaderName))
                .setDefaultValue("Other")
                .setTooltip(Text.literal("Title used for the automatic group that collects unmatched attributes."))
                .setSaveConsumer(v -> safeSettings().otherHeaderName = emptyToNull(trimOrNull(v)))
                .build());

        compactSC.add(eb.startStrField(Text.literal("Other header icon (optional)"),
                        nullToEmpty(safeSettings().otherHeaderIcon))
                .setTooltip(Text.literal("Texture path like modid:textures/gui/other.png; leave blank for none."))
                .setSaveConsumer(v -> safeSettings().otherHeaderIcon = emptyToNull(trimOrNull(v)))
                .build());

        compactSC.add(eb.startStrList(Text.literal("Global blacklist (globs)"),
                        new ArrayList<>(safeSettings().globalBlacklist))
                .setTooltip(Text.literal("Any matching attributes are hidden entirely."))
                .setSaveConsumer(list -> safeSettings().globalBlacklist = list)
                .build());

        for (int i = 0; i < workingHeaders.size(); i++) {
            final int idx = i;
            final var h = workingHeaders.get(i);
            final List<AbstractConfigListEntry<?>> rows = new ArrayList<>();

            rows.add(eb.startStrField(Text.literal("Header label (Text [path])"), nullToEmpty(h.header))
                    .setTooltip(Text.literal("""
                            Examples:
                            • Offense [modid:textures/gui/offense.png]
                            • [modid:textures/gui/defense.png]
                            • Utility
                            If an icon can’t be found at runtime, “[cant find texture]” is shown."""))
                    .setSaveConsumer(v -> h.header = emptyToNull(trimOrNull(v)))
                    .build());

            final List<String> attrRows = h.attributes.stream()
                    .map(a -> isBlank(a.icon) ? a.id : (a.id + " | " + a.icon))
                    .collect(Collectors.toList());

            rows.add(eb.startStrList(Text.literal("Attributes (id or id | icon)"), attrRows)
                    .setTooltip(Text.literal("""
                            Each line:
                            • Exact id:  minecraft:generic.attack_damage
                            Optional icon after a pipe:
                            • spell_engine:crit_chance | kevs-attributes-panel:textures/icons/crit.png
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

            rows.add(eb.startStrList(Text.literal("Header blacklist (globs)"), new ArrayList<>(h.blacklist))
                    .setTooltip(Text.literal("Hide matches from this header only."))
                    .setSaveConsumer(list -> h.blacklist = list)
                    .build());

            rows.add(new InlineButtonEntry(
                    Text.literal(""),
                    Text.literal("Remove this header"),
                    () -> {
                        if (idx >= 0 && idx < workingHeaders.size()) {
                            workingHeaders.remove(idx);
                            MinecraftClient.getInstance().setScreen(buildScreen(parent, workingHeaders));
                        }
                    }
            ));

            final var headerSC = eb.startSubCategory(Text.literal("Header " + (i + 1)));
            headerSC.addAll(rows);
            compactSC.add(headerSC.build());
        }

        compactSC.add(new InlineButtonEntry(
                Text.literal(""),
                Text.literal("Add Header +"),
                () -> {
                    AttributesPanelConfig.HeaderDef def = new AttributesPanelConfig.HeaderDef();
                    def.header = "New Header";
                    workingHeaders.add(def);
                    MinecraftClient.getInstance().setScreen(buildScreen(parent, workingHeaders));
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

    /* ---------- Small reusable custom entry: a single inline button ---------- */
    static final class InlineButtonEntry extends AbstractConfigListEntry<Void> {
        private final ButtonWidget button;
        private final Text label;

        InlineButtonEntry(Text label, Text buttonText, Runnable onPress) {
            super(label, false);
            this.label = label;
            this.button = ButtonWidget.builder(buttonText, b -> onPress.run())
                    .width(120).build();
        }

        @Override public int getItemHeight() { return 24; }
        @Override public Void getValue() { return null; }
        @Override public void save() {}
        @Override public Text getFieldName() { return label; }

        @Override
        public Optional<Void> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            var tr = MinecraftClient.getInstance().textRenderer;
            if (label != null && !label.getString().isEmpty()) {
                ctx.drawText(tr, label, x, y + (entryHeight - tr.fontHeight) / 2, 0xFFFFFF, false);
            }

            int w = 120, h = entryHeight - 2;
            button.setX(x + entryWidth - w);
            button.setY(y + 1);
            button.setWidth(w);
            button.setHeight(h);
            button.render(ctx, mouseX, mouseY, delta);
        }

        @Override public List<? extends Element> children() { return List.of(button); }
        @Override public List<? extends Selectable> narratables() { return List.of(button); }
    }

    /* ------------------------ Helpers ------------------------ */

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
