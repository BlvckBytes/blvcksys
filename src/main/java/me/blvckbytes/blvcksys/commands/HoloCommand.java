package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Create holograms by creating or deleting individual lines of them.
*/
@AutoConstruct
public class HoloCommand extends APlayerCommand {

  // TODO: /holos [near] command to list existing holograms

  private final IHologramHandler holo;
  private final IPersistence pers;

  private enum HoloAction {
    ADDLINE,
    REMOVELINE,
    CHANGELINE,
    LISTLINES,
    DELETE
  }

  public HoloCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHologramHandler holo,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "holo",
      "Manage holograms and their lines",
      PlayerPermission.COMMAND_HOLO,
      new CommandArgument("<name>", "Name of the hologram"),
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[line id/text]", "ID of the line to delete or the text to add"),
      new CommandArgument("[changed text]", "The text to change to")
    );

    this.holo = holo;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest existing holograms
    if (currArg == 0)
      return pers.listRaw(HologramLineModel.class, "name")
        .stream()
        .map(e -> e.get("name"))
        .filter(Objects::nonNull)
        .map(Object::toString);

    // Suggest actions
    if (currArg == 1)
      return suggestEnum(args, currArg, HoloAction.class);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    HoloAction action = parseEnum(HoloAction.class, args, 1, null);

    if (action == HoloAction.DELETE) {
      if (!holo.deleteHologram(name)) {
        p.sendMessage("§cThere is no hologram with the name " + name);
        return;
      }

      p.sendMessage("§aHologram " + name + " has been deleted");
      return;
    }

    if (action == HoloAction.ADDLINE) {
      holo.createHologramLine(name, p.getLocation(), argvar(args, 2));
      p.sendMessage("§aLine created");
      return;
    }

    List<HologramLineModel> lines = holo.getHologramLines(name).orElse(null);

    if (lines == null) {
      p.sendMessage("§cThere is no hologram with the name " + name);
      return;
    }

    if (action == HoloAction.LISTLINES) {
      p.sendMessage("Lines of " + name + ":");
      for (int i = 0; i < lines.size(); i++)
        p.sendMessage("[" + i + "]: " + lines.get(i).getText());
      return;
    }

    int lineId = parseInt(args, 2);

    if (lineId < 0 || lineId >= lines.size()) {
      p.sendMessage("§cLine id out of range");
      return;
    }

    HologramLineModel line = lines.get(lineId);

    if (action == HoloAction.CHANGELINE) {
      line.setText(argvar(args, 3));
      pers.store(line);
      p.sendMessage("§aText has been updated");
      return;
    }

    if (action == HoloAction.REMOVELINE) {
      if (!holo.deleteHologramLine(line)) {
        p.sendMessage("§cSeems like the line has been deleted in the mean time");
        return;
      }

      p.sendMessage("§aLine has been deleted");
      return;
    }
  }
}
