package com.solegendary.reignofnether.research;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * GM/testing commands for the research system: grant, revoke, clear and list a player's researches.
 */
public class ResearchCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("research")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(ctx -> set(ctx, true)))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(ctx -> set(ctx, false)))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ResearchCommand::clear)))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ResearchCommand::list)))
        );
    }

    private static int set(CommandContext<CommandSourceStack> ctx, boolean grant) {
        String playerName = StringArgumentType.getString(ctx, "player");
        ResourceLocation id = ResourceLocation.tryParse(StringArgumentType.getString(ctx, "id"));
        if (id == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid research id"));
            return 0;
        }
        ResearchSaveData data = ResearchSaveData.getInstance(ctx.getSource().getLevel());
        if (grant) {
            data.grant(playerName, id);
            if (!ResearchRegistry.exists(id))
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "Warning: no registered research '" + id + "' (granted anyway)"), false);
        } else {
            data.revoke(playerName, id);
        }
        data.save();
        ResearchClientboundPacket.sync(playerName, data.getFor(playerName));
        ResearchAttributeApplier.refreshForOwner(ctx.getSource().getLevel(), playerName);
        ResearchEquipApplier.refreshForOwner(ctx.getSource().getLevel(), playerName);
        ctx.getSource().sendSuccess(() -> Component.literal(
                (grant ? "Granted " : "Revoked ") + id + " for " + playerName), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        ResearchSaveData data = ResearchSaveData.getInstance(ctx.getSource().getLevel());
        data.clear(playerName);
        data.save();
        ResearchClientboundPacket.sync(playerName, Set.of());
        ResearchAttributeApplier.refreshForOwner(ctx.getSource().getLevel(), playerName);
        ResearchEquipApplier.refreshForOwner(ctx.getSource().getLevel(), playerName);
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared research for " + playerName), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Set<ResourceLocation> ids = ResearchSaveData.getInstance(ctx.getSource().getLevel()).getFor(playerName);
        ctx.getSource().sendSuccess(() -> Component.literal(
                playerName + " researched: " + (ids.isEmpty() ? "(none)" : ids)), false);
        return 1;
    }
}
