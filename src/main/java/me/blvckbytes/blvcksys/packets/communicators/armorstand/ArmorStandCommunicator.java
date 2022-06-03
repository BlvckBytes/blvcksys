package me.blvckbytes.blvcksys.packets.communicators.armorstand;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Creates all packets in order to create per-player customized armor stands
  and later update, move or destroy them again.
*/
@AutoConstruct
public class ArmorStandCommunicator implements IArmorStandCommunicator {

  private final MCReflect refl;
  private final ILogger logger;
  private final Map<Entity, Location> lastLocations;

  public ArmorStandCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
    this.lastLocations = new HashMap<>();
  }

  @Override
  public Entity create(Player p, Location loc, ArmorStandProperties properties) {
    try {
      Location shifted = properties.isShifted() ? shiftLocation(loc) : loc;

      // Create a new armor stand entity using craftbukkit's wrapper
      EntityArmorStand eas = (EntityArmorStand) refl.invokeMethodByName(
        p.getWorld(), "createEntity", new Class[]{ Location.class, Class.class, boolean.class },
        shifted, EntityType.ARMOR_STAND.getEntityClass(), false
      );

      Object ent = refl.invokeMethodByName(eas, "getBukkitEntity", new Class[] {});
      refl.invokeMethodByName(ent, "setCustomNameVisible", new Class[] { boolean.class }, properties.isNameVisible());
      refl.invokeMethodByName(ent, "setCustomName", new Class[] { String.class }, properties.getName());
      refl.invokeMethodByName(ent, "setGravity", new Class[] { boolean.class }, false);
      refl.invokeMethodByName(ent, "setVisible", new Class[] { boolean.class }, properties.isVisible());
      int entityId = (int) refl.invokeMethodByName(ent, "getEntityId", new Class[] {});
      DataWatcher watcher = refl.getFieldByType(eas, DataWatcher.class, 0);

      // Spawn the entity only for this player
      PacketPlayOutSpawnEntity spawnP = new PacketPlayOutSpawnEntity(eas);
      PacketPlayOutEntityMetadata metaP = new PacketPlayOutEntityMetadata(entityId, watcher, true);

      refl.sendPacket(p, spawnP);
      refl.sendPacket(p, metaP);

      Entity ret = eas.getBukkitEntity();
      lastLocations.put(ret, loc.clone());

      // Return a handle to the bukkit entity
      return ret;
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  @Override
  public void update(Player p, Entity handle, ArmorStandProperties properties) {
    try {
      refl.invokeMethodByName(handle, "setCustomNameVisible", new Class[] { boolean.class }, properties.isNameVisible());
      refl.invokeMethodByName(handle, "setCustomName", new Class[] { String.class }, properties.getName());
      refl.invokeMethodByName(handle, "setVisible", new Class[] { boolean.class }, properties.isVisible());

      Object ent = refl.invokeMethodByName(handle, "getHandle", new Class[] {});
      DataWatcher watcher = refl.getFieldByType(ent, DataWatcher.class, 0);

      // Send out another metadata update
      PacketPlayOutEntityMetadata metaP = new PacketPlayOutEntityMetadata(handle.getEntityId(), watcher, true);
      refl.sendPacket(p, metaP);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void delete(Player p, Entity handle) {
    try {
      // Destroy the entity by it's ID
      PacketPlayOutEntityDestroy destroyP = new PacketPlayOutEntityDestroy(handle.getEntityId());
      refl.sendPacket(p, destroyP);
      lastLocations.remove(handle);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void teleport(Player p, Entity handle, Location loc, boolean isShifted) {
    try {
      Location shifted = isShifted ? shiftLocation(loc) : loc;

      Object teleportP = refl.createPacket(PacketPlayOutEntityTeleport.class);

      refl.setFieldByType(teleportP, int.class, handle.getEntityId(), 0);
      refl.setFieldByType(teleportP, double.class, shifted.getX(), 0);
      refl.setFieldByType(teleportP, double.class, shifted.getY(), 1);
      refl.setFieldByType(teleportP, double.class, shifted.getZ(), 2);
      refl.setFieldByType(teleportP, byte.class, (byte)((int)(shifted.getYaw() * 256.0F / 360.0F)), 0);
      refl.setFieldByType(teleportP, byte.class, (byte)((int)(shifted.getPitch() * 256.0F / 360.0F)), 1);
      refl.setFieldByType(teleportP, boolean.class, handle.isOnGround(), 0);

      refl.sendPacket(p, teleportP);

      lastLocations.put(handle, loc.clone());
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void moveLine(Player p, Entity handle, Location loc, boolean isShifted) {
    try {
      Location toShifted = isShifted ? shiftLocation(loc) : loc;

      // Get the last location of this line
      Location prev = lastLocations.get(handle);
      if (prev == null)
        throw new IllegalStateException("Unknown armor stand with id=" + handle.getEntityId());

      Location prevShifted = shiftLocation(prev);

      // Calculate the delta per axis and encode it into the required representation
      PacketPlayOutEntity.PacketPlayOutRelEntityMove moveP = new PacketPlayOutEntity.PacketPlayOutRelEntityMove(
        handle.getEntityId(),
        (short) ((toShifted.getX() * 32 - prevShifted.getX() * 32) * 128),
        (short) ((toShifted.getY() * 32 - prevShifted.getY() * 32) * 128),
        (short) ((toShifted.getZ() * 32 - prevShifted.getZ() * 32) * 128),
        handle.isOnGround()
      );

      lastLocations.put(handle, loc.clone());
      refl.sendPacket(p, moveP);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void sendVelocity(Player p, Entity handle, Vector velocity) {
    PacketPlayOutEntityVelocity vel = new PacketPlayOutEntityVelocity(
      handle.getEntityId(),
      new Vec3D(velocity.getX(), velocity.getY(), velocity.getZ())
    );
    refl.sendPacket(p, vel);
  }

  /**
   * Shift a given location to have it end up at the armor stand's head
   * @param loc Location to shift
   */
  private Location shiftLocation(Location loc) {
    // Subtract the length between the armor stand's bottom plate and it's name (round about)
    return loc.clone().add(0, -2.2, 0);
  }
}