package net.pixeldreamstudios.attributepanel.forge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.PathPackResources;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.AddPackFindersEvent;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import net.pixeldreamstudios.attributepanel.network.forge.NetworkManagerImpl;

import java.nio.file.Path;

@Mod(KevsAttributesPanel.MOD_ID)
public final class KevsAttributesPanelForge {

    public KevsAttributesPanelForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        AttributesPanelConfig.load();
        KevsAttributesPanel.init();
        NetworkManagerImpl.registerPayloads();
        modBus.addListener(this::addPackFinders);
    }

    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        registerBuiltinPack(event, "kap_minimal_dark", "KAP Minimal Dark");
        registerBuiltinPack(event, "kevs_attributes_panel_old", "Old Panel Icons");
    }


    private void registerBuiltinPack(AddPackFindersEvent event, String folder, String title) {
        ModList.get().getModContainerById(KevsAttributesPanel.MOD_ID).ifPresent(container -> {
            Path resourcePath = container.getModInfo().getOwningFile().getFile()
                    .findResource("resourcepacks/" + folder);

            String packId = KevsAttributesPanel.MOD_ID + ":" + folder;

            Pack pack = Pack.readMetaAndCreate(
                    packId,
                    Component.literal(title),
                    false,
                    id -> new PathPackResources(id, resourcePath, false),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );

            if (pack != null) {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        });
    }
}
