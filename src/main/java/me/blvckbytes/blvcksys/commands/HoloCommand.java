package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

  private final IHologramHandler holo;
  private final IPersistence pers;

  private enum HoloAction {
    ADDLINE,
    REMOVELINE,
    CHANGELINE,
    LISTLINES,
    MOVEHERE,
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
      new CommandArgument("[text]", "The text to change to")
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

    // Suggest placeholders
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    HoloAction action = parseEnum(HoloAction.class, args, 1, null);

    // Delete the whole hologram
    if (action == HoloAction.DELETE) {
      if (!holo.deleteHologram(name)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_HOLO_NOT_EXISTING)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_DELETED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    // Move the whole hologram
    if (action == HoloAction.MOVEHERE) {
      if (!holo.moveHologram(name, p.getLocation())) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_HOLO_NOT_EXISTING)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_MOVED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    // Get all lines of the selected hologram
    List<HologramLineModel> lines = holo.getHologramLines(name).orElse(null);

    // Add an individual line to the end of the hologram
    if (action == HoloAction.ADDLINE) {
      // Set this new line's location either to the players location (when no
      // hologram with this name yet exists) or to the location of the first
      // existing line corresponding to this name (so that all lines have the
      // exact same location, as laying lines out will be done at runtime).
      Location loc = lines == null ? p.getLocation() : lines.get(0).getLoc();

      holo.createHologramLine(p, name, loc, ChatColor.translateAlternateColorCodes('&', argvar(args, 2)));
      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_LINE_ADDED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (lines == null) {
      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    // List all existing lines
    if (action == HoloAction.LISTLINES) {
      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_LINES_HEADER)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );

      for (int i = 0; i < lines.size(); i++) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_HOLO_LINES_LINE_FORMAT)
            .withPrefix()
            .withVariable("index", i + 1)
            .withVariable("text", lines.get(i).getText())
            .asScalar()
        );
      }
      return;
    }

    // Parse the desired line index (one-based, human friendly)
    int lineId = parseInt(args, 2);
    if (lineId <= 0 || lineId > lines.size()) {
      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_INVALID_INDEX)
          .withPrefix()
          .withVariable("index", lineId)
          .asScalar()
      );
      return;
    }

    HologramLineModel line = lines.get(lineId - 1);

    // Change a line by it's index
    if (action == HoloAction.CHANGELINE) {
      line.setText(ChatColor.translateAlternateColorCodes('&', argvar(args, 3)));
      pers.store(line);

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_LINE_UPDATED)
          .withPrefix()
          .withVariable("name", name)
          .withVariable("index", lineId)
          .asScalar()
      );
      return;
    }

    // Remove a line by it's index
    if (action == HoloAction.REMOVELINE) {
      if (!holo.deleteHologramLine(line)) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_HOLO_LINE_DISAPPEARED)
            .withPrefix()
            .withVariable("index", lineId)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLO_LINE_DELETED)
          .withPrefix()
          .withVariable("name", name)
          .withVariable("index", lineId)
          .asScalar()
      );
      return;
    }
  }
}
