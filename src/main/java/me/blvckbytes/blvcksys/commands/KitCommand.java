package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
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

  private IPersistence pers;
  private IGiveCommand give;

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
      new CommandArgument("[player]", "Player to give the kit to", PlayerPermission.KIT_OTHERS)
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
      p.sendMessage("§cThe kit '" + name + "' does not exist!");
      return;
    }

    KitModel kit = kitO.get();
    int dropped = 0;

    for (ItemStack item : kit.getItems().getContents()) {
      if (item == null || item.getType() == Material.AIR)
        continue;

      dropped += give.giveItemsOrDrop(target, item);
    }

    if (p.equals(target))
      p.sendMessage("§aYou received the kit '" + name + "'");
    else
      p.sendMessage("§aYou gave the kit '" + name + "' to " + target.getName());

    if (dropped > 0)
      target.sendMessage("§c" + dropped + " items have been dropped!");
  }
}
