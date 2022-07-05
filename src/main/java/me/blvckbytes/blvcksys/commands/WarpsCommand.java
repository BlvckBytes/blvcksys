package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ITeleportationHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Get a list of all available warps which shows additional
  information on hovering the individual warps.
*/
@AutoConstruct
public class WarpsCommand extends APlayerCommand {

  private final IPersistence pers;
  private final ITeleportationHandler teleportationHandler;
  private final ChatUtil chatUtil;

  public WarpsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject ITeleportationHandler teleportationHandler,
    @AutoInject ChatUtil chatUtil
  ) {
    super(
      plugin, logger, cfg, refl,
      "warps",
      "List all existing warps",
      null
    );

    this.pers = pers;
    this.teleportationHandler = teleportationHandler;
    this.chatUtil = chatUtil;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    List<WarpModel> warps = pers.list(WarpModel.class);

    if (warps.size() == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.WARP_LIST_PREFIX)
          .withPrefix()
          .withVariable("count", warps.size())
          .asScalar() +
        cfg.get(ConfigKey.WARP_LIST_NO_ITEMS)
          .asScalar()
      );
      return;
    }

    List<Triple<ConfigValue, @Nullable ConfigValue, Runnable>> buttons = new ArrayList<>();
    for (int i = 0; i < warps.size(); i++) {
      WarpModel warp = warps.get(i);

      Location l = warp.getLoc();
      World w = l.getWorld();

      buttons.add(new Triple<>(
        cfg.get(ConfigKey.WARP_LIST_ITEM_FORMAT)
          .withVariable("name", warp.getName())
          .withVariable("sep", i == warps.size() - 1 ? "" : ","),
        cfg.get(ConfigKey.WARP_LIST_HOVER)
          .withVariable("created_at", warp.getCreatedAtStr())
          .withVariable("updated_at", warp.getUpdatedAtStr())
          .withVariable("creator", warp.getCreator().getName())
          .withVariable("world", w == null ? "?" : w.getName())
          .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")"),
        () -> {
          teleportationHandler.requestTeleportation(
            p, warp.getLoc(), () -> {
              p.sendMessage(
                cfg.get(ConfigKey.WARP_LIST_TELEPORTED)
                  .withPrefix()
                  .withVariable("name", warp.getName())
                  .asScalar()
              );
            }, null
          );
        }
      ));
    }

    chatUtil.beginPrompt(
      p, null,
      cfg.get(ConfigKey.WARP_LIST_PREFIX)
        .withPrefix()
        .withVariable("count", warps.size()),
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED).withPrefix(),
      buttons
    );
  }
}