package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ITeleportationHandler;
import me.blvckbytes.blvcksys.handlers.IWarpHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Teleport to an existing warping-point.
*/
@AutoConstruct
public class WarpCommand extends APlayerCommand implements IWarpCommand {

  private final IWarpHandler warps;
  private final IPersistence pers;
  private final ITeleportationHandler tp;

  public WarpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject ITeleportationHandler tp,
    @AutoInject IWarpHandler warps
  ) {
    super(
      plugin, logger, cfg, refl,
      "warp",
      "Teleport to a warping point",
      null,
      new CommandArgument("<name>", "Name of the warp")
    );

    this.pers = pers;
    this.warps = warps;
    this.tp = tp;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest available warps
    if (currArg == 0)
      return suggestModels(args, currArg, WarpModel.class, "name", pers);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);

    boolean res = invokeWarping(p, name, () -> {
      p.sendMessage(
        cfg.get(ConfigKey.WARP_TELEPORTED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
    });

    if (!res) {
      p.sendMessage(
        cfg.get(ConfigKey.WARP_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
    }
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean invokeWarping(Player p, String name, @Nullable Runnable done) throws PersistenceException {
    Optional<WarpModel> res = warps.getWarp(name);
    if (res.isEmpty())
      return false;

    WarpModel warp = res.get();
    tp.requestTeleportation(p, warp.getLoc(), done, null);
    return true;
  }
}
