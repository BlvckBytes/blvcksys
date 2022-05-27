package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.handlers.gui.CrateContentGui;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import me.blvckbytes.blvcksys.persistence.models.SequenceSortResult;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Sort the items within a crate.
*/
@AutoConstruct
public class CrateSortCommand extends APlayerCommand {

  private final IPersistence pers;
  private final ICrateHandler crateHandler;
  private final CrateContentGui crateContentGui;

  public CrateSortCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject IPersistence pers,
    @AutoInject CrateContentGui crateContentGui
  ) {
    super(
      plugin, logger, cfg, refl,
      "cratesort",
      "Sort the items within a crate",
      PlayerPermission.COMMAND_CRATE,
      new CommandArgument("<name>", "Name of the crate"),
      new CommandArgument("[ids]", "Item IDs in desired order")
    );

    this.pers = pers;
    this.crateHandler = crateHandler;
    this.crateContentGui = crateContentGui;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestModels(args, currArg, CrateModel.class, "name", pers);
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    String idStr = argvar(args, 1, "");

    // Display current items and their ids
    if (idStr.isBlank()) {
      List<CrateItemModel> items = crateHandler.getItems(name).orElse(null);

      if (items == null) {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATE_NOT_EXISTING)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_CRATESORT_LIST_HEADER)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );

      for (int i = 0; i < items.size(); i++) {
        CrateItemModel item = items.get(i);

        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATESORT_LIST_ENTRY)
            .withPrefix()
            .withVariable("index", i + 1)
            .withVariable("name", crateContentGui.getItemName(item))
            .asScalar()
        );
      }
      return;
    }

    // Parse all string IDs into integers
    String[] idStrs = idStr.split(" ");
    int[] ids = new int[idStrs.length];
    for (int i = 0; i < ids.length; i++) {
      try {
        ids[i] = Integer.parseInt(idStrs[i]);
      }

      // Specified a non-numeric ID
      catch (NumberFormatException e) {
        p.sendMessage(
          cfg.get(ConfigKey.ERR_INTPARSE)
            .withPrefix()
            .withVariable("number", idStrs[i])
            .asScalar()
        );
        return;
      }
    }

    // Try to sort the lines and check if there were IDs missing
    Tuple<SequenceSortResult, Integer> result = crateHandler.sortItems(name, ids);

    switch (result.a()) {
      // Successfully sorted
      case SORTED -> {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATESORT_SORTED)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
      }

      // One of the specified IDs was invalid
      case ID_INVALID -> {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATESORT_INVALID_ID)
            .withPrefix()
            .withVariable("invalid_id", result.b())
            .asScalar()
        );
      }

      // There are some IDs missing
      case IDS_MISSING -> {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATESORT_MISSING_IDS)
            .withPrefix()
            .withVariable("num_missing", result.b())
            .asScalar()
        );
      }

      // There is no model with this name
      case MODEL_UNKNOWN -> {
        p.sendMessage(
          cfg.get(ConfigKey.COMMAND_CRATESORT_NOT_EXISTING)
            .withPrefix()
            .withVariable("name", name)
            .asScalar()
        );
      }
    }
  }
}
