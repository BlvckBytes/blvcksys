package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IArmorStandHandler;
import me.blvckbytes.blvcksys.handlers.gui.AnimationType;
import me.blvckbytes.blvcksys.handlers.gui.ArmorStandGui;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.ArmorStandModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Create, delete, move and change the appearance of fake armor stands.
*/
@AutoConstruct
public class ArmorStandCommand extends APlayerCommand {

  private enum ArmorStandAction {
    CREATE,
    DELETE,
    MOVEHERE,
    CUSTOMIZE
  }

  private final IArmorStandHandler stands;
  private final IPersistence pers;
  private final ArmorStandGui armorStandGui;

  public ArmorStandCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IArmorStandHandler stands,
    @AutoInject IPersistence pers,
    @AutoInject ArmorStandGui armorStandGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "armorstands,as",
      "Manage fake Armor Stands",
      PlayerPermission.COMMAND_ARMORSTAND,
      new CommandArgument("<name>", "Name of the armor stand"),
      new CommandArgument("<action>", "Action to perform")
    );

    this.stands = stands;
    this.pers = pers;
    this.armorStandGui = armorStandGui;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestModels(args, currArg, ArmorStandModel.class, "name", pers);

    if (currArg == 1)
      return suggestEnum(args, currArg, ArmorStandAction.class);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    ArmorStandAction action = parseEnum(ArmorStandAction.class, args, 1, null);

    if (action == ArmorStandAction.CREATE) {
      Optional<ArmorStandModel> stand = stands.create(p, name, p.getLocation());

      if (stand.isEmpty()) {
        p.sendMessage(
          cfg.get(ConfigKey.ARMOR_STAND_EXISTS)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.ARMOR_STAND_CREATED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == ArmorStandAction.DELETE) {

      if (!stands.delete(name)) {
        p.sendMessage(
          cfg.get(ConfigKey.ARMOR_STAND_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.ARMOR_STAND_DELETED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == ArmorStandAction.MOVEHERE) {
      if (!stands.move(name, p.getLocation())) {
        p.sendMessage(
          cfg.get(ConfigKey.ARMOR_STAND_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.ARMOR_STAND_MOVED)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    if (action == ArmorStandAction.CUSTOMIZE) {
      ArmorStandModel target = stands.getByName(name).orElse(null);

      if (target == null) {
        p.sendMessage(
          cfg.get(ConfigKey.ARMOR_STAND_NOT_FOUND)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      armorStandGui.show(p, target, AnimationType.SLIDE_UP);
    }
  }
}
