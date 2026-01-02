package net.pixeldreamstudios.attributepanel.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import net.pixeldreamstudios.attributepanel.config.ConfigScreenBuilder;
import net.pixeldreamstudios.attributepanel.network.neoforge.NetworkManagerImpl;

@Mod(KevsAttributesPanel.MOD_ID)
public final class KevsAttributesPanelNeoforge {

    public KevsAttributesPanelNeoforge(IEventBus modBus, ModContainer modContainer) {
        KevsAttributesPanel.init();
        AttributesPanelConfig.load();
        modBus.addListener(this:: registerPayloads);
        modBus.addListener(this::addPackFinders);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraft, parent) -> ConfigScreenBuilder.buildConfigScreen(parent));
    }

    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(KevsAttributesPanel.MOD_ID, "resourcepacks/kap_minimal_dark"),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("KAP Minimal Dark"),
                    PackSource.BUILT_IN,
                    false,
                    Pack.Position.TOP
            );

            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(KevsAttributesPanel.MOD_ID, "resourcepacks/kevs_attributes_panel_old"),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("Old Panel Icons"),
                    PackSource.BUILT_IN,
                    false,
                    Pack.Position.TOP
            );
        }
    }
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        NetworkManagerImpl.registerPayloads(event);
    }
}