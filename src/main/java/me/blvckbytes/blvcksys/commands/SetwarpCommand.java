package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IWarpHandler;
import me.blvckbytes.blvcksys.util.ChatButtons;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Set a new warp at your current position with a specified unique name.
*/
@AutoConstruct
public class SetwarpCommand extends APlayerCommand {

  private final IWarpHandler warps;
  private final ChatUtil chat;

  public SetwarpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarpHandler warps,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "setwarp",
      "Create a new warp at your current location",
      PlayerPermission.COMMAND_SETWARP.toString(),
      new CommandArgument("<name>", "Name of the warp")
    );

    this.warps = warps;
    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    Location l = p.getLocation();

    boolean res = warps.setWarp(name, p, l);

    // Warp already existed
    if (!res) {
      // Send out an overwrite confirmation prompt
      chat.sendButtons(p, ChatButtons.buildYesNo(
        cfg.get(ConfigKey.WARP_OVERWRITE_PREFIX)
          .withVariable("name", name)
          .withPrefixes()
          .asScalar(),
        plugin, cfg,

        // Yes
        () -> {

          boolean changed = warps.moveWarp(name, p, l);

          // Got deleted in the meantime
          if (!changed) {
            p.sendMessage(
              cfg.get(ConfigKey.WARP_NOT_EXISTING)
                .withPrefix()
                .withVariable("name", name)
                .asScalar()
            );

            return;
          }

          p.sendMessage(
            cfg.get(ConfigKey.WARP_OVERWRITE_SAVED)
              .withPrefix()
              .withVariable("name", name)
              .asScalar()
          );
        },

        // No
        () -> {
          p.sendMessage(
            cfg.get(ConfigKey.WARP_OVERWRITE_CANCELLED)
              .withPrefix()
              .asScalar()
          );
        },

        null
      ));

      return;
    }

    p.sendMessage(
      cfg.get(ConfigKey.WARP_CREATED)
        .withPrefix()
        .withVariable("name", name)
        .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
        .asScalar()
    );
  }
}
