package net.pixeldreamstudios.attributepanel.compat.fabric;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.compat.OriginsCompat;

import java.util.Optional;

public final class OriginsCompatImpl {

    private OriginsCompatImpl() {
    }

    public static Optional<OriginsCompat.Source> findSource(Player player, Identifier modifierId) {
        return Optional.empty();
    }
}
