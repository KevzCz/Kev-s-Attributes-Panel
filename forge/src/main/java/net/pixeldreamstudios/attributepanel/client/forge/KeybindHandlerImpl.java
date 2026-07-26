package net.pixeldreamstudios.attributepanel.client.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.pixeldreamstudios.attributepanel.KevsAttributesPanel;
import net.pixeldreamstudios.attributepanel.api.AttributePanelAPI;
import net.pixeldreamstudios.attributepanel.client.KeybindHandler;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = KevsAttributesPanel.MOD_ID, value = Dist.CLIENT)
public class KeybindHandlerImpl {
    private static KeyMapping quickOpenKeybind;

    /** Keybind registration runs on the mod bus. */
    @Mod.EventBusSubscriber(modid = KevsAttributesPanel.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterKeybindings(RegisterKeyMappingsEvent event) {
            quickOpenKeybind = new KeyMapping(
                    "key.kevs_attributes_panel.quick_open",
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories.kevs_attributes_panel"
            );
            event.register(quickOpenKeybind);

            KeybindHandler.setQuickOpenKeybind(quickOpenKeybind);
        }
    }

    /**
     * 1.20.1 Forge has a single {@code TickEvent.ClientTickEvent} with a phase, rather than
     * NeoForge's split Pre/Post events.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (quickOpenKeybind == null) return;

        while (quickOpenKeybind.consumeClick()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.screen == null) {
                client.setScreen(new InventoryScreen(client.player));

                client.execute(() -> {
                    if (client.screen instanceof InventoryScreen) {
                        AttributePanelAPI.togglePanel();
                    }
                });
            }
        }
    }

    public static void register() {
    }
}
