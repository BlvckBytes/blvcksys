package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Edit an existing ban's fields.
*/
@AutoConstruct
public class EditBanCommand extends APlayerCommand {

  private enum BanField {
    REASON
  }

  public EditBanCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "editban",
      "Edit a field of an existing ban",
      PlayerPermission.COMMAND_BAN,
      new CommandArgument("<id>", "Name of the target player"),
      new CommandArgument("<field>", "Field to change"),
      new CommandArgument("<value>", "New value of the field")
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 1)
      return suggestEnum(args, currArg, BanField.class);

    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String id = argval(args, 0);
    BanField field = parseEnum(BanField.class, args, 1, null);
    String value = argvar(args, 2);
  }
}
