package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/23/2022

  Offers a quick alias to warp to the spawn warping point.
 */
@AutoConstruct
public class SpawnCommand extends APlayerCommand implements ISpawnCommand {

  // Name of the spawn warping point
  private final static String WARP_NAME = "spawn";

  private final IPersistence pers;
  private final IWarpCommand warps;

  public SpawnCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarpCommand warps,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "spawn",
      "Teleport to the spawn point",
      null
    );

    this.warps = warps;
    this.pers = pers;
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

  @Override
  public Optional<Location> getSpawn() {
    return pers.findFirst(
      new QueryBuilder<>(
        WarpModel.class,
        "name", EqualityOperation.EQ_IC, WARP_NAME
      )
    ).map(WarpModel::getLoc);
  }
}
