package com.solegendary.reignofnether.faction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.solegendary.reignofnether.player.PlayerServerEvents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /startrts [<faction>]}: opens the faction-selection menu for the caller. With no faction the
 * whole list is selectable; with a faction named, only that one is, the rest are shown but disabled.
 */
public class FactionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("startrts")
                .executes(ctx -> open(ctx, null))
                .then(Commands.argument("faction", ResourceLocationArgument.id())
                        .executes(ctx -> open(ctx, ResourceLocationArgument.getId(ctx, "faction")))));
    }

    private static int open(CommandContext<CommandSourceStack> ctx, ResourceLocation factionId) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only a player can start an RTS match"));
            return 0;
        }
        if (factionId != null && FactionRegistries.get(ctx.getSource().getServer(), factionId) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown faction: " + factionId));
            return 0;
        }
        if (!PlayerServerEvents.hasRTSPass(player)) {
            return 0; // the pass is required to enter; without it nothing happens
        }
        FactionClientboundPacket.open(player, factionId);
        return 1;
    }
}
