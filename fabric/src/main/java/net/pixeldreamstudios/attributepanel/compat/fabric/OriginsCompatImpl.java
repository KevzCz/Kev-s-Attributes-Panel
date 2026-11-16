package net.pixeldreamstudios.attributepanel.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.attributepanel.compat.OriginsCompat;

import java.util.Optional;

public final class OriginsCompatImpl {
    private static final String ORIGINS_MOD_ID = "origins";

    private OriginsCompatImpl() {
    }

    public static Optional<OriginsCompat.Source> findSource(Player player, ResourceLocation modifierId) {
        if (!FabricLoader.getInstance().isModLoaded(ORIGINS_MOD_ID)) {
            return Optional.empty();
        }
        return OriginsBridge.findSource(player, modifierId);
    }
}
