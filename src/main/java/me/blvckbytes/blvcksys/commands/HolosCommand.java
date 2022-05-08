package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  List all global holograms or just holograms near you.
*/
@AutoConstruct
public class HolosCommand extends APlayerCommand {

  private final IHologramHandler holo;

  public HolosCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHologramHandler holo
  ) {
    super(
      plugin, logger, cfg, refl,
      "holos",
      "List global or nearby holograms",
      PlayerPermission.COMMAND_HOLO,
      new CommandArgument("[radius]", "Only list holograms within that radius of you")
    );

    this.holo = holo;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    float radius = parseFloat(args, 0, 50F);

    // TODO: Sort by distance and also print the distance with each entry

    // No radius specified, list globally
    Map<String, List<HologramLineModel>> groupedLines = this.holo.getNear(p.getLocation(), radius);

    // Print the first found location of each hologram
    p.sendMessage("§aHolograms within a radius of " + radius + " blocks:");
    for (Map.Entry<String, List<HologramLineModel>> le : groupedLines.entrySet()) {
      Location l = le.getValue().get(0).getLoc();
      p.sendMessage("§aHologram " + le.getKey() + " is at (" + l.getBlockX() + "|" + l.getBlockY() + "|" + l.getBlockZ() + ")");
    }

    if (groupedLines.size() == 0)
      p.sendMessage("§cThere are no holograms near you!");
  }
}
