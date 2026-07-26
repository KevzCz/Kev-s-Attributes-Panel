package net.pixeldreamstudios.attributepanel.forge.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.client.KeybindHandler;
import net.pixeldreamstudios.attributepanel.command.AttributeSnapshotCommand;
import net.pixeldreamstudios.attributepanel.config.ConfigScreenBuilder;
import net.pixeldreamstudios.attributepanel.network.forge.NetworkManagerImpl;

@Mod.EventBusSubscriber(modid = KevsAttributesPanel.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class KevsAttributesPanelForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AttributeSnapshotCommand.register();
        KeybindHandler.register();
        NetworkManagerImpl.registerClientPayloads();

        event.enqueueWork(() -> {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parent) -> (Screen) ConfigScreenBuilder.buildConfigScreen(parent)));
        });
    }
}
