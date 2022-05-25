package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.handlers.MultilineHologram;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/26/2022

  Listens to damage done by a player to another player and spawns a temporary
  hologram which displays information about the damage done.
*/
@AutoConstruct
public class PlayerDamageListener implements Listener {

  // Used for hologram vector generation
  private static final Random rand = new Random();

  // Offset in blocks added to the victim's location when spawning holograms
  private static final double LOCATION_YOFF = 2;

  // Range of the x and z axis used when generating a new vector
  private static final double VECTOR_XZ_RANGE = 0.25D;

  // Number of hologram vectors to prepare beforehand for the ringbuffer
  private static final int NUM_VECTORS = 20;

  private final IHologramHandler holos;
  private final IConfig cfg;

  private final Vector[] hologramVectors;
  private int hologramVectorsInd;

  public PlayerDamageListener(
    @AutoInject IHologramHandler holos,
    @AutoInject IConfig cfg
  ) {
    this.holos = holos;
    this.cfg = cfg;

    this.hologramVectors = new Vector[NUM_VECTORS];

    for (int i = 0; i < NUM_VECTORS; i++)
      hologramVectors[i] = generateVector();
  }

  //=========================================================================//
  //                                  Listener                               //
  //=========================================================================//

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof Player victim))
      return;

    if (!(e.getDamager() instanceof Player damager))
      return;

    spawnDamageIndicator(victim, damager, e.getDamage());
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Spawn a new damage indicating temporary hologram
   * @param victim Player that has taken damage
   * @param damager Player that has been damaged
   * @param damage Amount of damage done
   */
  private void spawnDamageIndicator(Player victim, Player damager, double damage) {
    MultilineHologram holo = holos.createTemporary(
      victim.getLocation().add(0, LOCATION_YOFF, 0),
      cfg.get(isCriticallyDamaging(damager) ? ConfigKey.DAMAGE_INDICATORS_CRITICAL : ConfigKey.DAMAGE_INDICATORS_NORMAL)
        .withVariable("damage", Math.round(damage * 100) / 100)
        .asList()
    );
    holo.setVelocity(getNextVector(), () -> holos.destroyTemporary(holo));
  }

  /**
   * Checks whether a player is currently causing critical damage when fighting
   * @param damager Damaging player
   */
  private boolean isCriticallyDamaging(Player damager) {
    Material standingOn = damager.getLocation().getBlock().getType();

    return (
      damager.getFallDistance() > 0.0F &&
      !((LivingEntity) damager).isOnGround() &&
      !standingOn.equals(Material.VINE) &&
      !standingOn.equals(Material.LADDER) &&
      !standingOn.equals(Material.WATER) &&
      !standingOn.equals(Material.LAVA) &&
      !damager.hasPotionEffect(PotionEffectType.BLINDNESS) &&
      damager.getVehicle() == null
    );
  }

  /**
   * Get the next usable hologram vector from the ringbuffer
   */
  private Vector getNextVector() {
    Vector next = hologramVectors[hologramVectorsInd];

    if (++hologramVectorsInd == NUM_VECTORS)
      hologramVectorsInd = 0;

    return next;
  }

  /**
   * Generates a random vector within range to be used with holograms
   */
  private Vector generateVector() {
    double genX = rand.nextDouble(VECTOR_XZ_RANGE * 2);
    double genZ = rand.nextDouble(VECTOR_XZ_RANGE * 2);

    return new Vector(
      (genX > VECTOR_XZ_RANGE ? -genX : genX) / 2,
      0.3D,
      (genZ > VECTOR_XZ_RANGE ? -genZ : genZ) / 2
    );
  }
}
