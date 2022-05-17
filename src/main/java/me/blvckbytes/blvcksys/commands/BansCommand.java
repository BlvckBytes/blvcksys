package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.BanType;
import me.blvckbytes.blvcksys.handlers.IBanHandler;
import me.blvckbytes.blvcksys.persistence.models.BanModel;
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

  List all bans a player has received over time.
*/
@AutoConstruct
public class BansCommand extends APlayerCommand {

  private final IBanHandler bans;
  private final TimeUtil time;
  private final IFontWidthTable fwTable;

  public BansCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IBanHandler bans,
    @AutoInject TimeUtil time,
    @AutoInject IFontWidthTable fwTable
  ) {
    super(
      plugin, logger, cfg, refl,
      "bans",
      "List all bans of a player",
      PlayerPermission.COMMAND_BANS,
      new CommandArgument("<name>", "Name of the target player"),
      new CommandArgument("<type>", "Type of ban to list")
    );

    this.bans = bans;
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
      return suggestEnum(args, currArg, BanType.class);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0);
    BanType type = parseEnum(BanType.class, args, 1, null);

    List<BanModel> list = switch (type) {
      case BAN -> bans.listBans(target, true, false, null, null);
      case IPBAN -> bans.listBans(target, true, true, null, null);
      case TEMPBAN -> bans.listBans(target, false, false, null, null);
      case TEMPIPBAN -> bans.listBans(target, false, true, null, null);
      case ACTIVE -> bans.listBans(target, null, null, false, true);
      case REVOKED -> bans.listBans(target, null, null, true, null);
      case ALL -> bans.listBans(target, null, null, null, null);
    };

    if (list.size() == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.BAN_LIST_EMPTY)
          .withPrefix()
          .withVariable("type", type.name())
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    TidyTable table = new TidyTable("|", fwTable);

    table.addLines(
      cfg.get(ConfigKey.BAN_LIST_HEADLINE)
        .withPrefixes()
        .withVariable("type", type.name())
        .withVariable("target", target.getName())
        .asScalar()
    );

    String yesStr = cfg.get(ConfigKey.BAN_LIST_YES).asScalar();
    String noStr = cfg.get(ConfigKey.BAN_LIST_NO).asScalar();

    for (BanModel ban : list) {
      TextComponent banComp = table.addLine(
        cfg.get(ConfigKey.BAN_LIST_ENTRY)
          .withPrefix()
          .withVariable("creator", ban.getCreator().getName())
          .withVariable("created_at", ban.getCreatedAtStr(true))
          .withVariable(
            "duration",
            ban.getDurationSeconds() == null ?
              cfg.get(ConfigKey.BAN_DURATION_PERMANENT) :
              time.formatDuration(ban.getDurationSeconds())
          )
          .withVariable("has_ip", ban.getIpAddress() == null ? noStr : yesStr)
          .withVariable("is_active", ban.isActive() ? yesStr : noStr)
          .asScalar()
          .replaceAll("(§.)", ban.isRevoked() ? "$1§m" : "$1")
      );

      String command = "/baninfo " + ban.getId();

      banComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
        cfg.get(ConfigKey.BAN_LIST_HOVER)
          .withVariable("command", command)
          .asScalar()
      )));

      banComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
    }

    table.displayTo(p);
  }
}
