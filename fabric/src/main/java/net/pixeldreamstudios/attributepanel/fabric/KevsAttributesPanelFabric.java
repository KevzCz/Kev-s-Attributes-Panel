package net.pixeldreamstudios.attributepanel.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import net.pixeldreamstudios.attributepanel.network.fabric.NetworkManagerImpl;

public final class KevsAttributesPanelFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KevsAttributesPanel.init();
        AttributesPanelConfig.load();
        NetworkManagerImpl.registerServerPayloads();
        registerBuiltinResourcePacks();
    }

    private void registerBuiltinResourcePacks() {

        FabricLoader.getInstance().getModContainer("kevs_attributes_panel").ifPresent(modContainer -> {

            ResourceManagerHelper.registerBuiltinResourcePack(
                    ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "kap_minimal_dark"),
                    modContainer,
                    Component.literal("KAP Minimal Dark"),
                    ResourcePackActivationType.NORMAL
            );

            ResourceManagerHelper.registerBuiltinResourcePack(
                    ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "kevs_attributes_panel_old"),
                    modContainer,
                    Component.literal("Old Panel Icons"),
                    ResourcePackActivationType.NORMAL
            );

        });
    }
}