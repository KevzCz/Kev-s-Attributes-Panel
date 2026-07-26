package net.pixeldreamstudios.attributepanel.command.forge;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.pixeldreamstudios.attributepanel.util.ModifierIds;
import net.pixeldreamstudios.attributepanel.compat.CuriosCompat;
import net.pixeldreamstudios.attributepanel.network.NetworkManager;
import net.pixeldreamstudios.attributepanel.network.payload.RequestAttributeSnapshotPayload;

import java.util.*;

public class AttributeSnapshotCommandImpl {

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(AttributeSnapshotCommandImpl::onRegisterCommands);
    }

    public static void registerAttributeCheck() {
        MinecraftForge.EVENT_BUS.addListener(AttributeSnapshotCommandImpl::onRegisterCommands);
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

        player.sendSystemMessage(
                Component.literal("Requested attribute snapshot for: " + targetName)
                        .withStyle(ChatFormatting.GRAY)
        );

        return 1;
    }

    private static int executeAttributeCheck(CommandContext<CommandSourceStack> ctx, String attributeId) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return 0;
        }

        ResourceLocation attrResourceLocation;
        try {
            attrResourceLocation = ResourceLocation.tryParse(attributeId);
        } catch (Exception e) {
            player.sendSystemMessage(
                    Component.literal("Invalid attribute ID:  " + attributeId)
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
                    Component.literal("You don't have this attribute: " + attributeId)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        player.sendSystemMessage(
                Component.literal("=== Modifiers for: ")
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
            for (AttributeModifier mod :  grouped.get(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
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

        if (CuriosCompat.isLoaded()) {
            var curioMods = CuriosCompat.getCurioModifierSources(player);
            for (var source : curioMods) {
                if (source.attribute().equals(attrHolder) && source.modifier().getId().equals(mod.getId())) {
                    return "Curio: " + source.stack().getHoverName().getString() + " [" + source.slotType() + "]";
                }
            }
        }

        for (var effectInstance : player.getActiveEffects()) {
            var effect = effectInstance.getEffect();
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
        return switch (mod.getOperation()) {
            case ADDITION -> String.format("%+.2f", mod.getAmount());
            case MULTIPLY_BASE -> String.format("%+d%% Base", (int) (mod.getAmount() * 100));
            case MULTIPLY_TOTAL -> String.format("%+d%% Total", (int) (mod.getAmount() * 100));
        };
    }
}