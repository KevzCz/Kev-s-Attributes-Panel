package net.pixeldreamstudios.attributepanel.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;

public class KeybindHandler {
    private static KeyMapping quickOpenKeybind;

    @ExpectPlatform
    public static void register() {
        throw new AssertionError();
    }

    public static KeyMapping getQuickOpenKeybind() {
        return quickOpenKeybind;
    }

    public static void setQuickOpenKeybind(KeyMapping keybind) {
        quickOpenKeybind = keybind;
    }
}
