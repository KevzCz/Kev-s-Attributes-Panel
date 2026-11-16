package net.pixeldreamstudios.attributepanel.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class OriginsCompat {

    public record Source(Component name, ItemStack icon) {
    }

    private OriginsCompat() {
    }

    @ExpectPlatform
    public static Optional<Source> findSource(Player player, ResourceLocation modifierId) {
        throw new AssertionError();
    }
}
