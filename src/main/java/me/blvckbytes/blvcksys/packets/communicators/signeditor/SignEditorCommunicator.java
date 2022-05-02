package me.blvckbytes.blvcksys.packets.communicators.signeditor;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.packets.communicators.blockspoof.IBlockSpoofCommunicator;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInUpdateSign;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenSignEditor;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Creates all packets in regard to opening a sign editor GUI and retrieving it's result.
*/
@AutoConstruct
public class SignEditorCommunicator implements ISignEditorCommunicator, IPacketModifier, Listener, IAutoConstructed {

  private final MCReflect refl;
  private final IBlockSpoofCommunicator spoof;
  private final JavaPlugin plugin;
  private final IConfig cfg;
  private final ILogger logger;

  // Map of a player to their signedit request (tuple of a callback and fake sign location)
  private final Map<UUID, Tuple<Consumer<String[]>, Location>> signeditRequests;

  public SignEditorCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject IBlockSpoofCommunicator spoof,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject IConfig cfg,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.spoof = spoof;
    this.plugin = plugin;
    this.cfg = cfg;
    this.logger = logger;

    this.signeditRequests = new HashMap<>();
    interceptor.register(this);
  }

  @Override
  public boolean openSignEditor(Player p, String[] lines, Consumer<String[]> submit) {
    // Just spawn the fake sign somewhere below the player
    Location fs = p.getLocation().add(0, -5, 0);

    // Open a sign editor and set a sign-edit request with the
    // callback and the fake sign's location
    if (sendSignEditor(p, lines, fs)) {
      signeditRequests.put(p.getUniqueId(), new Tuple<>(submit, fs));
      return true;
    }

    return false;
  }

  /**
   * Sends the packet to open a sign editor for a player
   * @param p Target player
   * @param lines Lines to display within the editor
   * @param loc Location of the fake sign to spoof
   * @return Success state
   */
  private boolean sendSignEditor(Player p, String[] lines, Location loc) {
    BlockPosition pos = new BlockPosition(loc.getX(), loc.getY(), loc.getZ());

    // Spoof a sign at the fake sign's location
    if (!spoof.spoofBlock(p, loc, Material.OAK_SIGN))
      return false;

    try {
      // Send out a sign lines update packet for the fake sign
      Object ped = refl.createPacket(PacketPlayOutTileEntityData.class);
      NBTTagCompound nbt = new NBTTagCompound();

      // Append all lines to this tag
      for (int i = 0; i < lines.length; i++)
        refl.invokeMethodByArgsOnly(
          nbt,
          new Class[]{ String.class, String.class },
          "Text" + (i + 1),
          "{\"text\":\"" + lines[i] + "\"}"
        );

      // Append the location to this tag
      Method setDouble = refl.findMethodByArgsOnly(nbt, String.class, double.class);
      setDouble.invoke(nbt, "x", loc.getX());
      setDouble.invoke(nbt, "y", loc.getY());
      setDouble.invoke(nbt, "z", loc.getZ());

      refl.setFieldByType(ped, BlockPosition.class, pos, 0);
      refl.setFieldByType(ped, NBTTagCompound.class, nbt, 0);
      refl.setFieldByType(ped, TileEntityTypes.class, TileEntityTypes.h, 0);

      if (!refl.sendPacket(p, ped))
        return false;

      // Send out a sign editor open packet for the fake block
      Object pse =  refl.createPacket(PacketPlayOutOpenSignEditor.class);
      refl.setFieldByType(pse, BlockPosition.class, pos, 0);
      return refl.sendPacket(p, pse);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }

  @Override
  public Packet<?> modifyIncoming(UUID sender, NetworkManager nm, Packet<?> incoming) {
    // Only listen to sign updates
    if (incoming instanceof PacketPlayInUpdateSign us) {
      // No callback registered for this client
      Tuple<Consumer<String[]>, Location> tuple = signeditRequests.remove(sender);
      if (tuple == null)
        return incoming;

      try {
        // Get the lines from the sign
        String[] lines = refl.getGenericFieldByType(us, String[].class, String.class, 0);

        // Update the fake block back to it's original state
        Player tar = Bukkit.getPlayer(sender);
        if (tar != null)
          tar.sendBlockChange(tuple.b(), tuple.b().getBlock().getBlockData());

        // Synchronize back with the main task
        Bukkit.getScheduler().runTask(plugin, () -> tuple.a().accept(lines));
      } catch (Exception e) {
        logger.logError(e);
      }
    }
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    return outgoing;
  }

  @Override
  public void cleanup() {
    // Cancel editing for all active requests
    for (UUID u : signeditRequests.keySet()) {
      Player p = Bukkit.getPlayer(u);
      if (p != null)
        cancelEdit(p);
    }
  }

  @Override
  public void initialize() {}

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Unregister pending editing request
    signeditRequests.remove(e.getPlayer().getUniqueId());
  }

  private void cancelEdit(Player p) {
    // No active edit
    if (signeditRequests.remove(p.getUniqueId()) == null)
      return;

    // Close the active window
    try {
      Object pcw = refl.createPacket(PacketPlayOutCloseWindow.class);
      refl.setFieldByType(pcw, int.class, 0, 0);
      refl.sendPacket(p, pcw);
    } catch (Exception e) {
      logger.logError(e);
    }

    // Inform
    p.sendMessage(
      cfg.get(ConfigKey.SIGNEDIT_CANCELLED)
        .withPrefix()
        .asScalar()
    );
  }
}
