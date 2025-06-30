package net.pixeldreamstudios.attributepanel.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;

public class AttributeSnapshotCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommand(dispatcher);
        });
    }

    private static void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("attributesnapshot")
                .then(ClientCommandManager.argument("target", StringArgumentType.word())
                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "target"))))
        );
    }

    private static int execute(CommandContext<FabricClientCommandSource> ctx, String targetName) {
        ClientPlayerEntity player = ctx.getSource().getPlayer();

        RequestAttributeSnapshotPayload payload = new RequestAttributeSnapshotPayload(targetName);
        ClientPlayNetworking.send(payload);

        player.sendMessage(Text.literal("Requested attribute snapshot for: " + targetName)
                .formatted(Formatting.GRAY), false);

        return 1;
    }
}
