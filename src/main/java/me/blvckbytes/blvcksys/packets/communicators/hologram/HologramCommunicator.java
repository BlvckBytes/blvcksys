package me.blvckbytes.blvcksys.packets.communicators.hologram;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Creates all packets in order to create per-player customized hologram lines
  and later update or destroy them again.
*/
@AutoConstruct
public class HologramCommunicator implements IHologramCommunicator {

  private final MCReflect refl;
  private final ILogger logger;

  public HologramCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
  }

  @Override
  public Entity createLine(Player p, Location loc, String line) {
    try {
      // Create a new armor stand entity using craftbukkit's wrapper
      EntityArmorStand eas = (EntityArmorStand) refl.invokeMethodByName(p.getWorld(), "createEntity", new Class[]{ Location.class, Class.class, boolean.class }, p.getLocation(), EntityType.ARMOR_STAND.getEntityClass(), false);
      Object ent = refl.invokeMethodByName(eas, "getBukkitEntity", new Class[] {});
      refl.invokeMethodByName(ent, "setCustomNameVisible", new Class[] { boolean.class }, true);
      refl.invokeMethodByName(ent, "setCustomName", new Class[] { String.class }, line);
      refl.invokeMethodByName(ent, "setGravity", new Class[] { boolean.class }, false);
      refl.invokeMethodByName(ent, "setVisible", new Class[] { boolean.class }, false);
      int entityId = (int) refl.invokeMethodByName(ent, "getEntityId", new Class[] {});
      DataWatcher watcher = refl.getFieldByType(eas, DataWatcher.class, 0);

      // Spawn the entity only for this player
      PacketPlayOutSpawnEntity spawnP = new PacketPlayOutSpawnEntity(eas);
      PacketPlayOutEntityMetadata metaP = new PacketPlayOutEntityMetadata(entityId, watcher, true);

      refl.sendPacket(p, spawnP);
      refl.sendPacket(p, metaP);

      // Return a handle to the bukkit entity
      return eas.getBukkitEntity();
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  @Override
  public void updateLine(Player p, Entity handle, String newLine) {
    try {
      // Update the custom name
      refl.invokeMethodByName(handle, "setCustomName", new Class[] {String.class}, newLine);

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
  public void deleteLine(Player p, Entity handle) {
    try {
      // Destroy the entity by it's ID
      PacketPlayOutEntityDestroy destroyP = new PacketPlayOutEntityDestroy(handle.getEntityId());
      refl.sendPacket(p, destroyP);
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
