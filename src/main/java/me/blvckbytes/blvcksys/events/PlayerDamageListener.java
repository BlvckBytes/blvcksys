package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.handlers.MultilineHologram;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/26/2022

  Listens to damage done by a player to another player and spawns a temporary
  hologram which displays information about the damage done. When a player is being
  damaged by another player, blood particles are displayed, where the amount of
  particles is directly proportional to the damage done. Also modifies vanilla death
  messages to very detailled config values.
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
  private static final int NUM_HOLO_VECTORS = 20;

  // How many blood particles to spawn at what damage level, using a linear correlation
  private static final int BLOOD_PARTICLES_MIN = 8, BLOOD_PARTICLES_MAX = 32;
  private static final double BLOOD_HEALTH_MIN = 1.0, BLOOD_HEALTH_MAX = 20.0;

  private final ChatListener chat;
  private final IHologramHandler holos;
  private final IConfig cfg;

  private final Vector[] hologramVectors, bloodVectors;
  private int hologramVectorsInd, bloodVectorsInd;

  public PlayerDamageListener(
    @AutoInject IHologramHandler holos,
    @AutoInject IConfig cfg,
    @AutoInject ChatListener chat
  ) {
    this.holos = holos;
    this.cfg = cfg;
    this.chat = chat;

    this.hologramVectors = new Vector[NUM_HOLO_VECTORS];
    this.bloodVectors = new Vector[BLOOD_PARTICLES_MAX];

    for (int i = 0; i < NUM_HOLO_VECTORS; i++)
      hologramVectors[i] = generateVector(VECTOR_XZ_RANGE, 0.3, 0.3);

    for (int i = 0; i < BLOOD_PARTICLES_MAX; i++)
      bloodVectors[i] = generateVector(.5, -1, 1.5);
  }

  //=========================================================================//
  //                                  Listener                               //
  //=========================================================================//

  @EventHandler
  public void onDamageByAny(EntityDamageEvent e) {
    // Didn't affect a player
    if (!(e.getEntity() instanceof Player p))
      return;

    // Didn't kill them
    if (p.getHealth() > e.getDamage())
      return;

    // Decide on what death message to print
    ConfigKey deathMessage = switch (e.getCause()) {
      case BLOCK_EXPLOSION -> ConfigKey.DEATH_MESSAGES_BLOCK_EXPLOSION;
      case FALL -> ConfigKey.DEATH_MESSAGES_FALL;
      case FALLING_BLOCK -> ConfigKey.DEATH_MESSAGES_FALLING_BLOCK;
      case CONTACT -> ConfigKey.DEATH_MESSAGES_BLOCK_CONTACT;
      case CRAMMING -> ConfigKey.DEATH_MESSAGES_TRAMPLED;
      case DRAGON_BREATH -> ConfigKey.DEATH_MESSAGES_DRAGON;
      case DROWNING -> ConfigKey.DEATH_MESSAGES_DROWNED;
      case FIRE -> ConfigKey.DEATH_MESSAGES_IN_FIRE;
      case FIRE_TICK -> ConfigKey.DEATH_MESSAGES_FIRE;
      case FLY_INTO_WALL -> ConfigKey.DEATH_MESSAGES_WALL;
      case FREEZE -> ConfigKey.DEATH_MESSAGES_FREEZE;
      case HOT_FLOOR -> ConfigKey.DEATH_MESSAGES_MAGMA;
      case LAVA -> ConfigKey.DEATH_MESSAGES_LAVA;
      case LIGHTNING -> ConfigKey.DEATH_MESSAGES_LIGHTNING;
      case MAGIC -> ConfigKey.DEATH_MESSAGES_MAGIC;
      case POISON -> ConfigKey.DEATH_MESSAGES_POISON;
      case PROJECTILE -> ConfigKey.DEATH_MESSAGES_PROJECTILE;
      case STARVATION -> ConfigKey.DEATH_MESSAGES_STARVATION;
      case SUFFOCATION -> ConfigKey.DEATH_MESSAGES_SUFFOCATION;
      case SUICIDE -> ConfigKey.DEATH_MESSAGES_SUICIDE;
      case THORNS -> ConfigKey.DEATH_MESSAGES_THORNS;
      case VOID -> ConfigKey.DEATH_MESSAGES_VOID;
      case WITHER -> ConfigKey.DEATH_MESSAGES_WITHER;
      case ENTITY_ATTACK, ENTITY_EXPLOSION, ENTITY_SWEEP_ATTACK -> ConfigKey.DEATH_MESSAGES_ENTITY;
      default -> ConfigKey.DEATH_MESSAGES_UNKNOWN;
    };

    // Figure out what block damaged the player, if any
    Material damagingBlock = null;
    if (p.getLastDamageCause() instanceof EntityDamageByBlockEvent be)
      damagingBlock = be.getDamager() == null ? null : be.getDamager().getType();

    // Check for falling block entities near the player to find out what block type did the damage
    World w = p.getLocation().getWorld();
    if (damagingBlock == null && e.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK && w != null) {
       FallingBlock fb = (FallingBlock) w.getNearbyEntities(p.getLocation(), 2, 2, 2)
         .stream()
         .filter(ent -> ent instanceof FallingBlock)
         .findFirst()
         .orElse(null);

       if (fb != null)
         damagingBlock = fb.getBlockData().getMaterial();
    }

    // Check for what block suffocated the player
    if (damagingBlock == null && e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION)
      damagingBlock = p.getLocation().add(0, 1, 0).getBlock().getType();

    // Figure out what entity damaged the player, if any
    Entity damagingEntity = null;
    if (p.getLastDamageCause() instanceof EntityDamageByEntityEvent ee)
      damagingEntity = ee.getDamager();

    // Player caused deaths are handled separately
    if (damagingEntity instanceof Player)
      return;

    chat.broadcastMessage(
      Bukkit.getOnlinePlayers(),
      cfg.get(deathMessage)
        .withPrefix()
        .withVariable("player", p.getName())
        .withVariable("block", damagingBlock == null ? "/" : damagingBlock.name())
        .withVariable("entity", damagingEntity == null ? "/" : damagingEntity.getType().name())
        .asScalar()
    );
  }

  @EventHandler
  public void onDamageByPlayer(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof Player victim))
      return;

    if (!(e.getDamager() instanceof Player damager))
      return;

    spawnDamageIndicator(victim, damager, e.getDamage());
    spawnBloodParticles(victim, e.getDamage());
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Spawns damage indicating blood particles
   * @param victim Player to spawn at
   * @param damage Damage done (determines number of particles)
   */
  private void spawnBloodParticles(Player victim, double damage) {
    World w = victim.getLocation().getWorld();
    if (w == null)
      return;

    for (int i = 0; i < calculateNumBloodParticles(damage); i++) {
      w.spawnParticle(
        Particle.BLOCK_CRACK,
        victim.getLocation()
          // Particles use a y-offset from -1 to 1 (whole player height)
          .add(0, 1, 0)
          .add(getNextBloodVector()),
        1,
        Material.REDSTONE_BLOCK.createBlockData()
      );
    }
  }

  /**
   * Calculates the number of blood particles to show corresponding
   * to an amount of damage done
   * @param damage Damage done
   * @return Number of particles to spawn
   */
  private int calculateNumBloodParticles(double damage) {
    // This is pretty much the general form for a map(i, from, to, from, to) function
    // I'm leaving this big chunk here for future reference
    //
    // f(x) = kx + d, x: damage, f: particles
    // f(HEALTH_MIN) = PARTICLES_MIN, f(HEALTH_MAX) = PARTICLES_MAX
    // PARTICLES_MIN = k * HEALTH_MIN  + d
    // PARTICLES_MAX = k * HEALTH_MAX + d
    // -
    // PARTICLES_MIN - PARTICLES_MAX = (k * HEALTH_MIN) - (k * HEALTH_MAX)
    // PARTICLES_MIN - PARTICLES_MAX = k * (HEALTH_MIN-HEALTH_MAX)
    // (PARTICLES_MIN - PARTICLES_MAX) / (HEALTH_MIN-HEALTH_MAX) = k
    // PARTICLES_MIN = ((PARTICLES_MIN - PARTICLES_MAX) / (HEALTH_MIN-HEALTH_MAX)) * HEALTH_MIN + d
    // => d = PARTICLES_MIN - ((PARTICLES_MIN - PARTICLES_MAX) / (HEALTH_MIN-HEALTH_MAX)) * HEALTH_MIN

    // => f(x) = (PARTICLES_MIN - PARTICLES_MAX) / (HEALTH_MIN - HEALTH_MAX) * x + PARTICLES_MIN - ((PARTICLES_MIN - PARTICLES_MAX) / (HEALTH_MIN-HEALTH_MAX)) * HEALTH_MIN

    double i = (BLOOD_PARTICLES_MIN - BLOOD_PARTICLES_MAX) / (BLOOD_HEALTH_MIN - BLOOD_HEALTH_MAX);
    return (int) Math.round(i * damage + BLOOD_PARTICLES_MIN - i * BLOOD_HEALTH_MIN);
  }

  /**
   * Spawn a new damage indicating temporary hologram
   * @param victim Player that has taken damage
   * @param damager Player that has been damaged
   * @param damage Amount of damage done
   */
  private void spawnDamageIndicator(Player victim, Player damager, double damage) {
    MultilineHologram holo = holos.createTemporary(
      victim.getLocation().add(0, LOCATION_YOFF, 0), null,
      cfg.get(isCriticallyDamaging(damager) ? ConfigKey.DAMAGE_INDICATORS_CRITICAL : ConfigKey.DAMAGE_INDICATORS_NORMAL)
        .withVariable("damage", Math.round(damage * 100.0) / 100.0)
        .asList()
    );
    holo.setVelocity(getNextHoloVector(), null, true, true, null, () -> holos.destroyTemporary(holo));
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
   * Get the next usable blood vector from the ringbuffer
   */
  private Vector getNextBloodVector() {
    Vector next = bloodVectors[bloodVectorsInd];

    if (++bloodVectorsInd == BLOOD_PARTICLES_MAX)
      bloodVectorsInd = 0;

    return next;
  }

  /**
   * Get the next usable hologram vector from the ringbuffer
   */
  private Vector getNextHoloVector() {
    Vector next = hologramVectors[hologramVectorsInd];

    if (++hologramVectorsInd == NUM_HOLO_VECTORS)
      hologramVectorsInd = 0;

    return next;
  }

  /**
   * Generates a random vector within range to be used with holograms
   */
  private Vector generateVector(double symXzRange, double yFrom, double yTo) {
    double genX = rand.nextDouble(symXzRange * 2);
    double genZ = rand.nextDouble(symXzRange * 2);

    return new Vector(
      (genX > symXzRange ? -genX : genX) / 2,
      rand.nextDouble() * (yTo - yFrom) + yFrom,
      (genZ > symXzRange ? -genZ : genZ) / 2
    );
  }
}
