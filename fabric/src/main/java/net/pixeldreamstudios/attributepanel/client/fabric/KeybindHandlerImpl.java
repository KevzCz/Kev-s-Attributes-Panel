package net.pixeldreamstudios.attributepanel.client.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.pixeldreamstudios.attributepanel.api.AttributePanelAPI;
import org.lwjgl.glfw.GLFW;

public class KeybindHandlerImpl {
    private static KeyMapping quickOpenKeybind;

    public static void register() {
        quickOpenKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.kevs_attributes_panel.quick_open",
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.kevs_attributes_panel"
        ));

        net.pixeldreamstudios.attributepanel.client.KeybindHandler.setQuickOpenKeybind(quickOpenKeybind);

        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            while (quickOpenKeybind.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new InventoryScreen(client.player));
                    
                    Minecraft.getInstance().execute(() -> {
                        if (client.screen instanceof InventoryScreen) {
                            AttributePanelAPI.togglePanel();
                        }
                    });
                }
            }
        });
    }
}
