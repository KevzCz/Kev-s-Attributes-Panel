package net.pixeldreamstudios.attributepanel.config;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
@Environment(EnvType.CLIENT)
public class AttributesPanelModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("config.attributepanel.title"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.attributepanel.category.general"));

            general.addEntry(entryBuilder
                    .startIntField(Text.translatable("config.attributepanel.x_offset"), AttributesPanelConfig.INSTANCE.xOffset)
                    .setDefaultValue(-61)
                    .setTooltip(Text.translatable("config.attributepanel.x_offset.tooltip"))
                    .setSaveConsumer(value -> AttributesPanelConfig.INSTANCE.xOffset = value)
                    .build());

            general.addEntry(entryBuilder
                    .startIntField(Text.translatable("config.attributepanel.y_offset"), AttributesPanelConfig.INSTANCE.yOffset)
                    .setDefaultValue(66)
                    .setTooltip(Text.translatable("config.attributepanel.y_offset.tooltip"))
                    .setSaveConsumer(value -> AttributesPanelConfig.INSTANCE.yOffset = value)
                    .build());

            general.addEntry(entryBuilder
                    .startBooleanToggle(Text.translatable("config.attributepanel.book_background"), AttributesPanelConfig.INSTANCE.useBookBackground)
                    .setDefaultValue(true)
                    .setTooltip(Text.translatable("config.attributepanel.book_background.tooltip"))
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
