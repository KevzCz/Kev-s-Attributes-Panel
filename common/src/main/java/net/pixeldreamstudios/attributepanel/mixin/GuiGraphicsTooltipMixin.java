package net.pixeldreamstudios.attributepanel.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.pixeldreamstudios.attributepanel.accessor.AttributePanelAccessor;
import net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipMixin {

    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void attributespanel$suppressTooltipOverPanel(Font font, List<ClientTooltipComponent> components,
                                                          int x, int y, ClientTooltipPositioner positioner,
                                                          Identifier background, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AttributePanelAccessor accessor)) return;

        AttributePanelDrawable panel = accessor.attributespanel$getAttributePanel();
        if (panel == null) return;
        if (panel.isWithinPanelBounds(x, y)) {
            ci.cancel();
        }
    }
}
