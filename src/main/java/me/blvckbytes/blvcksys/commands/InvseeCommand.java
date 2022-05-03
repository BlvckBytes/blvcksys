package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Check out the inventory contents of others and possibly even
  alter them remotely.
*/
@AutoConstruct
public class InvseeCommand extends APlayerCommand implements Listener {

  public InvseeCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "invsee",
      "Watch foreign inventories remotely",
      PlayerPermission.COMMAND_INVSEE,
      new CommandArgument("<player>", "Target inventory holder")
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0);

    // Players cannot invsee themselves
    if (target.equals(p)) {
      p.sendMessage(
        cfg.get(ConfigKey.INVSEE_SELF)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    p.openInventory(target.getInventory());
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    // Wasn't caused by a player
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // Clicked into empty space
    Inventory inv = e.getClickedInventory();
    if (inv == null)
      return;

    // The holder is not a player
    if (!(inv.getHolder() instanceof Player h))
      return;

    // It's their own inventory
    if (h.equals(p))
      return;

    // It's a foreign inventory but the player is allowed to alter it
    if (PlayerPermission.COMMAND_INVSEE_ALTER.has(p))
      return;

    // Cancel messing with any contents
    e.setCancelled(true);
  }
}
