package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.PlayerPermissionsChangedEvent;
import me.blvckbytes.blvcksys.handlers.ITeamHandler;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Hide yourself from all other players which don't have the corresponding
  bypass-permission by going out of sight, hiding the tab-list entry and stopping
  all further possible chat-suggestions and identifications. Listens for
  permission changes and updates visibilities accordingly.
*/
@AutoConstruct
public class VanishCommand extends APlayerCommand implements IAutoConstructed, Listener, IVanishCommand {

  // All players which currently are in vanish mode
  private final Set<Player> vanished;
  private final ITeamHandler team;

  public VanishCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ITeamHandler team
  ) {
    super(
      plugin, logger, cfg, refl,
      "vanish",
      "Hide yourself from all players",
      PlayerPermission.VANISH
    );

    this.team = team;
    this.vanished = new HashSet<>();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    boolean state = isVanished(p);

    setVanishState(p, !state, true);

    p.sendMessage(
      cfg.get(!state ? ConfigKey.VANISH_HIDDEN : ConfigKey.VANISH_SHOWN)
        .withPrefix()
        .asScalar()
    );
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    // Undo all players vanished state
    for (Iterator<Player> pi = this.vanished.iterator(); pi.hasNext();) {
      setVanishState(pi.next(), false, false);
      pi.remove();
    }
  }

  @Override
  public void initialize() {}

  @Override
  public boolean isVanished(Player p) {
    return this.vanished.contains(p);
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuit(PlayerQuitEvent e) {
    // Undo vanish on quit
    if (isVanished(e.getPlayer()))
      setVanishState(e.getPlayer(), false, true);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    if (PlayerPermission.VANISH_BYPASS.has(e.getPlayer()))
      return;

    // Hide all vanished players for the newly joined player
    for (Player vanish : vanished)
      e.getPlayer().hidePlayer(plugin, vanish);
  }

  @EventHandler
  public void onPermissionsChange(PlayerPermissionsChangedEvent e) {
    // Update the visibility for all vanished players for the player
    // that just received a permission update
    boolean canSeeVanished = PlayerPermission.VANISH_BYPASS.has(e.getPlayer());

    for (Player vanish : vanished) {
      if (canSeeVanished)
        e.getPlayer().showPlayer(plugin, vanish);
      else
        e.getPlayer().hidePlayer(plugin, vanish);
    }
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Sets the vanish mode of a given player
   * @param p Target player
   * @param state Vanished state
   * @param modifyList Whether to modify the local list of vanished players (adding, removing)
   */
  private void setVanishState(Player p, boolean state, boolean modifyList) {
    // Hide for all other players
    for (Player t : Bukkit.getOnlinePlayers()) {
      // Skip self or bypassing players
      if (t.equals(p) || PlayerPermission.VANISH_BYPASS.has(t))
        continue;

      if (state) {
        t.hidePlayer(plugin, p);
      } else {
        t.showPlayer(plugin, p);
      }
    }

    if (!modifyList)
      return;

    if (state)
      this.vanished.add(p);
    else
      this.vanished.remove(p);

    team.update(p);
  }
}
