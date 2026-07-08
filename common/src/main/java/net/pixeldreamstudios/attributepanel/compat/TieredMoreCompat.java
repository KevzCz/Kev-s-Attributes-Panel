package net.pixeldreamstudios.attributepanel.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class TieredMoreCompat {
    public record ImprintState(
            String imprintId,
            String plateNameKey,
            int plateColorRgb,
            float current,
            float max,
            String valueDisplay,
            int stacks,
            float cooldownFill,
            boolean active
    ) {}

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    public static List<ImprintState> getActiveImprints(Player player) {
        if (!isLoaded()) {
            return new ArrayList<>();
        }
        return getActiveImprintsImpl(player);
    }

    @ExpectPlatform
    protected static List<ImprintState> getActiveImprintsImpl(Player player) {
        throw new AssertionError();
    }
}
