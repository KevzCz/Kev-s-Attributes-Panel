package net.pixeldreamstudios.attributepanel.mixin;

import net.minecraft.client.Minecraft;
import net.pixeldreamstudios.attributepanel.client.AnimatedTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class AnimatedFrameMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void attributespanel$startAnimatedFrame(boolean renderLevel, CallbackInfo info) {
        AnimatedTextures.startFrame();
    }
}
