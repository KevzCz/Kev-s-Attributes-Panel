package net.pixeldreamstudios.attributepanel.command;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class AttributeSnapshotCommand {

    @ExpectPlatform
    public static void register() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerAttributeCheck() {
        throw new AssertionError();
    }
}