package net.pixeldreamstudios.attributepanel.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable;
import net.pixeldreamstudios.attributepanel.config.AttributesPanelConfig;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> extends Screen {

    protected HandledScreenMixin(Component title) {
        super(title);
    }

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow protected T menu;

    @Unique
    private static final ResourceLocation ATTRIBUTE_BOOK =
            ResourceLocation.fromNamespaceAndPath("kevs_attributes_panel", "textures/gui/attribute_book.png");

    @Unique
    private int attributespanel$iconTick = 0;

    @Unique
    private AttributePanelDrawable attributespanel$attributePanel;

    @Unique
    private boolean attributespanel$shouldAttach() {
        return this.menu instanceof InventoryMenu;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void attributespanel$onInit(CallbackInfo ci) {
        if (!attributespanel$shouldAttach()) return;

        int panelX = this.leftPos + AttributesPanelConfig.INSTANCE.panelOffsetX;
        int panelY = this.topPos + AttributesPanelConfig.INSTANCE.panelOffsetY;

        attributespanel$attributePanel = new AttributePanelDrawable(panelX, panelY, 120);
        attributespanel$attributePanel.setHeightFromInventory(166);

        this.addRenderableWidget(attributespanel$attributePanel);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void attributespanel$onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!attributespanel$shouldAttach()) return;

        attributespanel$iconTick++;
        if (attributespanel$attributePanel != null) {
            attributespanel$attributePanel.tick();

            int iconSize = 9;
            int buttonX = this.leftPos + this.imageWidth / 2 + AttributesPanelConfig.INSTANCE.xOffset;
            int buttonY = this.topPos + AttributesPanelConfig.INSTANCE.yOffset;

            boolean hovered = mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                    mouseY >= buttonY && mouseY <= buttonY + iconSize;

            int color = hovered ? 0xFFFFFFFF : 0xFF666666;
            context.setColor(
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

                PoseStack pose = context.pose();
                pose.pushPose();
                pose.translate(buttonX + iconSize / 2f, buttonY + iconSize / 2f, 0);
                pose.scale(scale, scale, 1f);
                pose.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(rotation)));
                pose.translate(-iconSize / 2f, -iconSize / 2f, 0);
                context.blit(ATTRIBUTE_BOOK, 0, 0, 0, 0, iconSize, iconSize, 9, 9);
                pose.popPose();
            } else {
                context.blit(ATTRIBUTE_BOOK, buttonX, buttonY, 0, 0, iconSize, iconSize, 9, 9);
            }

            context.setColor(1f, 1f, 1f, 1f);

            if (hovered) {
                context.renderTooltip(this.font, Component.translatable("attributepanel.tooltip.button"), mouseX, mouseY);
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void attributespanel$renderTooltipAfterEverything(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
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
            int buttonX = this.leftPos + this.imageWidth / 2 + AttributesPanelConfig.INSTANCE.xOffset;
            int buttonY = this.topPos + AttributesPanelConfig.INSTANCE.yOffset;

            if (mouseX >= buttonX && mouseX <= buttonX + iconSize &&
                    mouseY >= buttonY && mouseY <= buttonY + iconSize) {
                attributespanel$attributePanel.toggle();
                cir.setReturnValue(true);
                return;
            }

            if (attributespanel$attributePanel.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(attributespanel$attributePanel);
                if (button == 0) this.setDragging(true);
                cir.setReturnValue(true);
            }
        }
    }
}