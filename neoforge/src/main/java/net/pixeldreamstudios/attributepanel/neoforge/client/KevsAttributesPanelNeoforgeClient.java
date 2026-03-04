package net.pixeldreamstudios.attributepanel.neoforge.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.client.KeybindHandler;
import net.pixeldreamstudios.attributepanel.command.AttributeSnapshotCommand;
import net.pixeldreamstudios.attributepanel.config.ConfigScreenBuilder;

@EventBusSubscriber(modid = KevsAttributesPanel.MOD_ID, value = Dist.CLIENT)
public class KevsAttributesPanelNeoforgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AttributeSnapshotCommand.register();
        KeybindHandler.register();

        event.enqueueWork(() -> {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (minecraft, parent) -> (Screen) ConfigScreenBuilder.buildConfigScreen(parent));
        });
    }
}