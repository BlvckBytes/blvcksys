package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.gui.VirtualFurnace;
import me.blvckbytes.blvcksys.util.MCReflect;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Handles virtual furnaces by keeping a list of instances per player which
  are ticked inside the main loop and persisted periodically as well as when
  the handler is unloaded.
*/
@AutoConstruct
public class VirtualFurnaceHandler implements IVirtualFurnaceHandler, IAutoConstructed, Listener {

  // Mapping players to their virtual furnaces, which each have a unique
  // sequence ID, as players may own multiple concurrent furnaces
  private final Map<Player, Map<Integer, VirtualFurnace>> furnaces;

  private final JavaPlugin plugin;
  private final MCReflect refl;
  private BukkitTask tickerHandle;

  public VirtualFurnaceHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject MCReflect refl
  ) {
    this.furnaces = new HashMap<>();
    this.plugin = plugin;
    this.refl = refl;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public VirtualFurnace accessFurnace(Player p, int index) {
    if (!furnaces.containsKey(p))
      furnaces.put(p, new HashMap<>());

    Map<Integer, VirtualFurnace> pFurnaces = furnaces.get(p);

    // Furnace with this index is not yet existing, create a new one
    if (!pFurnaces.containsKey(index)) {
      VirtualFurnace vf = new VirtualFurnace(p, index);
      pFurnaces.put(index, vf);
      return vf;
    }

    // Return already existing furnace
    return pFurnaces.get(index);
  }

  @Override
  public void cleanup() {
    if (this.tickerHandle != null)
      tickerHandle.cancel();
  }

  @Override
  public void initialize() {
    this.tickerHandle = Bukkit.getScheduler().runTaskTimer(plugin, this::tickFurnaces, 0L, 1L);
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // TODO: Persist state before removing
    furnaces.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Tick all currently registered furnaces
   */
  private void tickFurnaces() {
    for (Map<Integer, VirtualFurnace> pFurnaces : furnaces.values()) {
      for (VirtualFurnace furnace : pFurnaces.values()) {
        furnace.tick(refl);
      }
    }
  }
}
