package net.pixeldreamstudios.attributepanel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class AttributesPanelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/attributespanel.json");

    public int xOffset = -61;
    public int yOffset = 10;
    public int panelOffsetX = -130;
    public int panelOffsetY = 0;

    public GuiStyle guiStyle = GuiStyle.COMPACT;

    public boolean hoverIconAnimation = true;

    public List<String> percentAttributes =
            new ArrayList<>(List.of("kevslibrary:armor_penetration"));
    public List<String> percentAttributesBase100 =
            new ArrayList<>(List.of(
                    "spell_power:critical_damage",
                    "spell_power:critical_chance",
                    "spell_power:haste",
                    "ranged_weapon:haste",
                    "spell_engine:damage_taken",
                    "spell_engine:healing_taken",
                    "spell_engine:evasion_chance",
                    "critical_strike:damage",
                    "critical_strike:chance"
            ));
    public List<String> percentKeywords = new ArrayList<>(List.of(
            "chance", "movement_speed"
    ));
    public List<String> percentBase100Keywords = new ArrayList<>();
    public boolean forceTrackVisibleAttributes = true;
    public CompactSettings compact = CompactSettings.defaultPreset();

    public static AttributesPanelConfig INSTANCE = new AttributesPanelConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, AttributesPanelConfig.class);

                if (INSTANCE == null) {
                    INSTANCE = new AttributesPanelConfig();
                }
                if (INSTANCE.compact == null) {
                    INSTANCE.compact = CompactSettings.defaultPreset();
                }
                return;
            } catch (Exception e) {
                System.err.println("[Kev's Attributes Panel] Failed to read config: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (INSTANCE == null) {
            INSTANCE = new AttributesPanelConfig();
        }
        if (INSTANCE.compact == null) {
            INSTANCE.compact = CompactSettings.defaultPreset();
        }

        save();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            System.err.println("[Kev's Attributes Panel] Failed to write config: " + e.getMessage());
        }
    }

    public static void apply() {
        System.out.println("[Kev's Attributes Panel] Config applied: xOffset=" + INSTANCE.xOffset
                + ", yOffset=" + INSTANCE.yOffset
                + ", panelOffsetX=" + INSTANCE.panelOffsetX
                + ", panelOffsetY=" + INSTANCE.panelOffsetY
                + ", GUI=" + INSTANCE.guiStyle
                + ", hoverIconAnimation=" + INSTANCE.hoverIconAnimation);
    }

    public enum GuiStyle {
        BOOK,
        VANILLA,
        COMPACT
    }

    public enum TextTheme {
        LIGHT,
        DARK
    }

    public static class CompactSettings {
        public List<HeaderDef> headers = new ArrayList<>();
        public String otherHeaderName = "Other";
        public String otherHeaderIcon = null;
        public List<String> globalBlacklist = new ArrayList<>();

        public boolean disableOtherHeader = false;
        public boolean disableBonusesHeader = false;
        public String bonusesHeaderName = "Bonuses";
        public String bonusesHeaderIcon = null;

        public int sidePadding = 15;
        public float textScale = 0.55f;

        public TextTheme textTheme = TextTheme.DARK;

        public static CompactSettings defaultPreset() {
            CompactSettings s = new CompactSettings();

            HeaderDef offensive = new HeaderDef();
            offensive.header = "Offensive";
            offensive.attributes.add(AttributeSpec.of("minecraft:generic.attack_damage", null));
            offensive.attributes.add(AttributeSpec.of("minecraft:generic.attack_speed",  null));
            offensive.attributes.add(AttributeSpec.of("ranged_weapon:damage",            null));
            offensive.attributes.add(AttributeSpec.of("ranged_weapon:haste",             null));
            offensive.attributes.add(AttributeSpec.of("critical_strike:chance",             null));
            offensive.attributes.add(AttributeSpec.of("critical_strike:damage",              null));
            s.headers.add(offensive);

            HeaderDef defensive = new HeaderDef();
            defensive.header = "Defensive";
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.armor",                 null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.armor_toughness",      null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.max_health",           null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.movement_speed",       null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.luck",                 null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.knockback_resistance", null));
            defensive.attributes.add(AttributeSpec.of("spell_engine:evasion_chance",         null));
            s.headers.add(defensive);

            HeaderDef magic = new HeaderDef();
            magic.header = "Magic";
            magic.attributes.add(AttributeSpec.of("spell_power:generic",          null));
            magic.attributes.add(AttributeSpec.of("spell_power:fire",             null));
            magic.attributes.add(AttributeSpec.of("spell_power:frost",            null));
            magic.attributes.add(AttributeSpec.of("spell_power:arcane",           null));
            magic.attributes.add(AttributeSpec.of("spell_power:lightning",        null));
            magic.attributes.add(AttributeSpec.of("spell_power:soul",             null));
            magic.attributes.add(AttributeSpec.of("spell_power:healing",          null));
            magic.attributes.add(AttributeSpec.of("spell_power:critical_damage",  null));
            magic.attributes.add(AttributeSpec.of("spell_power:critical_chance",  null));
            magic.attributes.add(AttributeSpec.of("spell_power:haste",            null));
            s.headers.add(magic);

            s.otherHeaderName = "Misc";
            s.otherHeaderIcon = null;
            s.bonusesHeaderName = "Bonuses";
            s.bonusesHeaderIcon = null;
            s.globalBlacklist = new ArrayList<>();

            s.disableOtherHeader = false;
            s.disableBonusesHeader = false;
            s.sidePadding = 15;
            s.textScale = 0.55f;

            s.textTheme = TextTheme.DARK;

            return s;
        }
    }

    public static class HeaderDef {
        public String header = "Header";
        public List<AttributeSpec> attributes = new ArrayList<>();
        public List<String> blacklist = new ArrayList<>();
    }

    public static class AttributeSpec {
        public String id;
        public String icon;
        public static AttributeSpec of(String id, String icon) {
            AttributeSpec s = new AttributeSpec();
            s.id = id;
            s.icon = icon;
            return s;
        }
    }
}
