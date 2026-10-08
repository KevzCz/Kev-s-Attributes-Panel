package net.pixeldreamstudios.attributepanel.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.world.effect.MobEffect;
import net.pixeldreamstudios.attributepanel.accessor.AttributePanelAccessor;
import net.pixeldreamstudios.attributepanel.client.AttributePanelDrawable;

@EmiEntrypoint
public class KapEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericStackProvider((screen, x, y) -> {
            if (!(screen instanceof AttributePanelAccessor accessor)) return EmiStackInteraction.EMPTY;

            AttributePanelDrawable panel = accessor.attributespanel$getAttributePanel();
            if (panel == null) return EmiStackInteraction.EMPTY;

            MobEffect effect = panel.getEffectAt(x, y);
            if (effect == null) return EmiStackInteraction.EMPTY;

            EmiStack stack = EmiEffectBridge.find(effect);
            return stack == null ? EmiStackInteraction.EMPTY : new EmiStackInteraction(stack, null, false);
        });
    }
}
