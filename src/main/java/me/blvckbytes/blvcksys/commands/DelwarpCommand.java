package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.ModelNotFoundException;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Delete an existing warp by it's name.
*/
@AutoConstruct
public class DelwarpCommand extends APlayerCommand {

  private final IPersistence pers;

  public DelwarpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "delwarp",
      "Delete an existing warp",
      PlayerPermission.DELWARP,
      new CommandArgument("<uuid>", "UUID of the warp (for now)")
    );

    this.pers = pers;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String uuid = argval(args, 0);

    try {
      UUID u = UUID.fromString(uuid);
      pers.delete(WarpModel.class, u);
      p.sendMessage("§aDeleted " + u);
    } catch (IllegalArgumentException e) {
      customError("§cInvalid UUID");
    } catch (ModelNotFoundException e) {
      p.sendMessage("§cNo model with ID " + uuid);
    }
  }
}
