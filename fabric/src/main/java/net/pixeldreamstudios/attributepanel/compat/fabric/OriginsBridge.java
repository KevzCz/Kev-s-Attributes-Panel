package net.pixeldreamstudios.attributepanel.compat.fabric;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.type.AttributePowerType;
import io.github.apace100.apoli.util.AttributedEntityAttributeModifier;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.compat.OriginsCompat;

import java.util.Optional;

final class OriginsBridge {
    private static final boolean INCLUDE_INACTIVE = true;

    private OriginsBridge() {
    }

    static Optional<OriginsCompat.Source> findSource(Player player, ResourceLocation modifierId) {
        PowerHolderComponent holder = PowerHolderComponent.getNullable(player);
        if (holder == null) {
            return Optional.empty();
        }
        for (AttributePowerType type : holder.getPowerTypes(AttributePowerType.class, INCLUDE_INACTIVE)) {
            for (AttributedEntityAttributeModifier modifier : type.attributedModifiers()) {
                if (modifier.modifier().id().equals(modifierId)) {
                    return originOf(holder, type.getPower());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<OriginsCompat.Source> originOf(PowerHolderComponent holder, Power power) {
        for (ResourceLocation source : holder.getSources(power)) {
            Optional<Origin> origin = OriginManager.getOptional(source);
            if (origin.isPresent()) {
                return Optional.of(new OriginsCompat.Source(origin.get().getName(), origin.get().getDisplayItem()));
            }
        }
        return Optional.empty();
    }
}
