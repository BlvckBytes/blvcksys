package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.handlers.MultilineHologram;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
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
  hologram which displays information about the damage done. Also modifies
  vanilla death messages to very detailled config values.
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

  private final ChatListener chat;
  private final IHologramHandler holos;
  private final IConfig cfg;

  private final Vector[] hologramVectors;
  private int hologramVectorsInd;

  public PlayerDamageListener(
    @AutoInject IHologramHandler holos,
    @AutoInject IConfig cfg,
    @AutoInject ChatListener chat
  ) {
    this.holos = holos;
    this.cfg = cfg;
    this.chat = chat;

    this.hologramVectors = new Vector[NUM_VECTORS];

    for (int i = 0; i < NUM_VECTORS; i++)
      hologramVectors[i] = generateVector();
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
      victim.getLocation().add(0, LOCATION_YOFF, 0), null,
      cfg.get(isCriticallyDamaging(damager) ? ConfigKey.DAMAGE_INDICATORS_CRITICAL : ConfigKey.DAMAGE_INDICATORS_NORMAL)
        .withVariable("damage", Math.round(damage * 100.0) / 100.0)
        .asList()
    );
    holo.setVelocity(getNextVector(), null, true, true, null, () -> holos.destroyTemporary(holo));
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
