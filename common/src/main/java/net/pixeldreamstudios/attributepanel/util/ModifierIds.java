package net.pixeldreamstudios.attributepanel.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


public final class ModifierIds {

    private ModifierIds() {}

    public static final String UNKNOWN_NAMESPACE = "unknown";

    private static final Map<UUID, ResourceLocation> VANILLA = new HashMap<>();

    private static void vanilla(String uuid, String path) {
        UUID key = UUID.fromString(uuid);
        ResourceLocation previous = VANILLA.put(key, new ResourceLocation("minecraft", path));
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate vanilla modifier UUID " + uuid + ": " + previous + " vs minecraft:" + path);
        }
    }

    static {
        vanilla("CB3F55D3-645C-4F38-A497-9C13A33DB5CF", "base_attack_damage");
        vanilla("FA233E1C-4180-4865-B01B-BCCE9785ACA3", "base_attack_speed");

        vanilla("845DB27C-C624-495F-8C9F-6020A9A58B6B", "armor.boots");
        vanilla("D8499B04-0E66-4726-AB29-64469D734E0D", "armor.leggings");
        vanilla("9F3D476D-C118-4544-8365-64846904B48E", "armor.chestplate");
        vanilla("2AD3F246-FEE1-4E67-B886-69FD380BB150", "armor.helmet");

        vanilla("662A6B8D-DA3E-4C1C-8813-96EA6097278D", "sprinting");
        vanilla("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce", "soul_speed");
        vanilla("87f46a96-686f-4796-b035-22e16ee9e038", "powder_snow");

    }

    public static ResourceLocation of(AttributeModifier modifier) {
        if (modifier == null) {
            return new ResourceLocation(UNKNOWN_NAMESPACE, "null");
        }

        ResourceLocation known = VANILLA.get(modifier.getId());
        if (known != null) {
            return known;
        }

        return fromName(modifier.getName(), modifier.getId());
    }


    public static ResourceLocation fromName(String name, UUID fallbackUuid) {
        if (name == null || name.isBlank()) {
            return new ResourceLocation(UNKNOWN_NAMESPACE, uuidPath(fallbackUuid));
        }

        String trimmed = name.trim();

        ResourceLocation parsed = ResourceLocation.tryParse(trimmed);
        if (parsed != null && trimmed.indexOf(':') >= 0) {
            return parsed;
        }

        if (parsed != null) {
            return new ResourceLocation(UNKNOWN_NAMESPACE, parsed.getPath());
        }

        String path = sanitize(trimmed);
        if (path.isEmpty()) {
            path = uuidPath(fallbackUuid);
        }
        return new ResourceLocation(UNKNOWN_NAMESPACE, path);
    }

    public static boolean isUnknown(ResourceLocation id) {
        return id == null || UNKNOWN_NAMESPACE.equals(id.getNamespace());
    }

    private static String sanitize(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-') {
                sb.append(c);
            } else if (c == ' ' || c == '/') {
                sb.append('_');
            }
        }
        String path = sb.toString().replaceAll("_+", "_");
        if (path.startsWith("_")) path = path.substring(1);
        if (path.endsWith("_")) path = path.substring(0, path.length() - 1);
        return path;
    }

    private static String uuidPath(UUID uuid) {
        return uuid == null ? "unnamed" : uuid.toString();
    }

    public static boolean isArmor(ItemStack stack) {
        Item item = stack == null ? null : stack.getItem();
        return item instanceof ArmorItem;
    }
}
