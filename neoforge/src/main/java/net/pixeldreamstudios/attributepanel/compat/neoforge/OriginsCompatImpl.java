package net.pixeldreamstudios.attributepanel.compat.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.compat.OriginsCompat;

import java.util.Optional;

public final class OriginsCompatImpl {

    private OriginsCompatImpl() {
    }

    public static Optional<OriginsCompat.Source> findSource(Player player, ResourceLocation modifierId) {
        return Optional.empty();
    }
}
