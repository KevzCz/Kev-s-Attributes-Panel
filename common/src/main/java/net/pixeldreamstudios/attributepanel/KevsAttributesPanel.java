package net.pixeldreamstudios.attributepanel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KevsAttributesPanel {
    public static final String MOD_ID = "kevs_attributes_panel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static void init() {
        trackVisibleAttributes();
    }

    private static void trackVisibleAttributes() {
        var config = AttributesPanelConfig.INSTANCE;
        if (config.forceTrackVisibleAttributes) {
            for (var section : config.compact.headers) {
                for (var attribute : section.attributes) {
                    if (attribute.id == null || attribute.id.isBlank()) {
                        continue;
                    }

                    if (attribute.id.contains("*")) {
                        String regex = attribute.id.replace("*", ".*");
                        for (var entry : BuiltInRegistries.ATTRIBUTE) {
                            var entryId = BuiltInRegistries.ATTRIBUTE.getKey(entry);
                            if (entryId != null && entryId.toString().matches(regex)) {
                                try {
                                    entry.setSyncable(true);
                                } catch (Exception e) {
                                    LOGGER.warn("Failed to set syncable for attribute: " + entryId, e);
                                }
                            }
                        }
                    } else {
                        var id = ResourceLocation.tryParse(attribute.id);
                        if (id == null) continue;

                        var entry = BuiltInRegistries.ATTRIBUTE.getOptional(id);
                        if (entry.isPresent()) {
                            try {
                                entry.get().setSyncable(true);
                            } catch (Exception e) {
                                LOGGER.warn("Failed to set syncable for attribute: " + id, e);
                            }
                        }
                    }
                }
            }
            for (String attrIdStr : config.percentAttributesBase100) {
                var id = ResourceLocation.tryParse(attrIdStr);
                if (id == null) continue;

                var entry = BuiltInRegistries.ATTRIBUTE.getOptional(id);
                if (entry.isPresent()) {
                    try {
                        entry.get().setSyncable(true);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to set syncable for attribute: " + id, e);
                    }
                }
            }

            for (String attrIdStr : config.percentAttributes) {
                var id = ResourceLocation.tryParse(attrIdStr);
                if (id == null) continue;

                var entry = BuiltInRegistries.ATTRIBUTE.getOptional(id);
                if (entry.isPresent()) {
                    try {
                        entry.get().setSyncable(true);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to set syncable for attribute: " + id, e);
                    }
                }
            }
        }
    }
}