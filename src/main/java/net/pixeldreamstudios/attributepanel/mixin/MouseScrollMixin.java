package net.pixeldreamstudios.attributepanel.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public abstract class MouseScrollMixin {

    @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void attributespanel$globalScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Screen screen = mc.currentScreen;
        if (screen == null) return;

        if (!(screen instanceof ParentElement parent)) return;

        double scaledX = mc.mouse.getX() * (double) mc.getWindow().getScaledWidth()  / (double) mc.getWindow().getWidth();
        double scaledY = mc.mouse.getY() * (double) mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        List<? extends Element> children = parent.children();
        if (children == null) return;

        for (Element e : children) {
            if (e instanceof AttributePanelDrawable panel && panel.isExpanded()) {
                boolean used = panel.mouseScrolled(scaledX, scaledY, horizontal, vertical);
                if (used) {
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
