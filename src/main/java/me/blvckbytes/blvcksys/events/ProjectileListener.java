package me.blvckbytes.blvcksys.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.IAnimationHandler;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/31/2022

  Listens for projectile launches and then tracks and plays trailing
  effects on arrows shot by players.
*/
@AutoConstruct
public class ProjectileListener implements IAutoConstructed, Listener {

  // Maximum time of life of an arrow in milliseconds
  private static final long ARROW_TOL_MS = 30 * 1000;

  // Size of the pre-alloced last location buffer
  private static final int ARROW_POS_BUF = 50;

  // Distance between trail pixels, size of trail pixels
  private static final float TRAIL_PIXEL_DIST = 0.06F, TRAIL_PIXEL_SIZE = 0.8F;

  // initial delay distance in blocks squared, so that the user doesn't
  // get a huge load of particles into their face when shooting
  private static final double INITIAL_DEL_BLOCKS_SQ = Math.pow(1, 2);

  /**
   * Holds all properties a trailing arrow has
   */
  @Getter
  @RequiredArgsConstructor
  private static class EffectArrow {
    private final Arrow arrow;
    private final Location start;
    private final long spawnStamp;
    private final Particle particle;
    private final Color color;

    @Setter
    private boolean trails = false;
  }

  private final List<EffectArrow> arrows;
  private final Location[] arrowLastLoc;
  private final JavaPlugin plugin;
  private final IPreferencesHandler preferencesHandler;
  private BukkitTask tickerHandle;

  public ProjectileListener(
    @AutoInject IAnimationHandler animationHandler,
    @AutoInject IPreferencesHandler preferencesHandler,
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;
    this.preferencesHandler = preferencesHandler;

    this.arrowLastLoc = new Location[ARROW_POS_BUF];
    this.arrows = new ArrayList<>();
  }

  @EventHandler
  public void onLaunch(ProjectileLaunchEvent e) {
    // Not an arrow
    if (!(e.getEntity() instanceof Arrow a))
      return;

    // Not launched by a player
    if (!(a.getShooter() instanceof Player p))
      return;

    // Load the player's trail preference
    Tuple<@Nullable Particle, @Nullable Color> effect = preferencesHandler.getArrowTrail(p);
    Particle part = effect.a();

    // Has trailing effects disabled
    if (part == null)
      return;

    if (part.getDataType() != Void.class) {
      if (part.getDataType() != Particle.DustOptions.class) {
        // Unsupported particle effect
        return;
      }

      // This effect requires a color, but none is set yet
      else if (effect.b() == null)
        return;
    }

    // Initially store the last location and register this new arrow
    arrowLastLoc[a.getEntityId() % ARROW_POS_BUF] = a.getLocation();
    arrows.add(new EffectArrow(a, a.getLocation(), System.currentTimeMillis(), effect.a(), effect.b()));
  }

  @Override
  public void cleanup() {
    if (tickerHandle != null)
      tickerHandle.cancel();
  }

  @Override
  public void initialize() {
    tickerHandle = Bukkit.getScheduler().runTaskTimer(plugin, this::tickArrows, 0L, 1L);
  }

  /**
   * Tick all currently tracked arrows and play their assigned trail particle effects
   */
  private void tickArrows() {
    for (Iterator<EffectArrow> eaI = arrows.iterator(); eaI.hasNext();) {
      EffectArrow ea = eaI.next();
      Arrow arrow = ea.getArrow();

      if (
        // Arrow died (fell out of the world, for exampke)
        arrow.isDead() ||

        // Hit a block
        arrow.isOnGround() ||
        arrow.isInBlock() ||

        // Exceeded it's time of life
        System.currentTimeMillis() > ea.getSpawnStamp() + ARROW_TOL_MS
      ) {
        arrow.remove();
        eaI.remove();
        continue;
      }

      // Retrieve and store the last location, then calculate the path the arrow has moved between
      // last tick and this tick, to then loop that vector and thus get more granular control
      // over the effect particle distance
      int bufId = arrow.getEntityId() % ARROW_POS_BUF;
      Location lastLoc = arrowLastLoc[bufId];
      arrowLastLoc[bufId] = arrow.getLocation();
      Vector movePath = arrow.getLocation().toVector().subtract(lastLoc.toVector());

      // Vector doesn't trail yet (initial delay)
      if (!ea.isTrails()) {
        // Didn't yet travel far enough
        if (ea.getStart().distanceSquared(arrow.getLocation()) < INITIAL_DEL_BLOCKS_SQ)
          continue;

        // Set the last location to the exact point where the initial
        // delay would have expired to account for overshoots
        lastLoc = ea.getStart().clone().add(movePath.normalize().multiply(Math.sqrt(INITIAL_DEL_BLOCKS_SQ)));
        ea.setTrails(true);
      }

      // Decide on a step size to exactly end up at the desired trail distance
      double length = movePath.length();
      double step = TRAIL_PIXEL_DIST / length;

      // Walk the move-path vector
      double playX = lastLoc.getX(), playY = lastLoc.getY(), playZ = lastLoc.getZ();
      for (double i = 0; i <= 1; i += step) {
        arrow.getWorld().spawnParticle(
          ea.getParticle(),
          playX + movePath.getX() * i,
          playY + movePath.getY() * i,
          playZ + movePath.getZ() * i,
          1,
          ea.color == null ? null : new Particle.DustOptions(ea.color, TRAIL_PIXEL_SIZE)
        );
      }
    }
  }
}
