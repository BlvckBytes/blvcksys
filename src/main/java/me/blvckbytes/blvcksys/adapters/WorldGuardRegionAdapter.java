package me.blvckbytes.blvcksys.adapters;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Implements the region interaction interface towards the WorldGuard region manager
*/
//@AutoConstruct(pluginDependencies = { "WorldGuard" })
public class WorldGuardRegionAdapter implements IRegionAdapter {

  private final WorldGuard wg;
  private final WorldGuardPlugin wgp;
  private final RegionContainer rg;

  public WorldGuardRegionAdapter() {
    this.wg = WorldGuard.getInstance();
    this.wgp = WorldGuardPlugin.inst();
    this.rg = this.wg.getPlatform().getRegionContainer();
  }

  @Override
  public boolean canBuild(Player p, Location loc) {
    LocalPlayer lp = wgp.wrapPlayer(p);
    return rg.createQuery().testState(
      BukkitAdapter.adapt(loc),
      lp,
      Flags.BUILD
    ) || hasBypass(lp);
  }

  /**
   * Checks if a player has sufficient permissions to bypass protections
   * @param lp Target player
   */
  private boolean hasBypass(LocalPlayer lp) {
    return wg.getPlatform().getSessionManager().hasBypass(lp, lp.getWorld());
  }
}
