package net.pixeldreamstudios.attributepanel.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.pixeldreamstudios.attributepanel.command.AttributeSnapshotCommand;
import net.pixeldreamstudios.attributepanel.network.ClientNetwork;

@Environment(EnvType.CLIENT)
public class KevsAttributesPanelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetwork.register();
        AttributeSnapshotCommand.register();
    }
}
