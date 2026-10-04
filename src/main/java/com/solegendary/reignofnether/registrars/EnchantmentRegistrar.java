package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Handles of the mod's seven enchantments.
 *
 * <p>On 1.20.1 these were Java classes extending {@code Enchantment} and registered through a
 * {@code DeferredRegister}. 1.21.1 made {@code Enchantment} a final record and moved its
 * definition into data, so the enchantments now live in
 * {@code data/reignofnether/enchantment/*.json} and this class only hands out the registry
 * holders.
 *
 * <p>That has one consequence worth spelling out: the enchantment registry is a <em>datapack</em>
 * registry, so unlike blocks or items it has no field on {@code BuiltInRegistries} - it only
 * exists once the server has built a {@code RegistryAccess} for it. The registry object itself is
 * captured from {@code ModifyRegistriesEvent} (which fires on both logical sides), and the holders
 * are looked up lazily and memoised on first use.
 *
 * <p>Because of that, the seven mod enchantments are {@link Supplier}s rather than plain fields -
 * touching one before the registry exists would fail. Call sites read {@code .get()}, which is the
 * same shape the pre-port code had.
 *
 * <p>Vanilla's own enchantments moved the other way: {@code Enchantments.SHARPNESS} and friends are
 * now {@link ResourceKey}s, so {@link #vanilla(ResourceKey)} is the bridge from a key to the holder
 * that {@code ItemStack#getEnchantmentLevel} and friends want.
 */
public class EnchantmentRegistrar {

    private static final Map<ResourceKey<Enchantment>, Holder<Enchantment>> CACHE =
            new ConcurrentHashMap<>();

    private static volatile Registry<Enchantment> registry;

    public static final Supplier<Holder<Enchantment>> VIGOR = mod("vigor");
    public static final Supplier<Holder<Enchantment>> BREACHING = mod("breaching");
    public static final Supplier<Holder<Enchantment>> FORTYIFYING = mod("fortifying");
    public static final Supplier<Holder<Enchantment>> MAIMING = mod("maiming");
    public static final Supplier<Holder<Enchantment>> ZEAL = mod("zeal");
    public static final Supplier<Holder<Enchantment>> GUST = mod("gust");
    public static final Supplier<Holder<Enchantment>> LONGSHOT = mod("longshot");

    private EnchantmentRegistrar() { }

    /**
     * Captures the enchantment registry from a {@code RegistryAccess} that has it built.
     *
     * <p>Single player runs both logical sides in one JVM, and they have <em>separate</em>
     * registry accesses. Binding unconditionally let the client's access overwrite the server's,
     * after which server-side code handed out holders bound to the client's registry; saving such an
     * item then failed with "Element Reference{...} is not valid in current registry set". So the
     * first binding wins - the server binds on {@code ServerStartingEvent}, before the client logs
     * in - and a later binding from a different access is ignored.
     *
     * <p>A dedicated client never runs a server, so nothing is bound there until it joins a world,
     * and {@link #tryVanilla} keeps callers that only want a cosmetic holder working.
     */
    public static void bind(RegistryAccess access) {
        if (registry != null) return;
        registry = access.registryOrThrow(Registries.ENCHANTMENT);
    }

    /**
     * Resolves one of vanilla's {@code Enchantments.*} keys to its holder.
     *
     * <p>A few vanilla enchantments were also renamed on the way: {@code POWER_ARROWS} is now
     * {@code POWER}, {@code PUNCH_ARROWS} is {@code PUNCH}, {@code FLAMING_ARROWS} is
     * {@code FLAME}, {@code MOB_LOOTING} is {@code LOOTING} and {@code BLOCK_EFFICIENCY} is
     * {@code EFFICIENCY}.
     */
    public static Holder<Enchantment> vanilla(ResourceKey<Enchantment> key) {
        return holder(key);
    }

    /**
     * Like {@link #vanilla(ResourceKey)} but null instead of throwing when no world exists yet.
     *
     * <p>Client-only: the title screen renders before a level is joined, and the client has no
     * enchantment registry at that point, so a caller that only wants the key for a cosmetic effect
     * (an enchanted-looking item, say) has to be able to skip it.
     */
    public static @Nullable Holder<Enchantment> tryVanilla(ResourceKey<Enchantment> key) {
        Registry<Enchantment> reg = registry;
        if (reg == null) return null;
        Holder<Enchantment> cached = CACHE.get(key);
        return cached != null ? cached : reg.getHolderOrThrow(key);
    }

    /** Kept so the mod constructor stays unchanged; there is nothing to register any more. */
    public static void init(net.neoforged.fml.ModContainer container) { }

    private static Supplier<Holder<Enchantment>> mod(String path) {
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path));
        return () -> holder(key);
    }

    private static Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
        Holder<Enchantment> cached = CACHE.get(key);
        if (cached != null) return cached;

        Registry<Enchantment> reg = registry;
        if (reg == null)
            throw new IllegalStateException(
                    "Enchantment " + key.location() + " was requested before the enchantment registry"
                            + " was bound; bind(HolderLookup.Provider) has to run first, and it only"
                            + " can once a world with datapacks exists");

        Holder<Enchantment> resolved = reg.getHolderOrThrow(key);
        CACHE.put(key, resolved);
        return resolved;
    }
}
