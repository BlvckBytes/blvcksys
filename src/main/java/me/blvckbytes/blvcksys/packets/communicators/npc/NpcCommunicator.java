package me.blvckbytes.blvcksys.packets.communicators.npc;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.level.EnumGamemode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Sends packets for spawning, destroying and moving fake npcs as well as adding/removing
  them to/from the tablist.
*/
@AutoConstruct
public class NpcCommunicator implements INpcCommunicator {

  private final MCReflect refl;
  private final ILogger logger;

  public NpcCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
  }

  //=========================================================================//
  //                                  API                                    //
  //=========================================================================//

  @Override
  public void spawnNpc(int entityId, Player receiver, Location loc, GameProfile profile) {
    try {
      // Spawn the named entity
      Object spawn = refl.createPacket(PacketPlayOutNamedEntitySpawn.class);

      // EntityID and UUID
      refl.setFieldByType(spawn, int.class, entityId, 0);
      refl.setFieldByType(spawn, UUID.class, profile.getId(), 0);

      // X, Y, Z
      refl.setFieldByType(spawn, double.class, loc.getX(), 0);
      refl.setFieldByType(spawn, double.class, loc.getY(), 1);
      refl.setFieldByType(spawn, double.class, loc.getZ(), 2);

      // Rotation angle in bytes: A rotation angle in steps of 1/256 of a full turn
      byte yawB = (byte) ((int) (loc.getYaw() / 360.0F * 256.0F));
      byte pitchB = (byte) ((int) (loc.getPitch() / 360.0F * 256.0F));
      refl.setFieldByType(spawn, byte.class, yawB, 0);
      refl.setFieldByType(spawn, byte.class, pitchB, 1);

      // Sets it's metadata (skin and such)
      Tuple<Object, Runnable> fpD = createFakeEntityPlayer(receiver, entityId, loc, profile);
      Object fakePlayer = fpD.a();
      DataWatcher watcher = refl.getFieldByType(fakePlayer, DataWatcher.class, 0);
      Object meta = new PacketPlayOutEntityMetadata(entityId, watcher, true);

      refl.sendPacket(receiver, meta);
      refl.sendPacket(receiver, spawn);

      setRotation(entityId, receiver, loc.getYaw(), loc.getPitch());

      // Execute the hidden armor stand mount
      fpD.b().run();
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void destroyNpc(int entityId, Player receiver) {
    refl.sendPacket(receiver, new PacketPlayOutEntityDestroy(entityId));
  }

  @Override
  public void addToTablist(int entityId, GameProfile profile, Player receiver) {
    try {
      refl.sendPacket(receiver, createInfoPacket(entityId, profile, false));
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void removeFromTablist(int entityId, GameProfile profile, Player receiver) {
    try {
      refl.sendPacket(receiver, createInfoPacket(entityId, profile, true));
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void setRotation(int entityId, Player receiver, float yaw, float pitch) {
    try {
      // Rotation angle in bytes: A rotation angle in steps of 1/256 of a full turn
      byte yawB = (byte) ((int) (yaw / 360.0F * 256.0F));
      byte pitchB = (byte) ((int) (pitch / 360.0F * 256.0F));

      // Rotate the head to the proper yaw value
      Object headRot = refl.createPacket(PacketPlayOutEntityHeadRotation.class);
      refl.setFieldByType(headRot, int.class, entityId, 0);
      refl.setFieldByType(headRot, byte.class, yawB, 0);

      // Rotate the body
      Object look = new PacketPlayOutEntity.PacketPlayOutEntityLook(
        entityId, yawB, pitchB,
        true // onGround
      );

      refl.sendPacket(receiver, headRot);
      refl.sendPacket(receiver, look);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  //=========================================================================//
  //                               Utilities                                 //
  //=========================================================================//

  /**
   * Create a new fake entity player by it's required parameters in order
   * to be able to retrieve a DataWatcher for the desired GameProfile, which
   * will later be used in combination with the metadata packet
   * @param p Receiving player
   * @param entityId ID of the fake player
   * @param loc Location to get the world from
   * @param profile Desired GameProfile
   */
  private Tuple<Object, Runnable> createFakeEntityPlayer(Player p, int entityId, Location loc, GameProfile profile) throws Exception {
    Object ms = refl.getMinecraftServer();
    Object ws = refl.getWorldServer(loc);

    // Create a new entity player by it's constructor
    Object ep = EntityPlayer.class.getDeclaredConstructor(
      MinecraftServer.class,
      WorldServer.class,
      GameProfile.class
    ).newInstance(ms, ws, profile);

    // Create a new armor stand entity using craftbukkit's wrapper
    EntityArmorStand eas = (EntityArmorStand) refl.invokeMethodByName(
      Objects.requireNonNull(loc.getWorld()), "createEntity", new Class[]{
        Location.class, Class.class, boolean.class
      }, loc, EntityType.ARMOR_STAND.getEntityClass(), false
    );

    // Set the armor stand's name and body to invisible
    Object aEnt = refl.invokeMethodByName(eas, "getBukkitEntity", new Class[] {});
    refl.invokeMethodByName(aEnt, "setCustomNameVisible", new Class[] { boolean.class }, false);
    refl.invokeMethodByName(aEnt, "setVisible", new Class[] { boolean.class }, false);

    // Get the armor stand's watcher and entity ID
    DataWatcher watcher = refl.getFieldByType(eas, DataWatcher.class, 0);
    int asID = (int) refl.invokeMethodByName(aEnt, "getEntityId", new Class[] {});

    // Create armor stand spawning packets
    PacketPlayOutSpawnEntity spawnAS = new PacketPlayOutSpawnEntity(eas);
    PacketPlayOutEntityMetadata metaAS = new PacketPlayOutEntityMetadata(asID, watcher, true);

    // Create a packet to mount the armorstand on the fake player
    Object mountAS = refl.createPacket(PacketPlayOutMount.class);
    refl.setFieldByType(mountAS, int.class, entityId, 0);
    refl.setGenericFieldByType(mountAS, int[].class, int.class, new int[]{ asID }, 0);

    // Provide a routine to spawn and mount this hidden armorstand
    return new Tuple<>(ep, () -> {
      refl.sendPacket(p, spawnAS);
      refl.sendPacket(p, metaAS);
      refl.sendPacket(p, mountAS);
    });
  }

  /**
   * Create the info (tablist add/remove) packet from required parameters
   * @param entityId ID of the NPC
   * @param profile GameProfile of the NPC
   * @param remove True to remove from tab, false to add
   */
  private Object createInfoPacket(
    int entityId,
    GameProfile profile,
    boolean remove
  ) throws Exception {
    Object info = refl.createPacket(PacketPlayOutPlayerInfo.class);

    PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a;
    List<PacketPlayOutPlayerInfo.PlayerInfoData> dataList = new ArrayList<>();

    if (remove)
      action = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e;

    // Add the fake player with it's entity ID and GM survivial
    dataList.add(
      new PacketPlayOutPlayerInfo.PlayerInfoData(
        profile, entityId, EnumGamemode.a, new ChatMessage(profile.getName())
      )
    );

    refl.setFieldByType(info, PacketPlayOutPlayerInfo.EnumPlayerInfoAction.class, action, 0);
    refl.setGenericFieldByType(info, List.class, PacketPlayOutPlayerInfo.PlayerInfoData.class, dataList, 0);

    return info;
  }
}
