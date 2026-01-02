package net.pixeldreamstudios.attributepanel.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.command.AttributeSnapshotCommand;

@EventBusSubscriber(modid = KevsAttributesPanel.MOD_ID, value = Dist.CLIENT)
public class KevsAttributesPanelNeoforgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AttributeSnapshotCommand.register();
    }
}