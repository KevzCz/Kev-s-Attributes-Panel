package net.pixeldreamstudios.attributepanel.config;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class AttributesPanelModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Kev's Library Config"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));

            general.addEntry(entryBuilder
                    .startIntField(Text.literal("Book icon X Offset"), AttributesPanelConfig.INSTANCE.xOffset)
                    .setDefaultValue(-61)
                    .setTooltip(Text.of("Horizontal position of the attribute panel icon"))
                    .setSaveConsumer(value -> AttributesPanelConfig.INSTANCE.xOffset = value)
                    .build());

            general.addEntry(entryBuilder
                    .startIntField(Text.literal("Book icon Y Offset"), AttributesPanelConfig.INSTANCE.yOffset)
                    .setDefaultValue(66)
                    .setTooltip(Text.of("Vertical position of the attribute panel icon"))
                    .setSaveConsumer(value -> AttributesPanelConfig.INSTANCE.yOffset = value)
                    .build());
            general.addEntry(entryBuilder
                    .startBooleanToggle(Text.literal("Use Book Background"), AttributesPanelConfig.INSTANCE.useBookBackground)
                    .setDefaultValue(true)
                    .setTooltip(Text.of("If enabled, shows stats inside a book UI. Otherwise uses vanilla-styled panels."))
                    .setSaveConsumer(value -> AttributesPanelConfig.INSTANCE.useBookBackground = value)
                    .build());

            builder.setSavingRunnable(() -> {
                AttributesPanelConfig.save();
                AttributesPanelConfig.apply();
            });

            return builder.build();
        };
    }
}
