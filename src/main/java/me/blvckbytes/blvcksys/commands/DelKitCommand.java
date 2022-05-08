package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Delete an existing kit by it's name.
*/
@AutoConstruct
public class DelKitCommand extends APlayerCommand {

  private final IPersistence pers;

  public DelKitCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "delkit",
      "Delete an existing kit",
      PlayerPermission.COMMAND_DELKIT,
      new CommandArgument("<name>", "Name of the kit")
    );

    this.pers = pers;
  }

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest existing kits
    if (currArg == 0)
      return pers.listRaw(KitModel.class, "name")
        .stream()
        .map(k -> k.get("name"))
        .filter(Objects::nonNull)
        .map(Object::toString);

    return super.onTabCompletion(p, args, currArg);
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);

    Optional<KitModel> res = pers.findFirst(
      new QueryBuilder<>(
        KitModel.class,
        "name", EqualityOperation.EQ_IC, name
      )
    );

    if (res.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.KIT_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    KitModel kit = res.get();

    pers.delete(kit);
    p.sendMessage(
      cfg.get(ConfigKey.KIT_DELETED)
        .withPrefix()
        .withVariable("name", kit.getName())
        .asScalar()
    );
  }
}
