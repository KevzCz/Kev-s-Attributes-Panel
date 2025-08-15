package net.pixeldreamstudios.attributepanel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Global config + compact GUI customization.
 *
 * JSON file: config/attributespanel.json
 *
 * Example compact section:
 * {
 *   "xOffset": -61,
 *   "yOffset": 10,
 *   "guiStyle": "COMPACT",
 *   "compact": {
 *     "headers": [
 *       {
 *         "header": "Offense [kevs-attributes-panel:textures/gui/offense.png]",
 *         "attributes": [
 *           {"id": "minecraft:attack_damage", "icon": "kevs-attributes-panel:textures/icons/atk.png"},
 *         ],
 *       },
 *       {
 *         "header": "[kevs-attributes-panel:textures/gui/defense.png]"
 *       }
 *     ],
 *     "otherHeader": "Other",
 *     "otherHeaderIcon": null,
 *     "globalBlacklist": ["examplemod:hidden_attr"]
 *   }
 * }
 */
public class AttributesPanelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/attributespanel.json");

    public int xOffset = -61;
    public int yOffset = 10;

    public GuiStyle guiStyle = GuiStyle.COMPACT;

    /** Attribute ids that are stored as 0..1 but should display as percent. */
    public List<String> percentAttributes =
            new ArrayList<>(List.of("kevslibrary:armor_penetration"));
    /** Attribute ids that are stored as 100..200 (100 = 0%) but should display as percent. */
    public List<String> percentAttributesBase100 =
            new ArrayList<>(List.of(
                    "spell_power:critical_damage",
                    "spell_power:critical_chance",
                    "spell_power:haste",
                    "ranged_weapon:haste",
                    "spell_engine:damage_taken",
                    "spell_engine:healing_taken"
            ));
    public List<String> percentKeywords = new ArrayList<>(List.of(
            "chance", "movement_speed"
    ));
    public List<String> percentBase100Keywords = new ArrayList<>();

    public CompactSettings compact = CompactSettings.defaultPreset();

    public static AttributesPanelConfig INSTANCE = new AttributesPanelConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, AttributesPanelConfig.class);
                if (INSTANCE.compact == null) {
                    INSTANCE.compact = CompactSettings.defaultPreset();
                }
                return;
            } catch (Exception e) {
                System.err.println("[Kev's Attributes Panel] Failed to read config: " + e.getMessage());
            }
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
                + ", GUI=" + INSTANCE.guiStyle);
    }

    public enum GuiStyle {
        BOOK,
        VANILLA,
        COMPACT
    }


    public static class CompactSettings {
        /** Ordered list of headers; remaining attributes go to "otherHeader". */
        public List<HeaderDef> headers = new ArrayList<>();

        /** Title for “Other” group (hardcoded fallback if null/blank = "Other") */
        public String otherHeaderName = "Other";

        /** Optional icon path for the “Other” header, e.g. "modid:textures/gui/other.png" */
        public String otherHeaderIcon = null;

        /** Global blacklist: attributes matching any pattern here are hidden entirely. */
        public List<String> globalBlacklist = new ArrayList<>();

        public static CompactSettings defaultPreset() {
            CompactSettings s = new CompactSettings();

            // Offensive
            HeaderDef offensive = new HeaderDef();
            offensive.header = "Offensive";
            offensive.attributes.add(AttributeSpec.of("minecraft:generic.attack_damage", null));
            offensive.attributes.add(AttributeSpec.of("minecraft:generic.attack_speed",  null));
            offensive.attributes.add(AttributeSpec.of("ranged_weapon:damage",            null));
            offensive.attributes.add(AttributeSpec.of("ranged_weapon:haste",             null));
            s.headers.add(offensive);

            // Defensive
            HeaderDef defensive = new HeaderDef();
            defensive.header = "Defensive";
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.armor",                 null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.armor_toughness",      null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.max_health",           null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.movement_speed",       null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.luck",                 null));
            defensive.attributes.add(AttributeSpec.of("minecraft:generic.knockback_resistance", null));
            s.headers.add(defensive);

            // Magic
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
            s.globalBlacklist = new ArrayList<>();

            return s;
        }
    }

   public static class HeaderDef {
        /**
         * Header label rules:
         *  - "Text" -> prints "Text".
         *  - "[modid:textures/some.png]" -> icon only.
         *  - "Text [modid:textures/some.png]" -> prints Text, then tries icon in [].
         *    If icon missing at runtime, prints "Text [cant find texture]".
         */
        public String header = "Header";

        public List<AttributeSpec> attributes = new ArrayList<>();

        /** Optional blacklist local to this header. */
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
