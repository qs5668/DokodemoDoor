package top.midream.ddoor.visual;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Event-driven feedback: pair-success beam, teleport gather/burst, sounds.
 * Particles are sent per-recipient within range instead of world-wide broadcast.
 */
public final class Fx {

    private Fx() {}

    private static final int RANGE = 32;

    public static void pairSuccess(Location a, Location b, boolean sound) {
        if (a == null || b == null) return;
        beam(a);
        beam(b);
        if (sound) {
            chime(a);
            chime(b);
        }
    }

    private static void beam(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) > RANGE * RANGE) continue;
            p.spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 0, 0.5), 40, 0.35, 1.2, 0.35, 0.06);
            p.spawnParticle(Particle.PORTAL, loc.clone().add(0.5, 1, 0.5), 60, 0.5, 1.2, 0.5, 0.4);
        }
    }

    private static void chime(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.playSound(loc, Sound.ENTITY_ALLAY_ITEM_THROWN, 1f, 1.2f);
        world.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
    }

    public static void pendingSelect(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
    }

    /** Particle spiral collapsing into the door frame at teleport time. */
    public static void tpGather(Location doorLoc) {
        if (doorLoc == null) return;
        World world = doorLoc.getWorld();
        if (world == null) return;
        Location center = doorLoc.clone().add(0.5, 1, 0.5);
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(center) > RANGE * RANGE) continue;
            p.spawnParticle(Particle.PORTAL, center, 50, 0.8, 1.2, 0.8, 0.5);
            p.spawnParticle(Particle.END_ROD, center, 8, 0.2, 0.9, 0.2, 0.02);
        }
        world.playSound(center, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 0.7f);
    }

    /** Arrival burst at the destination door. */
    public static void tpArrive(Location dstLoc) {
        if (dstLoc == null) return;
        World world = dstLoc.getWorld();
        if (world == null) return;
        Location center = dstLoc.clone().add(0.5, 1, 0.5);
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(center) > RANGE * RANGE) continue;
            p.spawnParticle(Particle.PORTAL, center, 70, 0.5, 1.1, 0.5, 0.45);
            p.spawnParticle(Particle.END_ROD, center, 12, 0.4, 0.8, 0.4, 0.05);
        }
        world.playSound(center, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1.3f);
    }
}
