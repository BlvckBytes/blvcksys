package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.PlayerPermissionsChangedEvent;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.cmd.CommandArgument;
import me.blvckbytes.blvcksys.util.cmd.exception.CommandException;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@AutoConstruct
public class FlyCommand extends APlayerCommand implements Listener {

  // How long the fall-damage protection will wait for one occurrence of
  // cancellable damage until it expires (to avoid dead list entries)
  private static final int PROTECTION_TIMEOUT = 30;

  // Players that have just got their flying permissions revoked
  // Maps the player to their protection timeout task handle
  private final Map<Player, Integer> justRevoked;

  public FlyCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "fly",
      "Toggle flying mode",
      PlayerPermission.COMMAND_FLY,
      new CommandArgument("[player]", "The target player", PlayerPermission.COMMAND_FLY_OTHERS)
    );

    this.justRevoked = new HashMap<>();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // First argument - provide all online players
    if (currArg == 0)
      return suggestOnlinePlayers(args, currArg);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0, p);

    // Toggle flight allow state
    boolean newState = !target.getAllowFlight();
    p.setAllowFlight(newState);

    p.sendMessage(
      cfg.get(newState ? ConfigKey.FLY_ENABLED : ConfigKey.FLY_DISABLED)
        .withPrefix()
        .asScalar()
    );
  }

  @EventHandler
  public void onPermissionsChanged(PlayerPermissionsChangedEvent e) {
    Player p = e.getPlayer();

    // Only protect vulnerable players
    if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE)
      return;

    // Is not allowed to fly
    if (!p.getAllowFlight())
      return;

    // Revoked flight
    if (!PlayerPermission.COMMAND_FLY.has(p)) {
      // Schedule a task to timeout this protection
      int taskHandle = Bukkit.getScheduler().scheduleSyncDelayedTask(
        plugin, () -> justRevoked.remove(p),
        20L * PROTECTION_TIMEOUT
      );

      // Disable flight
      justRevoked.put(p, taskHandle);
      p.setAllowFlight(false);

      // Inform
      p.sendMessage(
        cfg.get(ConfigKey.FLY_REVOKED)
          .withPrefix()
          .asScalar()
      );
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent e) {
    // Only listen for players
    if (!(e.getEntity() instanceof Player p))
      return;

    // Only listen for fall-damage
    if (e.getCause() != EntityDamageEvent.DamageCause.FALL)
      return;

    // Hasn't been revoked
    if (!justRevoked.containsKey(p))
      return;

    // Cancel the timeout task and prevent the damage
    int taskHandle = justRevoked.remove(p);
    Bukkit.getScheduler().cancelTask(taskHandle);
    e.setCancelled(true);
  }
}