package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Create a new kit with the contents of your inventory as
  well as a specified cooldown.
*/
@AutoConstruct
public class SetKitCommand extends APlayerCommand {

  private final IPersistence pers;

  public SetKitCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "setkit",
      "Create a new kit with the contents of your inventory",
      PlayerPermission.SETKIT,
      new CommandArgument("<name>", "Name of the kit"),
      new CommandArgument("<cooldown unit multiplier>", "Multiplier of the cooldown unit chosen"),
      new CommandArgument("<cooldown unit>", "Unit of time for the cooldown")
    );

    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest cooldown units
    if (currArg == 2)
      return suggestEnum(args, currArg, CooldownUnit.class);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    float cooldown = parseFloat(args, 1);
    CooldownUnit unit = parseEnum(CooldownUnit.class, args, 2, null);

    // Check if this inventory contains any items
    boolean hasItems = false;
    for (ItemStack item : p.getInventory().getContents()) {
      if (item != null && item.getType() != Material.AIR) {
        hasItems = true;
        break;
      }
    }

    if (!hasItems) {
      p.sendMessage("§cYour inventory is empty.");
      return;
    }

    int cooldownSecs = Math.round(unit.getSeconds() * cooldown);
    KitModel kit = new KitModel(name, p.getInventory(), cooldownSecs);

    try {
      pers.store(kit);
    } catch (DuplicatePropertyException e) {
      p.sendMessage("§cA kit with the name '" + name + "' already exists!");
      return;
    }

    p.sendMessage("§aKit '" + name + "' created!");
  }
}
