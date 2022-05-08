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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Obtain a kit by it's name.
*/
@AutoConstruct
public class KitCommand extends APlayerCommand {

  private final IPersistence pers;
  private final IGiveCommand give;

  public KitCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject IGiveCommand give
  ) {
    super(
      plugin, logger, cfg, refl,
      "kit",
      "Obtain a specific kit by it's name",
      null,
      new CommandArgument("<name>", "Name of the kit"),
      new CommandArgument("[player]", "Player to give the kit to", PlayerPermission.COMMAND_KIT_OTHERS)
    );

    this.pers = pers;
    this.give = give;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest existing kits
    if (currArg == 0)
      return pers.listRaw(KitModel.class, "name")
        .stream()
        .map(k -> k.get("name"))
        .filter(Objects::nonNull)
        .map(Object::toString);

    if (currArg == 1)
      return suggestOnlinePlayers(p, args, currArg, false);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    Player target = onlinePlayer(args, 1, p);

    Optional<KitModel> kitO = pers.findFirst(
      new QueryBuilder<>(
        KitModel.class,
        "name", EqualityOperation.EQ_IC, name
      )
    );

    if (kitO.isEmpty()) {
      p.sendMessage(
        cfg.get(ConfigKey.KIT_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
      return;
    }

    KitModel kit = kitO.get();

    cooldownGuard(
      p, pers, kit,
      cfg.get(ConfigKey.KIT_COOLDOWN)
        .withPrefix()
        .withVariable("name", kit.getName())
    );

    // Count the number of dropped items
    int numDropped = 0;
    for (ItemStack item : kit.getItems().getContents()) {
      if (item == null || item.getType() == Material.AIR)
        continue;

      numDropped += give.giveItemsOrDrop(target, item);
    }

    if (p.equals(target)) {
      p.sendMessage(
        cfg.get(ConfigKey.KIT_CONSUMED_SELF)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
    } else {
      p.sendMessage(
        cfg.get(ConfigKey.KIT_CONSUMED_OTHERS_SENDER)
          .withPrefix()
          .withVariable("name", name)
          .withVariable("target", target.getName())
          .asScalar()
      );

      // Inform the sender about the drop too
      if (numDropped > 0)
        p.sendMessage(
          cfg.get(ConfigKey.KIT_CONSUMED_DROPPED)
            .withPrefix()
            .withVariable("num_dropped", numDropped)
            .asScalar()
        );

      target.sendMessage(
        cfg.get(ConfigKey.KIT_CONSUMED_OTHERS_RECEIVER)
          .withPrefix()
          .withVariable("name", name)
          .withVariable("issuer", p.getName())
          .asScalar()
      );
    }

    // Inform the target about the drop
    if (numDropped > 0)
      target.sendMessage(
        cfg.get(ConfigKey.KIT_CONSUMED_DROPPED)
          .withPrefix()
          .withVariable("num_dropped", numDropped)
          .asScalar()
      );
  }
}