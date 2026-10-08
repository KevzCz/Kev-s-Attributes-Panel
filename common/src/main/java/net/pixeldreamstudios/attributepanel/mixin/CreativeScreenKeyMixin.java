package net.pixeldreamstudios.attributepanel.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.pixeldreamstudios.attributepanel.accessor.AttributePanelAccessor;
import net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenKeyMixin {

    @Unique
    private AttributePanelDrawable attributespanel$typingPanel() {
        CreativeModeTab tab = CreativeSelectedTabAccessor.attributespanel$getSelectedTab();
        if (tab == null || tab.getType() != CreativeModeTab.Type.INVENTORY) return null;
        if (!((Object) this instanceof AttributePanelAccessor accessor)) return null;

        AttributePanelDrawable panel = accessor.attributespanel$getAttributePanel();
        return panel != null && panel.wantsKeys() ? panel : null;
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void attributespanel$onCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        AttributePanelDrawable panel = attributespanel$typingPanel();
        if (panel != null) cir.setReturnValue(panel.charTyped(chr, modifiers));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void attributespanel$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return;
        AttributePanelDrawable panel = attributespanel$typingPanel();
        if (panel != null) cir.setReturnValue(panel.keyPressed(keyCode, scanCode, modifiers));
    }
}
