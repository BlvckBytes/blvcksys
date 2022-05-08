package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Teleport to the highest block at your position.
 */
@AutoConstruct
public class TopCommand extends APlayerCommand implements ITopCommand {

  public TopCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "top",
      "Teleport to the highest block",
      PlayerPermission.COMMAND_TOP
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Optional<Location> highestPoint = searchYSlice(p, false, false, true, true);

    p.sendMessage(
      cfg.get(highestPoint.isPresent() ? ConfigKey.TOP_TELEPORTED : ConfigKey.TOP_AIR)
        .withPrefix()
        .asScalar()
    );

    highestPoint
      .map(l -> l.add(0, 1, 0))
      .ifPresent(p::teleport);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  @Override
  public Optional<Location> searchYSlice(Player p, boolean air, boolean down, boolean exhaust, boolean boundaries) {
    Location loc = p.getLocation();
    int lastY = loc.getBlockY();

    // Loop all blocks at the player's current location
    for (
      // Start out at their current location offset by one
      int y = loc.clone().add(0, down ? -1 : 1, 0).getBlockY();

      // Go till max- or minheight
      !down && y <= p.getWorld().getMaxHeight() || down && y >= p.getWorld().getMinHeight();

      // Go up or down
      y += (down ? -1 : 1)
    ) {
      Material curr = p.getWorld().getType(loc.getBlockX(), y, loc.getBlockZ());

      // Save last air-matching location
      if (air == curr.isAir()) {
        lastY = y;

        if (exhaust)
          continue;
      }

      // Done with search - found result (possibly with exhaustion)
      // Not going to the boundaries, stop here
      if (lastY != loc.getBlockY() && !boundaries)
        break;
    }

    // No results yielded
    if (lastY == loc.getBlockY())
      return Optional.empty();

    // Return the result
    loc.setY(lastY);
    return Optional.of(loc);
  }
}
