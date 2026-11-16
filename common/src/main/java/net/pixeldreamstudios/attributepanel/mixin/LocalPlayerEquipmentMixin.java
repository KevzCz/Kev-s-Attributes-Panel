package net.pixeldreamstudios.attributepanel.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerEquipmentMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void attributespanel$applyEquipmentAttributes(CallbackInfo info) {
        ((LivingEntityEquipmentInvoker) this).attributespanel$collectEquipmentChanges();
    }
}
