package com.solegendary.reignofnether.util;

import java.util.Comparator;
import java.util.Objects;

import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * 1.20.1's Forge {@code ChunkTicketManager.forceChunk(level, modId, owner, x, z, add, ticking)}
 * kept a per-owner ticket so a chunk stayed loaded while any of its owners did. NeoForge 21.1
 * dropped that helper in favour of the vanilla region-ticket API, so the per-owner bookkeeping
 * lives here: one ticket type per ticking flag, keyed on the owner object.
 *
 * <p>Vanilla {@code DistanceManager#addRegionTicket} takes a level in {@code FullChunkStatus}
 * units ({@code ENTITY_TICKING} < {@code BLOCK_TICKING} < {@code FULL}), so a ticking=false ticket
 * simply asks for the weaker status — the same distinction the old {@code ticking} flag made.
 */
public final class ChunkTicketUtil {
	private ChunkTicketUtil() {}

	private static final Comparator<Object> OWNER_ORDER =
			(a, b) -> Integer.compare(ownerOrder(a), ownerOrder(b));

	/** Owner-keyed ticket that keeps the chunk loaded and block-ticking. */
	private static final TicketType<Object> OWNER_BLOCK_TICKING =
			TicketType.create(ReignOfNether.MOD_ID + "_owner_block_ticking", OWNER_ORDER);

	/** Owner-keyed ticket that keeps the chunk fully ticking, entities included. */
	private static final TicketType<Object> OWNER_ENTITY_TICKING =
			TicketType.create(ReignOfNether.MOD_ID + "_owner_entity_ticking", OWNER_ORDER);

	/**
	 * TicketType uses the comparator to decide whether a re-added ticket is the same ticket, so
	 * owners of the same chunk must compare equal. Identity hash is that key here.
	 */
	private static int ownerOrder(Object owner) {
		return System.identityHashCode(Objects.requireNonNull(owner, "owner"));
	}

	/**
	 * Forcibly loads (or releases) the chunk containing {@code chunkX}/{@code chunkZ} on behalf of
	 * {@code owner}.
	 *
	 * @param owner   the object holding the chunk; use the entity or placement itself
	 * @param ticking whether the chunk must keep ticking entities, not just blocks
	 */
	public static void forceChunk(ServerLevel level, Object owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
		ChunkPos pos = new ChunkPos(chunkX, chunkZ);
		FullChunkStatus status = ticking ? FullChunkStatus.FULL : FullChunkStatus.BLOCK_TICKING;
		TicketType<Object> type = ticking ? OWNER_ENTITY_TICKING : OWNER_BLOCK_TICKING;
		int ticketLevel = ChunkLevel.byStatus(status);
		if (add) {
			level.getChunkSource().addRegionTicket(type, pos, ticketLevel, owner, true);
		} else {
			level.getChunkSource().removeRegionTicket(type, pos, ticketLevel, owner, true);
		}
	}
}
