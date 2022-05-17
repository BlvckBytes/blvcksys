package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IWarnHandler;
import me.blvckbytes.blvcksys.handlers.WarnType;
import me.blvckbytes.blvcksys.persistence.models.WarnModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.TidyTable;
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  List all warns a player has received over time.
*/
@AutoConstruct
public class WarnsCommand extends APlayerCommand {

  private final IWarnHandler warns;
  private final TimeUtil time;
  private final IFontWidthTable fwTable;

  public WarnsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWarnHandler warns,
    @AutoInject TimeUtil time,
    @AutoInject IFontWidthTable fwTable
  ) {
    super(
      plugin, logger, cfg, refl,
      "warns",
      "List all warns of a player",
      PlayerPermission.COMMAND_WARNS,
      new CommandArgument("<name>", "Name of the target player"),
      new CommandArgument("<type>", "Type of warn to list")
    );

    this.warns = warns;
    this.time = time;
    this.fwTable = fwTable;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);

    if (currArg == 1)
      return suggestEnum(args, currArg, WarnType.class);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0);
    WarnType type = parseEnum(WarnType.class, args, 1, null);

    List<WarnModel> list = switch (type) {
      case WARN -> warns.listWarns(target, true, false, null);
      case TEMPWARN -> warns.listWarns(target, false, false, null);
      case ACTIVE -> warns.listWarns(target, null, false, true);
      case REVOKED -> warns.listWarns(target, null, true, null);
      case ALL -> warns.listWarns(target, null, null, null);
    };

    if (list.size() == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.WARN_LIST_EMPTY)
          .withPrefix()
          .withVariable("type", type.name())
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    TidyTable table = new TidyTable("|", fwTable);

    table.addLines(
      cfg.get(ConfigKey.WARN_LIST_HEADLINE)
        .withPrefixes()
        .withVariable("type", type.name())
        .withVariable("target", target.getName())
        .asScalar()
    );

    String yesStr = cfg.get(ConfigKey.WARN_LIST_YES).asScalar();
    String noStr = cfg.get(ConfigKey.WARN_LIST_NO).asScalar();

    for (WarnModel warn : list) {
      TextComponent warnComp = table.addLine(
        cfg.get(ConfigKey.WARN_LIST_ENTRY)
          .withPrefix()
          .withVariables(warns.buildWarnVariables(warn))
          .withVariable("created_at", warn.getCreatedAtStr(true))
          .withVariable(
            "duration",
            warn.getDurationSeconds() == null ?
              cfg.get(ConfigKey.WARN_DURATION_PERMANENT) :
              time.formatDuration(warn.getDurationSeconds())
          )
          .withVariable("is_active", warn.isActive() ? yesStr : noStr)
          .asScalar()
          .replaceAll("(§.)", warn.isRevoked() ? "$1§m" : "$1")
      );

      String command = "/warninfo " + warn.getId();

      warnComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
        cfg.get(ConfigKey.WARN_LIST_HOVER)
          .withVariable("command", command)
          .asScalar()
      )));

      warnComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
    }

    table.displayTo(p);
  }
}
