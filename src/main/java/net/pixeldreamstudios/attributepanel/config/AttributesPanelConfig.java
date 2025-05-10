package net.pixeldreamstudios.attributepanel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AttributesPanelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/attributespanel.json");
    public boolean useBookBackground = true;
    public int xOffset = -61;
    public int yOffset = 10;

    public static AttributesPanelConfig INSTANCE = new AttributesPanelConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, AttributesPanelConfig.class);
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
         System.out.println("[Kev's Attributes Panel] Config applied: xOffset=" + INSTANCE.xOffset + ", yOffset=" + INSTANCE.yOffset);
    }
}
