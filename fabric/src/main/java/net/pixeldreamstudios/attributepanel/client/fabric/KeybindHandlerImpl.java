package net.pixeldreamstudios.attributepanel.client.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.pixeldreamstudios.attributepanel.api.AttributePanelAPI;
import net.pixeldreamstudios.attributepanel.client.KeybindHandler;
import org.lwjgl.glfw.GLFW;

public class KeybindHandlerImpl {
    private static KeyMapping quickOpenKeybind;
    private static KeyMapping.Category category;

    public static void register() {
        // KeyMapping categories are Identifier-backed records in 1.21.9+
        category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kevs_attributes_panel", "category"));
        quickOpenKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.kevs_attributes_panel.quick_open",
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        KeybindHandler.setQuickOpenKeybind(quickOpenKeybind);

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
