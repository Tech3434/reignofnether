package com.solegendary.reignofnether.registrars;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves enchantment holders against the datapack enchantment registry.
 *
 * <p>The mod's own seven enchantments (vigor, breaching, fortifying, maiming, zeal, gust, longshot)
 * were removed with stage D: {@code Enchantment} became a final record on 1.21.1 and their behaviour
 * lived in Java effect classes, so porting them would mean rewriting all seven. Their data files,
 * recipes, lang entries and every call site are gone with them.
 *
 * <p>What remains is the bridge to <em>vanilla</em> enchantments. Those moved from classes to
 * {@link ResourceKey}s ({@code Enchantments.SHARPNESS} and friends), and {@link #vanilla(ResourceKey)}
 * turns one into the holder that {@code ItemStack#getEnchantmentLevel} and {@link EnchantmentUtil}
 * want. {@link EnchantmentUtil} is the preferred entry point - it keeps working when nothing is bound.
 *
 * <p>The registry itself is a <em>datapack</em> registry: unlike blocks or items it has no field on
 * {@code BuiltInRegistries}, so it only exists once a {@code RegistryAccess} has been built for it.
 * That is why {@link #vanilla(ResourceKey)} throws when called too early - see
 * {@link EnchantmentUtil}, which returns 0/empty instead.
 */
public class EnchantmentRegistrar {

    private static final Map<ResourceKey<Enchantment>, Holder<Enchantment>> CACHE =
            new ConcurrentHashMap<>();

    private static volatile Registry<Enchantment> registry;

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
     * <p>Prefer {@link EnchantmentUtil}, which degrades to 0/empty instead of throwing when called
     * before a world exists.
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

    private static Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
        Holder<Enchantment> cached = CACHE.get(key);
        if (cached != null) return cached;

        Registry<Enchantment> reg = registry;
        if (reg == null)
            throw new IllegalStateException(
                    "Enchantment " + key.location() + " was requested before the enchantment registry"
                            + " was bound; bind(RegistryAccess) has to run first, and it only can once"
                            + " a world with datapacks exists");

        Holder<Enchantment> resolved = reg.getHolderOrThrow(key);
        CACHE.put(key, resolved);
        return resolved;
    }
}
