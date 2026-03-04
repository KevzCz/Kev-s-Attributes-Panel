package net.pixeldreamstudios.attributepanel.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.pixeldreamstudios.attributepanel.client.KeybindHandler;
import net.pixeldreamstudios.attributepanel.command.AttributeSnapshotCommand;
import net.pixeldreamstudios.attributepanel.network.fabric.NetworkManagerImpl;

public final class KevsAttributesPanelFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AttributeSnapshotCommand.register();
        AttributeSnapshotCommand.registerAttributeCheck();
        NetworkManagerImpl.registerClientPayloads();
        KeybindHandler.register();
    }
}
