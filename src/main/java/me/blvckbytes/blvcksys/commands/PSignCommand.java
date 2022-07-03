package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.adapters.IRegionAdapter;
import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerSignHandler;
import me.blvckbytes.blvcksys.persistence.models.PlayerSignModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Manage player signs and their displayed lines.
*/
@AutoConstruct
public class PSignCommand extends APlayerCommand {

  private final IPlayerSignHandler psign;
  private final IRegionAdapter regions;

  private enum PSignAction {
    DELETE,
    CREATE,
    SETLINE,
    LISTLINES
  }

  public PSignCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPlayerSignHandler psign,
    @AutoInject IRegionAdapter regions
  ) {
    super(
      plugin, logger, cfg, refl,
      "psign",
      "Manage player signs and their lines",
      PlayerPermission.COMMAND_PSIGN.toString(),
      new CommandArgument("<action>", "Action to perform"),
      new CommandArgument("[line id]", "ID of the line to set (1-4)"),
      new CommandArgument("[text]", "The text to set to")
    );

    this.psign = psign;
    this.regions = regions;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest actions
    if (currArg == 0)
      return suggestEnum(args, currArg, PSignAction.class);

    // Suggest lines
    if (currArg == 1)
      return Stream.of("1", "2", "3", "4");

    // Suggest placeholders
    return Stream.of(getArgumentPlaceholder(currArg));
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    PSignAction action = parseEnum(PSignAction.class, args, 0, null);
    Block b = p.getTargetBlockExact(10, FluidCollisionMode.NEVER);

    // Not a sign block
    if (b == null || !(b.getState() instanceof Sign s)) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_NOSIGN)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Cannot build here
    Location loc = b.getLocation();
    if (!regions.canBuild(p, loc)) {
      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_NOBUILD)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    String locStr = "(" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ() + ")";

    // Delete the managed sign
    if (action == PSignAction.DELETE) {
      if (!psign.deleteSign(s)) {
        p.sendMessage(
          cfg.get(ConfigKey.PSIGN_NOT_EXISTING)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_DELETED)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Create a new managed sign
    if (action == PSignAction.CREATE) {
      if (psign.createSign(p, s).isEmpty()) {
        p.sendMessage(
          cfg.get(ConfigKey.PSIGN_EXISTS)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_CREATED)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    if (action == PSignAction.LISTLINES) {
      PlayerSignModel sign = psign.findSign(s).orElse(null);

      if (sign == null) {
        p.sendMessage(
          cfg.get(ConfigKey.PSIGN_NOT_EXISTING)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_LISTLINES)
          .withPrefixes()
          .withVariable("location", locStr)
          .withVariable("creator", sign.getCreator().getName())
          .withVariable(
            "last_editor",
            sign.getLastEditor() == null ?
              "/" :
              sign.getLastEditor().getName()
          )
          .withVariable("created_at", sign.getCreatedAtStr())
          .withVariable("updated_at", sign.getUpdatedAtStr())
          .withVariable("line1", sign.getLine1())
          .withVariable("line2", sign.getLine2())
          .withVariable("line3", sign.getLine3())
          .withVariable("line4", sign.getLine4())
          .asScalar()
      );
      return;
    }

    if (action == PSignAction.SETLINE) {

      int lineId = parseInt(args, 1);
      String content = argvar(args, 2);

      if (lineId <= 0 || lineId > 4) {
        p.sendMessage(
          cfg.get(ConfigKey.PSIGN_INVALID_LINEID)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      if (!psign.editSign(p, s, content, lineId)) {
        p.sendMessage(
          cfg.get(ConfigKey.PSIGN_NOT_EXISTING)
            .withPrefix()
            .asScalar()
        );
        return;
      }

      p.sendMessage(
        cfg.get(ConfigKey.PSIGN_UPDATED)
          .withPrefix()
          .withVariable("line_id", lineId)
          .asScalar()
      );
      return;
    }
  }
}
