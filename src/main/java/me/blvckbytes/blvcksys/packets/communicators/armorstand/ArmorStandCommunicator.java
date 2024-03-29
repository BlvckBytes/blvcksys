package me.blvckbytes.blvcksys.packets.communicators.armorstand;

import com.mojang.datafixers.util.Pair;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

  // Storing the last location for each entity as well as a flag whether
  // the entity was small the last time
  private final Map<Entity, Tuple<Location, Boolean>> lastLocations;

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
      Location shifted = properties.isShifted() ? shiftLocation(loc, properties.isSmall()) : loc;

      // Create a new armor stand entity using craftbukkit's wrapper
      EntityArmorStand eas = (EntityArmorStand) refl.invokeMethodByName(
        p.getWorld(), "createEntity", new Class[]{ Location.class, Class.class, boolean.class },
        shifted, EntityType.ARMOR_STAND.getEntityClass(), false
      );

      Object ent = refl.invokeMethodByName(eas, "getBukkitEntity", new Class[] {});
      applyProperties((Entity) ent, properties);

      int entityId = (int) refl.invokeMethodByName(ent, "getEntityId", new Class[] {});
      DataWatcher watcher = refl.getFieldByType(eas, DataWatcher.class, 0);

      // Spawn the entity only for this player
      PacketPlayOutSpawnEntity spawnP = new PacketPlayOutSpawnEntity(eas);
      PacketPlayOutEntityMetadata metaP = new PacketPlayOutEntityMetadata(entityId, watcher, true);

      refl.sendPacket(p, spawnP);
      refl.sendPacket(p, metaP);

      Entity ret = eas.getBukkitEntity();
      lastLocations.put(ret, new Tuple<>(loc.clone(), properties.isSmall()));

      // Send updates for all equipment slots
      sendEquipment(p, (Entity) ent, properties);

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
      applyProperties(handle, properties);

      Object ent = refl.invokeMethodByName(handle, "getHandle", new Class[] {});
      DataWatcher watcher = refl.getFieldByType(ent, DataWatcher.class, 0);

      // Send out another metadata update
      PacketPlayOutEntityMetadata metaP = new PacketPlayOutEntityMetadata(handle.getEntityId(), watcher, true);
      refl.sendPacket(p, metaP);

      // Send updates for all equipment slots
      sendEquipment(p, handle, properties);
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
  public void teleport(Player p, Entity handle, Location loc, ArmorStandProperties properties) {
    try {
      Location shifted = properties.isShifted() ? shiftLocation(loc, properties.isSmall()) : loc;

      Object teleportP = refl.createPacket(PacketPlayOutEntityTeleport.class);

      refl.setFieldByType(teleportP, int.class, handle.getEntityId(), 0);
      refl.setFieldByType(teleportP, double.class, shifted.getX(), 0);
      refl.setFieldByType(teleportP, double.class, shifted.getY(), 1);
      refl.setFieldByType(teleportP, double.class, shifted.getZ(), 2);
      refl.setFieldByType(teleportP, byte.class, (byte)((int)(shifted.getYaw() * 256.0F / 360.0F)), 0);
      refl.setFieldByType(teleportP, byte.class, (byte)((int)(shifted.getPitch() * 256.0F / 360.0F)), 1);
      refl.setFieldByType(teleportP, boolean.class, handle.isOnGround(), 0);

      refl.sendPacket(p, teleportP);

      lastLocations.put(handle, new Tuple<>(loc.clone(), properties.isSmall()));
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void move(Player p, Entity handle, Location loc, ArmorStandProperties properties) {
    try {
      Location toShifted = properties.isShifted() ? shiftLocation(loc, properties.isSmall()) : loc;

      // Get the last location of this line
      Tuple<Location, Boolean> prev = lastLocations.get(handle);
      if (prev == null)
        throw new IllegalStateException("Unknown armor stand with id=" + handle.getEntityId());

      Location prevShifted = properties.isShifted() ? shiftLocation(prev.a(), prev.b()) : prev.a();

      // Calculate the delta per axis and encode it into the required representation
      PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook moveP = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
        handle.getEntityId(),
        (short) ((toShifted.getX() * 32 - prevShifted.getX() * 32) * 128),
        (short) ((toShifted.getY() * 32 - prevShifted.getY() * 32) * 128),
        (short) ((toShifted.getZ() * 32 - prevShifted.getZ() * 32) * 128),
        (byte) (toShifted.getYaw() * 256.0F / 360.0F),
        (byte) (toShifted.getPitch() * 256.0F / 360.0F),
        handle.isOnGround()
      );

      lastLocations.put(handle, new Tuple<>(loc.clone(), properties.isSmall()));
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
   * @param small Whether the armor stand is small
   */
  private Location shiftLocation(Location loc, boolean small) {
    /*
      https://minecraft.fandom.com/wiki/Armor_Stand
      Normal Height: 1.975 Blocks
      Small Height: 0.9875 Blocks
     */

    // Subtract the length between the armor stand's bottom plate and it's name (round about)
    return loc.clone().add(0, small ? -1 : -2, 0);
  }

  /**
   * Sends an equipment update packet for all itemstacks
   * @param p Target player
   * @param handle Entity handle
   * @param props Property wrapper containing all items
   */
  private void sendEquipment(Player p, Entity handle, ArmorStandProperties props) throws Exception {
    List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment = new ArrayList<>();

    getSlotByName("offhand").ifPresent(slot -> equipment.add(new Pair<>(slot, nmsStack(props.getOffHand()))));
    getSlotByName("mainhand").ifPresent(slot -> equipment.add(new Pair<>(slot, nmsStack(props.getHand()))));
    getSlotByName("head").ifPresent(slot -> equipment.add(new Pair<>(slot, nmsStack(props.getHelmet()))));
    getSlotByName("chest").ifPresent(slot -> equipment.add(new Pair<>(slot, nmsStack(props.getChestplate()))));
    getSlotByName("legs").ifPresent(slot -> equipment.add(new Pair<>(slot, nmsStack(props.getLeggings()))));
    getSlotByName("feet").ifPresent(slot -> equipment.add(new Pair<>(slot, nmsStack(props.getBoots()))));

    refl.sendPacket(p, new PacketPlayOutEntityEquipment(handle.getEntityId(), equipment));
  }

  /**
   * Get a slot by it's representing name within the enum
   * @param name Name of the target slot
   */
  private Optional<EnumItemSlot> getSlotByName(String name) throws Exception {
    for (EnumItemSlot slot : EnumItemSlot.values()) {
      if (refl.getFieldByType(slot, String.class, 0).equalsIgnoreCase(name))
        return Optional.of(slot);
    }
    return Optional.empty();
  }

  /**
   * Convert a bukkit item stack to it's NMS equivalent
   * @param item Bukkit item stack
   * @return NMS equivalent
   */
  private net.minecraft.world.item.ItemStack nmsStack(ItemStack item) {
    try {
      return (net.minecraft.world.item.ItemStack) refl.findMethodByName(
        refl.getClassBKT("inventory.CraftItemStack"),
        "asNMSCopy", ItemStack.class
      ).invoke(null, item);
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  /**
   * Applies all armor stand properties to a given entity handle
   * @param handle Handle to apply to
   * @param props Properties to apply
   */
  private void applyProperties(Entity handle, ArmorStandProperties props) throws Exception {
    refl.invokeMethodByName(handle, "setCustomNameVisible", new Class[] { boolean.class }, props.isNameVisible());
    refl.invokeMethodByName(handle, "setCustomName", new Class[] { String.class }, props.getName());
    refl.invokeMethodByName(handle, "setGravity", new Class[] { boolean.class }, false);
    refl.invokeMethodByName(handle, "setVisible", new Class[] { boolean.class }, props.isVisible());

    refl.invokeMethodByName(handle, "setArms", new Class[] { boolean.class }, props.isArms());
    refl.invokeMethodByName(handle, "setSmall", new Class[] { boolean.class }, props.isSmall());
    refl.invokeMethodByName(handle, "setBasePlate", new Class[] { boolean.class }, props.isBaseplate());

    refl.invokeMethodByName(handle, "setHeadPose", new Class[]{ EulerAngle.class }, angleFallback(props.getHeadPose()));
    refl.invokeMethodByName(handle, "setBodyPose", new Class[] { EulerAngle.class }, angleFallback(props.getBodyPose()));
    refl.invokeMethodByName(handle, "setLeftArmPose", new Class[] { EulerAngle.class }, angleFallback(props.getLeftArmPose()));
    refl.invokeMethodByName(handle, "setRightArmPose", new Class[] { EulerAngle.class }, angleFallback(props.getRightArmPose()));
    refl.invokeMethodByName(handle, "setLeftLegPose", new Class[] { EulerAngle.class }, angleFallback(props.getLeftLegPose()));
    refl.invokeMethodByName(handle, "setRightLegPose", new Class[] { EulerAngle.class }, angleFallback(props.getRightLegPose()));
  }

  /**
   * Provides a fallback of zero degrees for all axies if the given angle is null
   * @param angle Angle to provide a fallback for
   */
  private EulerAngle angleFallback(@Nullable EulerAngle angle) {
    if (angle != null)
      return angle;
    return new EulerAngle(0, 0, 0);
  }
}
