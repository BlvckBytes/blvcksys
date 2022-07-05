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
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Create a new kit with the contents of your inventory as
  well as a specified cooldown.
*/
@AutoConstruct
public class SetKitCommand extends APlayerCommand {

  private final IPersistence pers;
  private final ChatUtil chat;

  public SetKitCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers,
    @AutoInject ChatUtil chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "setkit",
      "Create a new kit with the contents of your inventory",
      PlayerPermission.COMMAND_SETKIT.toString(),
      new CommandArgument("<name>", "Name of the kit"),
      new CommandArgument("<cooldown unit multiplier>", "Multiplier of the cooldown unit chosen"),
      new CommandArgument("<cooldown unit>", "Unit of time for the cooldown")
    );

    this.pers = pers;
    this.chat = chat;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg < 2)
      return Stream.of(getArgumentPlaceholder(currArg));

    // Suggest cooldown units
    if (currArg == 2)
      return suggestEnum(args, currArg, CooldownUnit.class);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    float cooldown = parseFloat(args, 1);
    CooldownUnit unit = parseEnum(CooldownUnit.class, args, 2, null);

    // Check if this inventory contains any items
    boolean hasItems = false;
    for (ItemStack item : p.getInventory().getContents()) {
      if (item != null && item.getType() != Material.AIR) {
        hasItems = true;
        break;
      }
    }

    if (!hasItems) {
      p.sendMessage(
        cfg.get(ConfigKey.KIT_NO_ITEMS)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Check if a kit with this name already exists
    boolean exists = pers.count(
      new QueryBuilder<>(
        KitModel.class,
        "name", EqualityOperation.EQ_IC, name
      )
    ) > 0;

    if (exists) {
      // Send out an overwrite confirmation prompt
      chat.beginPrompt(
        p, null,
        cfg.get(ConfigKey.KIT_OVERWRITE_PREFIX)
          .withVariable("name", name)
          .withPrefixes(),
        cfg.get(ConfigKey.CHATBUTTONS_EXPIRED),
        List.of(
          new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_YES), null, () -> {
            // Get the existing model
            KitModel existing = pers.findFirst(
              new QueryBuilder<>(
                KitModel.class,
                "name", EqualityOperation.EQ, name
              )
            ).orElse(null);

            // Got deleted in the meantime
            if (existing == null) {
              p.sendMessage(
                cfg.get(ConfigKey.KIT_NOT_EXISTING)
                  .withPrefix()
                  .withVariable("name", name)
                  .asScalar()
              );

              return;
            }

            // Update the items
            existing.setItems(p.getInventory());
            pers.store(existing);

            p.sendMessage(
              cfg.get(ConfigKey.KIT_OVERWRITE_SAVED)
                .withPrefix()
                .withVariable("name", name)
                .withVariable("num_items", existing.getNumItems())
                .asScalar()
            );
          }),
          new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_NO), null, () -> {
            p.sendMessage(
              cfg.get(ConfigKey.KIT_OVERWRITE_CANCELLED)
                .withPrefix()
                .asScalar()
            );
          })
        )
      );

      return;
    }

    // Create the new kit
    int cooldownSecs = Math.round(unit.getSeconds() * cooldown);
    KitModel kit = new KitModel(name, p.getInventory(), cooldownSecs, p);
    pers.store(kit);

    p.sendMessage(
      cfg.get(ConfigKey.KIT_CREATED)
        .withPrefix()
        .withVariable("name", name)
        .withVariable("num_items", kit.getNumItems())
        .asScalar()
    );
  }
}
