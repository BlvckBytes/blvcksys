package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.adapters.IRegionAdapter;
import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerSignHandler;
import me.blvckbytes.blvcksys.handlers.TriResult;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Move a previously created psign to another sign.
*/
@AutoConstruct
public class MovePSignCommand extends APlayerCommand {

  private final IPlayerSignHandler psign;
  private final IRegionAdapter regions;

  public MovePSignCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPlayerSignHandler psign,
    @AutoInject IRegionAdapter regions
  ) {
    super(
      plugin, logger, cfg, refl,
      "movepsign",
      "Move an existing psign to another location",
      PlayerPermission.COMMAND_PSIGN,
      new CommandArgument("<x>", "X coordinate of the sign to move"),
      new CommandArgument("<y>", "Y coordinate of the sign to move"),
      new CommandArgument("<z>", "Z coordinate of the sign to move")
    );

    this.psign = psign;
    this.regions = regions;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest placeholders
    if (currArg < 3)
      return Stream.of(getArgumentPlaceholder(currArg));
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    float x = parseFloat(args, 0);
    float y = parseFloat(args, 1);
    float z = parseFloat(args, 2);
    Location from = new Location(p.getWorld(), x, y, z);
    String fromStr = "(" + from.getBlockX() + "|" + from.getBlockY() + "|" + from.getBlockZ() + ")";

    // Not a sign block at the from location
    if (!(from.getBlock().getState() instanceof Sign sFrom)) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_LOC_NOSIGN)
          .withPrefix()
          .withVariable("location", fromStr)
          .asScalar()
      );
      return;
    }

    // Cannot build at the from location
    if (!regions.canBuild(p, from)) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_LOC_NOBUILD)
          .withPrefix()
          .withVariable("location", fromStr)
          .asScalar()
      );
      return;
    }

    Block toB = p.getTargetBlockExact(10, FluidCollisionMode.NEVER);

    // Not a sign block at the to location
    if (toB == null || !(toB.getState() instanceof Sign sTo)) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_NOSIGN)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Cannot build at the to location
    if (!regions.canBuild(p, toB.getLocation())) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_NOBUILD)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    TriResult res = psign.moveSign(p, sFrom, sTo);

    if (res == TriResult.SUCC) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_MOVED)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (res == TriResult.ERR) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_MOVE_OCCUPIED)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (res == TriResult.EMPTY) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_MOVE_NOT_EXISTING)
          .withPrefix()
          .withVariable("location", fromStr)
          .asScalar()
      );
      return;
    }
  }
}
