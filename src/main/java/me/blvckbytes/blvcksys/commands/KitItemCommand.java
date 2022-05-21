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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Set the representitive item of a kit.
*/
@AutoConstruct
public class KitItemCommand extends APlayerCommand {

  private final IPersistence pers;

  public KitItemCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "kititem",
      "Set a kit's representitive item",
      PlayerPermission.COMMAND_KITITEM,
      new CommandArgument("<name>", "Name of the kit")
    );

    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestModels(args, currArg, KitModel.class, "name", pers);
    return super.onTabCompletion(p, args, currArg);
  }

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

    ItemStack item = p.getInventory().getItemInMainHand();

    if (item.getType().isAir()) {
      p.sendMessage(
        cfg.get(ConfigKey.KIT_NO_ITEM)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    KitModel kit = res.get();
    kit.setRepresentitiveItem(item);
    pers.store(kit);

    p.sendMessage(
      cfg.get(ConfigKey.KIT_ITEM_SET)
        .withPrefix()
        .withVariable("name", kit.getName())
        .withVariable("item", item.getType().name())
        .asScalar()
    );
  }
}
