package net.pixeldreamstudios.attributepanel.mixin;

import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.CreativeModeTab;
import net.pixeldreamstudios.attributepanel.accessor.*;
import net.pixeldreamstudios.attributepanel.client.*;
import net.pixeldreamstudios.attributepanel.config.*;
import org.joml.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.lang.Math;
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> extends Screen implements AttributePanelAccessor {

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
        if (this.menu instanceof InventoryMenu) return true;
        return attributespanel$isCreativeInventoryTab();
    }

    @Unique
    private boolean attributespanel$isCreativeInventoryTab() {
        if (!((Object) this instanceof CreativeModeInventoryScreen)) return false;
        CreativeModeTab tab = CreativeSelectedTabAccessor.attributespanel$getSelectedTab();
        return tab != null && tab.getType() == CreativeModeTab.Type.INVENTORY;
    }

    @Unique
    private int attributespanel$buttonXOffset() {
        return attributespanel$isCreativeInventoryTab()
                ? AttributesPanelConfig.INSTANCE.creativeXOffset
                : AttributesPanelConfig.INSTANCE.xOffset;
    }

    @Unique
    private int attributespanel$buttonYOffset() {
        return attributespanel$isCreativeInventoryTab()
                ? AttributesPanelConfig.INSTANCE.creativeYOffset
                : AttributesPanelConfig.INSTANCE.yOffset;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void attributespanel$onInit(CallbackInfo ci) {
        attributespanel$attributePanel = null;
        attributespanel$ensurePanel();
    }

    @Unique
    private void attributespanel$ensurePanel() {
        if (!attributespanel$shouldAttach()) return;

        if (attributespanel$attributePanel == null) {
            int panelX = this.leftPos + AttributesPanelConfig.INSTANCE.panelOffsetX;
            int panelY = this.topPos + AttributesPanelConfig.INSTANCE.panelOffsetY;

            attributespanel$attributePanel = new AttributePanelDrawable(panelX, panelY, 120);
            attributespanel$attributePanel.setHeightFromInventory(166);
            attributespanel$attributePanel.setExpanded(AttributePanelDrawable.getExpandedMemory());

            this.addRenderableWidget(attributespanel$attributePanel);
        } else if (!this.children().contains(attributespanel$attributePanel)) {
            this.addRenderableWidget(attributespanel$attributePanel);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void attributespanel$onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!attributespanel$shouldAttach()) return;
        attributespanel$ensurePanel();

        attributespanel$iconTick++;
        if (attributespanel$attributePanel != null) {
            attributespanel$attributePanel.tick();

            int iconSize = 9;
            int buttonX = this.leftPos + this.imageWidth / 2 + attributespanel$buttonXOffset();
            int buttonY = this.topPos + attributespanel$buttonYOffset();

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
            attributespanel$attributePanel.renderLate(context, mouseX, mouseY, delta);
            attributespanel$attributePanel.renderImprintWindowLate(context);
            attributespanel$attributePanel.renderTooltip(context);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void attributespanel$onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!attributespanel$shouldAttach()) return;
        attributespanel$ensurePanel();

        if (attributespanel$attributePanel != null) {
            int iconSize = 9;
            int buttonX = this.leftPos + this.imageWidth / 2 + attributespanel$buttonXOffset();
            int buttonY = this.topPos + attributespanel$buttonYOffset();

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

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void attributespanel$onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!attributespanel$shouldAttach()) return;
        if (attributespanel$attributePanel != null &&
                attributespanel$attributePanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void attributespanel$onMouseReleased(double mouseX, double mouseY, int button,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!attributespanel$shouldAttach()) return;
        if (attributespanel$attributePanel != null &&
                attributespanel$attributePanel.mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public AttributePanelDrawable attributespanel$getAttributePanel() {
        return attributespanel$attributePanel;
    }
}