package net.pixeldreamstudios.attributepanel.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

// Screen#hasShiftDown/#hasControlDown were removed in 1.21.9+; query GLFW directly.
public final class PanelKeys {
    private PanelKeys() {
    }

    private static boolean isKeyDown(int key) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public static boolean hasShiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean hasControlDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
