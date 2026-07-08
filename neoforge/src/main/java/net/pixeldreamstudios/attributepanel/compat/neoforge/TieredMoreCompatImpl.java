package net.pixeldreamstudios.attributepanel.compat.neoforge;

import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.compat.TieredMoreCompat;

import java.util.ArrayList;
import java.util.List;

public class TieredMoreCompatImpl {

    public static boolean isLoaded() {
        return false;
    }

    public static List<TieredMoreCompat.ImprintState> getActiveImprintsImpl(Player player) {
        return new ArrayList<>();
    }
}
