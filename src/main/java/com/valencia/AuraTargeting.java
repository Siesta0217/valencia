package com.valencia;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Shared targeting predicate for the combat auras (KillAura / MaceAura /
 * SpearAura). The faction gate below was copy-pasted verbatim into all three
 * {@code findTarget()} loops; this is the single source of truth.
 *
 * The auras keep their own static state (enabled flag, glowTarget, saved
 * rotations) because they are static-utility classes accessed directly from
 * their mixins — converting them to instances to share that state would ripple
 * through every call site for no behavioural gain, so only the genuinely
 * stateless logic is shared here. Reach / line-of-sight / smooth-rotation
 * helpers already live as static methods on {@link KillAuraMod}.
 */
public final class AuraTargeting {

    private AuraTargeting() {}

    /**
     * True if {@code e}'s faction is enabled for targeting. Mirrors the exact
     * four checks the auras used inline: a player/hostile/animal is allowed
     * only when its flag is set, and anything in none of those categories is
     * rejected.
     */
    public static boolean factionAllowed(Entity e, boolean players, boolean hostile, boolean animals) {
        boolean isPlayer  = e instanceof Player;
        boolean isHostile = e instanceof Enemy;
        boolean isAnimal  = e instanceof Animal;
        if (isPlayer  && !players) return false;
        if (isHostile && !hostile) return false;
        if (isAnimal  && !animals) return false;
        return isPlayer || isHostile || isAnimal;
    }
}
