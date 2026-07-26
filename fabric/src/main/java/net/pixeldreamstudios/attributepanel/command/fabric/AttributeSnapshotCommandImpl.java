package net.pixeldreamstudios.attributepanel.command.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.pixeldreamstudios.attributepanel.util.ModifierIds;
import net.pixeldreamstudios.attributepanel.compat.TrinketsCompat;
import net.pixeldreamstudios.attributepanel.network.NetworkManager;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;

import java.util.*;
@Environment(EnvType.CLIENT)
public class AttributeSnapshotCommandImpl {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommand(dispatcher);
        });
    }

    public static void registerAttributeCheck() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerAttributeCheckCommand(dispatcher);
        });
    }

    private static void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("attributesnapshot")
                        .then(ClientCommandManager.argument("target", StringArgumentType.word())
                                .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "target")))
                        )
        );
    }

    private static void registerAttributeCheckCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        SuggestionProvider<FabricClientCommandSource> attributeSuggestions = (context, builder) -> {
            BuiltInRegistries.ATTRIBUTE.keySet().forEach(id -> builder.suggest(id.toString()));
            return builder.buildFuture();
        };

        dispatcher.register(
                ClientCommandManager.literal("attributepanel")
                        .then(ClientCommandManager.argument("attribute", StringArgumentType.greedyString())
                                .suggests(attributeSuggestions)
                                .executes(ctx -> executeAttributeCheck(ctx, StringArgumentType.getString(ctx, "attribute")))
                        )
        );
    }

    private static int execute(CommandContext<FabricClientCommandSource> ctx, String targetName) {
        LocalPlayer player = ctx.getSource().getPlayer();

        RequestAttributeSnapshotPayload payload = new RequestAttributeSnapshotPayload(targetName);
        NetworkManager.sendToServer(payload);

        player.sendSystemMessage(
                Component.literal("Requested attribute snapshot for: " + targetName)
                        .withStyle(ChatFormatting.GRAY)
        );

        return 1;
    }

    private static int executeAttributeCheck(CommandContext<FabricClientCommandSource> ctx, String attributeId) {
        LocalPlayer player = ctx.getSource().getPlayer();

        ResourceLocation attrResourceLocation;
        try {
            attrResourceLocation = ResourceLocation.tryParse(attributeId);
        } catch (Exception e) {
            player.sendSystemMessage(
                    Component.literal("Invalid attribute ID: " + attributeId)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        Attribute attrHolder = BuiltInRegistries.ATTRIBUTE.getOptional(attrResourceLocation).orElse(null);
        if (attrHolder == null) {
            player.sendSystemMessage(
                    Component.literal("Attribute not found: " + attributeId)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        Attribute attribute = attrHolder;
        AttributeInstance instance = player.getAttribute(attrHolder);

        if (instance == null) {
            player.sendSystemMessage(
                    Component.literal("You don't have this attribute:  " + attributeId)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        player.sendSystemMessage(
                Component.literal("=== Modifiers for:  ")
                        .append(Component.translatable(attribute.getDescriptionId()))
                        .append(" ===")
                        .withStyle(ChatFormatting.GOLD)
        );

        player.sendSystemMessage(
                Component.literal("Base Value: " + String.format("%.2f", instance.getBaseValue()))
                        .withStyle(ChatFormatting.YELLOW)
        );
        player.sendSystemMessage(
                Component.literal("Final Value: " + String.format("%.2f", instance.getValue()))
                        .withStyle(ChatFormatting.AQUA)
        );

        Collection<AttributeModifier> modifiers = instance.getModifiers();

        if (modifiers.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("No modifiers found.")
                            .withStyle(ChatFormatting.GRAY)
            );
            return 1;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("Modifiers:").withStyle(ChatFormatting.YELLOW));

        Map<AttributeModifier.Operation, List<AttributeModifier>> grouped = new HashMap<>();
        for (AttributeModifier mod : modifiers) {
            grouped.computeIfAbsent(mod.getOperation(), k -> new ArrayList<>()).add(mod);
        }

        if (grouped.containsKey(AttributeModifier.Operation.ADDITION)) {
            player.sendSystemMessage(Component.literal("  Flat Additions:").withStyle(ChatFormatting.GREEN));
            for (AttributeModifier mod : grouped.get(AttributeModifier.Operation.ADDITION)) {
                displayModifier(player, mod, attrHolder);
            }
        }

        if (grouped.containsKey(AttributeModifier.Operation.MULTIPLY_BASE)) {
            player.sendSystemMessage(Component.literal("  Base Multipliers:").withStyle(ChatFormatting.BLUE));
            for (AttributeModifier mod : grouped.get(AttributeModifier.Operation.MULTIPLY_BASE)) {
                displayModifier(player, mod, attrHolder);
            }
        }

        if (grouped.containsKey(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            player.sendSystemMessage(Component.literal("  Total Multipliers:").withStyle(ChatFormatting.LIGHT_PURPLE));
            for (AttributeModifier mod : grouped.get(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
                displayModifier(player, mod, attrHolder);
            }
        }

        return 1;
    }

    private static void displayModifier(LocalPlayer player, AttributeModifier mod, Attribute attrHolder) {
        String opText = formatModifier(mod);
        String source = guessSource(mod, player, attrHolder);

        player.sendSystemMessage(
                Component.literal("    • " + opText)
                        .append(Component.literal(" - " + source).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" (ID: " + ModifierIds.of(mod) + ")").withStyle(ChatFormatting.DARK_GRAY))
                        .withStyle(ChatFormatting.WHITE)
        );
    }

    private static String guessSource(AttributeModifier mod, LocalPlayer player, Attribute attrHolder) {
        ResourceLocation modId = ModifierIds.of(mod);
        String namespace = modId.getNamespace();
        String path = modId.getPath();

        if (TrinketsCompat.isLoaded()) {
            var trinketMods = TrinketsCompat.getTrinketModifierSources(player);
            for (var source : trinketMods) {
                if (source.attribute().equals(attrHolder) && source.modifier().getId().equals(mod.getId())) {
                    return "Trinket:  " + source.stack().getHoverName().getString() + " [" + source.slotType() + "]";
                }
            }
        }

        for (var effectInstance : player.getActiveEffects()) {
            var effect = effectInstance.getEffect();
            if (path.contains("effect") || path.contains(effect.getDescriptionId())) {
                return "Effect: " + Component.translatable(effect.getDescriptionId()).getString();
            }
        }

        return switch (namespace) {
            case "minecraft" -> "Vanilla";
            case "tiered" -> "Tiered Item";
            case "puffish_skills" -> "Puffish Skills";
            case "rpg-systems" -> "RPG Systems";
            case "reskillable" -> "Reskillable";
            case "dungeon_difficulty" -> "Dungeon Difficulty";
            case "morequesttypes" -> "Quest Reward";
            default -> "Mod: " + namespace;
        };
    }

    private static String formatModifier(AttributeModifier mod) {
        return switch (mod.getOperation()) {
            case ADDITION -> String.format("%+.2f", mod.getAmount());
            case MULTIPLY_BASE -> String.format("%+d%% Base", (int) (mod.getAmount() * 100));
            case MULTIPLY_TOTAL -> String.format("%+d%% Total", (int) (mod.getAmount() * 100));
        };
    }
}