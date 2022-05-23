package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/23/2022

  Offers a quick alias to warp to the spawn warping point.
 */
@AutoConstruct
public class SpawnCommand extends APlayerCommand {

  // Name of the spawn warping point
  private final static String WARP_NAME = "spawn";

  private final IWarpCommand warps;

  public SpawnCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarpCommand warps
  ) {
    super(
      plugin, logger, cfg, refl,
      "spawn",
      "Teleport to the spawn point",
      null
    );

    this.warps = warps;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    boolean res = warps.invokeWarping(p, WARP_NAME, () -> {
      p.sendMessage(
        cfg.get(ConfigKey.SPAWN_TELEPORTED)
          .withPrefix()
          .asScalar()
      );
    });

    if (!res) {
      p.sendMessage(
        cfg.get(ConfigKey.SPAWN_NOT_SET)
          .withPrefix()
          .asScalar()
      );
    }
  }
}
