package net.pixeldreamstudios.attributepanel.command.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;
import net.pixeldreamstudios.attributepanel.network.NetworkManager;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;

import java.util.*;

public class AttributeSnapshotCommandImpl {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AttributeSnapshotCommandImpl::onRegisterCommands);
    }

    public static void registerAttributeCheck() {
        NeoForge.EVENT_BUS.addListener(AttributeSnapshotCommandImpl::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterClientCommandsEvent event) {
        registerCommand(event.getDispatcher());
        registerAttributeCheckCommand(event.getDispatcher());
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("attributesnapshot")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "target")))
                        )
        );
    }

    private static void registerAttributeCheckCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        SuggestionProvider<CommandSourceStack> attributeSuggestions = (context, builder) -> {
            return SharedSuggestionProvider.suggestResource(
                    BuiltInRegistries.ATTRIBUTE.keySet(),
                    builder
            );
        };

        dispatcher.register(
                Commands.literal("attributepanel")
                        .then(Commands.argument("attribute", StringArgumentType.greedyString())
                                .suggests(attributeSuggestions)
                                .executes(ctx -> executeAttributeCheck(ctx, StringArgumentType.getString(ctx, "attribute")))
                        )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String targetName) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return 0;
        }

        RequestAttributeSnapshotPayload payload = new RequestAttributeSnapshotPayload(targetName);
        NetworkManager.sendToServer(payload);

        player.displayClientMessage(
                Component.literal("Requested attribute snapshot for: " + targetName)
                        .withStyle(ChatFormatting.GRAY)
        , false);

        return 1;
    }

    private static int executeAttributeCheck(CommandContext<CommandSourceStack> ctx, String attributeId) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return 0;
        }

        Identifier attrResourceLocation;
        try {
            attrResourceLocation = Identifier.parse(attributeId);
        } catch (Exception e) {
            player.displayClientMessage(
                    Component.literal("Invalid attribute ID:  " + attributeId)
                            .withStyle(ChatFormatting.RED)
            , false);
            return 0;
        }

        Holder.Reference<Attribute> attrHolder = BuiltInRegistries.ATTRIBUTE.get(attrResourceLocation).orElse(null);
        if (attrHolder == null) {
            player.displayClientMessage(
                    Component.literal("Attribute not found: " + attributeId)
                            .withStyle(ChatFormatting.RED)
            , false);
            return 0;
        }

        Attribute attribute = attrHolder.value();
        AttributeInstance instance = player.getAttribute(attrHolder);

        if (instance == null) {
            player.displayClientMessage(
                    Component.literal("You don't have this attribute: " + attributeId)
                            .withStyle(ChatFormatting.RED)
            , false);
            return 0;
        }

        player.displayClientMessage(
                Component.literal("=== Modifiers for: ")
                        .append(Component.translatable(attribute.getDescriptionId()))
                        .append(" ===")
                        .withStyle(ChatFormatting.GOLD)
        , false);

        player.displayClientMessage(
                Component.literal("Base Value: " + String.format("%.2f", instance.getBaseValue()))
                        .withStyle(ChatFormatting.YELLOW)
        , false);
        player.displayClientMessage(
                Component.literal("Final Value: " + String.format("%.2f", instance.getValue()))
                        .withStyle(ChatFormatting.AQUA)
        , false);

        Collection<AttributeModifier> modifiers = instance.getModifiers();

        if (modifiers.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("No modifiers found.")
                            .withStyle(ChatFormatting.GRAY)
            , false);
            return 1;
        }

        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(Component.literal("Modifiers:").withStyle(ChatFormatting.YELLOW), false);

        Map<AttributeModifier.Operation, List<AttributeModifier>> grouped = new HashMap<>();
        for (AttributeModifier mod : modifiers) {
            grouped.computeIfAbsent(mod.operation(), k -> new ArrayList<>()).add(mod);
        }

        if (grouped.containsKey(AttributeModifier.Operation.ADD_VALUE)) {
            player.displayClientMessage(Component.literal("  Flat Additions:").withStyle(ChatFormatting.GREEN), false);
            for (AttributeModifier mod : grouped.get(AttributeModifier.Operation.ADD_VALUE)) {
                displayModifier(player, mod, attrHolder);
            }
        }

        if (grouped.containsKey(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
            player.displayClientMessage(Component.literal("  Base Multipliers:").withStyle(ChatFormatting.BLUE), false);
            for (AttributeModifier mod : grouped.get(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
                displayModifier(player, mod, attrHolder);
            }
        }

        if (grouped.containsKey(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
            player.displayClientMessage(Component.literal("  Total Multipliers:").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            for (AttributeModifier mod :  grouped.get(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
                displayModifier(player, mod, attrHolder);
            }
        }

        return 1;
    }

    private static void displayModifier(LocalPlayer player, AttributeModifier mod, Holder<Attribute> attrHolder) {
        String opText = formatModifier(mod);
        String source = guessSource(mod.id(), player, attrHolder);

        player.displayClientMessage(
                Component.literal("    • " + opText)
                        .append(Component.literal(" - " + source).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" (ID: " + mod.id() + ")").withStyle(ChatFormatting.DARK_GRAY))
                        .withStyle(ChatFormatting.WHITE)
        , false);
    }

    private static String guessSource(Identifier modId, LocalPlayer player, Holder<Attribute> attrHolder) {
        String namespace = modId.getNamespace();
        String path = modId.getPath();

        if (CuriosCompat.isLoaded()) {
            var curioMods = CuriosCompat.getCurioModifierSources(player);
            for (var source : curioMods) {
                if (source.attribute().equals(attrHolder) && source.modifier().id().equals(modId)) {
                    return "Curio: " + source.stack().getHoverName().getString() + " [" + source.slotType() + "]";
                }
            }
        }

        for (var effectInstance : player.getActiveEffects()) {
            var effect = effectInstance.getEffect().value();
            if (path.contains("effect") || path.contains(effect.getDescriptionId())) {
                return "Effect:  " + Component.translatable(effect.getDescriptionId()).getString();
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
        return switch (mod.operation()) {
            case ADD_VALUE -> String.format("%+.2f", mod.amount());
            case ADD_MULTIPLIED_BASE -> String.format("%+d%% Base", (int) (mod.amount() * 100));
            case ADD_MULTIPLIED_TOTAL -> String.format("%+d%% Total", (int) (mod.amount() * 100));
        };
    }
}