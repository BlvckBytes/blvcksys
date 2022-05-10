package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.models.SequenceSortResult;
import me.blvckbytes.blvcksys.handlers.IHologramHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.ModelNotFoundException;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Sort the lines of a hologram by their IDs
*/
@AutoConstruct
public class HoloSortCommand extends APlayerCommand {

  private final IHologramHandler holo;
  private final IPersistence pers;

  public HoloSortCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHologramHandler holo,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "holosort",
      "Sort all lines of a hologram",
      PlayerPermission.COMMAND_HOLO,
      new CommandArgument("<name>", "Name of the hologram"),
      new CommandArgument("<ids>", "Line IDs in desired order")
    );

    this.holo = holo;
    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest existing holograms
    if (currArg == 0)
      return pers.listRaw(HologramLineModel.class, "name")
        .stream()
        .map(e -> e.get("name"))
        .filter(Objects::nonNull)
        .map(Object::toString);

    // Suggest ids placeholder
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    String name = argval(args, 0);
    String[] idStrs = argvar(args, 1).split(" ");

    try {
      // Parse all string IDs into integers
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
      Tuple<SequenceSortResult, Integer> result = holo.sortHologramLines(name, ids);

      switch (result.a()) {
        // Successfully sorted
        case SORTED -> {
          p.sendMessage(
            cfg.get(ConfigKey.COMMAND_HOLOSORT_SORTED)
              .withPrefix()
              .withVariable("name", name)
              .asScalar()
          );
        }

        // One of the specified IDs was invalid
        case ID_INVALID -> {
          p.sendMessage(
            cfg.get(ConfigKey.COMMAND_HOLOSORT_INVALID_ID)
              .withPrefix()
              .withVariable("invalid_id", result.b())
              .asScalar()
          );
        }

        // There are some IDs missing
        case IDS_MISSING -> {
          p.sendMessage(
            cfg.get(ConfigKey.COMMAND_HOLOSORT_MISSING_IDS)
              .withPrefix()
              .withVariable("num_missing", result.b())
              .asScalar()
          );
        }
      }
    }

    // Specified a non-existing name
    catch (ModelNotFoundException e) {
      p.sendMessage(
        cfg.get(ConfigKey.COMMAND_HOLOSORT_NOT_EXISTING)
          .withPrefix()
          .withVariable("name", name)
          .asScalar()
      );
    }
  }
}
