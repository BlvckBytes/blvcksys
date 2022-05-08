package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
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

  private final IPersistence pers;
  private final ChatUtil chat;

  public SetwarpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "setwarp",
      "Create a new warp at your current location",
      PlayerPermission.COMMAND_SETWARP,
      new CommandArgument("<name>", "Name of the warp")
    );

    this.pers = pers;
    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    Location l = p.getLocation();

    // Check if a warp with this name already exists
    boolean exists = pers.count(
      new QueryBuilder<>(
        WarpModel.class,
        "name", EqualityOperation.EQ_IC, name
      )
    ) > 0;

    if (exists) {
      // Send out an overwrite confirmation prompt
      chat.sendButtons(p, ChatButtons.buildYesNo(
        cfg.get(ConfigKey.WARP_OVERWRITE_PREFIX)
          .withVariable("name", name)
          .withPrefixes()
          .asScalar(),
        plugin, cfg,

        // Yes
        () -> {

          // Get the existing model
          WarpModel existing = pers.findFirst(
            new QueryBuilder<>(
              WarpModel.class,
              "name", EqualityOperation.EQ, name
            )
          ).orElse(null);

          // Got deleted in the meantime
          if (existing == null) {
            p.sendMessage(
              cfg.get(ConfigKey.WARP_NOT_EXISTING)
                .withPrefix()
                .withVariable("name", name)
                .asScalar()
            );

            return;
          }

          // Update the location
          existing.setLoc(p.getLocation());
          pers.store(existing);

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

    // Create the new warp
    WarpModel warp = new WarpModel(name, l, p);
    pers.store(warp);

    p.sendMessage(
      cfg.get(ConfigKey.WARP_CREATED)
        .withPrefix()
        .withVariable("name", name)
        .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
        .asScalar()
    );
  }
}
