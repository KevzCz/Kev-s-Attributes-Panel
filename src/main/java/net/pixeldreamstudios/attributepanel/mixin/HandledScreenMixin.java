package net.pixeldreamstudios.attributepanel.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    protected HandledScreenMixin(Text title) { super(title); }

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    @Shadow protected T handler;

    @Unique private static final Identifier ATTRIBUTE_BOOK =
            Identifier.of("kevs-attributes-panel", "textures/gui/attribute_book.png");

    @Unique private int attributespanel$iconTick = 0;
    @Unique private AttributePanelDrawable attributespanel$attributePanel;

    @Unique
    private boolean attributespanel$shouldAttach() {
        return this.handler instanceof PlayerScreenHandler;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void attributespanel$onInit(CallbackInfo ci) {
        if (!attributespanel$shouldAttach()) return;

        int panelX = this.x + AttributesPanelConfig.INSTANCE.panelOffsetX;
        int panelY = this.y + AttributesPanelConfig.INSTANCE.panelOffsetY;

        attributespanel$attributePanel = new AttributePanelDrawable(panelX, panelY, 120);
        attributespanel$attributePanel.setHeightFromInventory(166);

        this.addDrawableChild(attributespanel$attributePanel);
        this.addSelectableChild(attributespanel$attributePanel);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void attributespanel$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!attributespanel$shouldAttach()) return;

        attributespanel$iconTick++;
        if (attributespanel$attributePanel != null) {
            attributespanel$attributePanel.tick();

            int iconSize = 9;
            int buttonX = this.x + this.backgroundWidth / 2 + AttributesPanelConfig.INSTANCE.xOffset;
            int buttonY = this.y + AttributesPanelConfig.INSTANCE.yOffset;

            boolean hovered = mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                    mouseY >= buttonY && mouseY <= buttonY + iconSize;

            int color = hovered ? 0xFFFFFFFF : 0xFF666666;
            context.setShaderColor(
                    ((color >> 16) & 0xFF) / 255f,
                    ((color >> 8) & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    ((color >> 24) & 0xFF) / 255f
            );

            boolean animate = AttributesPanelConfig.INSTANCE.hoverIconAnimation;
            if (hovered && animate) {
                float time = attributespanel$iconTick / 8f;
                float pulse = (float) Math.sin(time);
                float scale = 1.0f + 0.1f * pulse;
                float rotation = 1.5f * pulse;

                context.getMatrices().push();
                context.getMatrices().translate(buttonX + iconSize / 2f, buttonY + iconSize / 2f, 0);
                context.getMatrices().scale(scale, scale, 1f);
                context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
                context.getMatrices().translate(-iconSize / 2f, -iconSize / 2f, 0);
                context.drawTexture(ATTRIBUTE_BOOK, 0, 0, 0, 0, iconSize, iconSize, 9, 9);
                context.getMatrices().pop();
            } else {
                context.drawTexture(ATTRIBUTE_BOOK, buttonX, buttonY, 0, 0, iconSize, iconSize, 9, 9);
            }

            context.setShaderColor(1f, 1f, 1f, 1f);

            if (hovered) {
                context.drawTooltip(this.textRenderer, Text.translatable("attributepanel.tooltip.button"), mouseX, mouseY);
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void attributespanel$renderTooltipAfterEverything(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!attributespanel$shouldAttach()) return;
        if (attributespanel$attributePanel != null && attributespanel$attributePanel.isExpanded()) {
            attributespanel$attributePanel.renderTooltip(context);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void attributespanel$onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!attributespanel$shouldAttach()) return;

        if (attributespanel$attributePanel != null) {
            int iconSize = 9;
            int buttonX = this.x + this.backgroundWidth / 2 + AttributesPanelConfig.INSTANCE.xOffset;
            int buttonY = this.y + AttributesPanelConfig.INSTANCE.yOffset;

            if (mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                    mouseY >= buttonY && mouseY <= buttonY + iconSize) {
                attributespanel$attributePanel.toggle();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }

            if (attributespanel$attributePanel.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(attributespanel$attributePanel);
                if (button == 0) this.setDragging(true);
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}
