package net.pixeldreamstudios.attributepanel.client.neoforge;

import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.neoforged.api.distmarker.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.*;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.*;
import net.pixeldreamstudios.attributepanel.*;
import net.pixeldreamstudios.attributepanel.api.AttributePanelAPI;
import net.pixeldreamstudios.attributepanel.client.KeybindHandler;
import org.lwjgl.glfw.*;

@EventBusSubscriber(modid = KevsAttributesPanel.MOD_ID, value = Dist.CLIENT)
public class KeybindHandlerImpl {
    private static KeyMapping quickOpenKeybind;

    @SubscribeEvent
    public static void onRegisterKeybindings(RegisterKeyMappingsEvent event) {
        quickOpenKeybind = new KeyMapping(
                "key.kevs_attributes_panel.quick_open",
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.kevs_attributes_panel"
        );
        event.register(quickOpenKeybind);
        
        KeybindHandler.setQuickOpenKeybind(quickOpenKeybind);

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event2) -> {
            while (quickOpenKeybind.consumeClick()) {
                Minecraft client = Minecraft.getInstance();
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

    public static void register() {
    }
}
