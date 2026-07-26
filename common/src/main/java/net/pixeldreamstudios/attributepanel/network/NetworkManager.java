package net.pixeldreamstudios.attributepanel.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;
import net.pixeldreamstudios.attributepanel.network.payload.SendAttributeSnapshotPayload;

public class NetworkManager {

    @ExpectPlatform
    public static void registerClientPayloads() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerServerPayloads() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToServer(RequestAttributeSnapshotPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToClient(ServerPlayer player, SendAttributeSnapshotPayload payload) {
        throw new AssertionError();
    }
}