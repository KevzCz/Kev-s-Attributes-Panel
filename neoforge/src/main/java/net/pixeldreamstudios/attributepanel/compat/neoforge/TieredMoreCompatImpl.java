package net.pixeldreamstudios.attributepanel.compat.neoforge;

import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintApi;
import draylar.tiered.api.imprint.ImprintCooldownDisplay;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.ImprintResolver;
import draylar.tiered.api.imprint.ability.AbilityBinding;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.pixeldreamstudios.attributepanel.compat.TieredMoreCompat;

import java.util.ArrayList;
import java.util.List;

public class TieredMoreCompatImpl {

    public static boolean isLoaded() {
        return ModList.get().isLoaded("tiered_more");
    }

    public static List<TieredMoreCompat.ImprintState> getActiveImprintsImpl(Player player) {
        List<TieredMoreCompat.ImprintState> results = new ArrayList<>();
        if (player == null) return results;

        for (String id : ImprintApi.allImprintIds()) {
            if (!ImprintApi.isActive(player, id)) continue;

            Imprint imprint = ImprintRegistry.get(id);
            if (!(imprint instanceof DataImprint data)) continue;

            AbilityBinding binding = data.activeBinding(player);
            String abilityId = binding != null ? binding.getAbility() : null;

            String plateNameKey = data.plateNameKeyFor(abilityId);
            int plateColorRgb = data.plateColorRgbFor(abilityId);

            float current = ImprintApi.resolvedPrimary(player, id);
            float max = ImprintApi.maxPrimary(id);
            int stacks = ImprintResolver.stackCount(player, id);

            String valueDisplay = "raw";
            ImprintApi.ImprintView view = ImprintApi.describe(id);
            if (view != null && !view.valueRanges().isEmpty()) {
                valueDisplay = view.valueRanges().get(0).display();
            }

            String cdKey = abilityId != null && !abilityId.isEmpty() ? abilityId : id;
            float cooldownFill = ImprintCooldownDisplay.fillOf(cdKey);
            boolean active = ImprintCooldownDisplay.isActive(cdKey);

            results.add(new TieredMoreCompat.ImprintState(
                    id, plateNameKey, plateColorRgb, current, max, valueDisplay, stacks, cooldownFill, active));
        }

        return results;
    }
}
