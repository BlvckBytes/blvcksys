package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IMuteHandler;
import me.blvckbytes.blvcksys.handlers.MuteType;
import me.blvckbytes.blvcksys.persistence.models.MuteModel;
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
  Created On: 05/17/2022

  List all mutes a player has received over time.
*/
@AutoConstruct
public class MutesCommand extends APlayerCommand {

  private final IMuteHandler mutes;
  private final TimeUtil time;
  private final IFontWidthTable fwTable;

  public MutesCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IMuteHandler mutes,
    @AutoInject TimeUtil time,
    @AutoInject IFontWidthTable fwTable
  ) {
    super(
      plugin, logger, cfg, refl,
      "mutes",
      "List all mutes of a player",
      PlayerPermission.COMMAND_MUTES,
      new CommandArgument("<name>", "Name of the target player"),
      new CommandArgument("<type>", "Type of mute to list")
    );

    this.mutes = mutes;
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
      return suggestEnum(args, currArg, MuteType.class);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0);
    MuteType type = parseEnum(MuteType.class, args, 1, null);

    List<MuteModel> list = switch (type) {
      case ACTIVE -> mutes.listMutes(target, null, true);
      case REVOKED -> mutes.listMutes(target, true, null);
      case ALL -> mutes.listMutes(target, null, null);
    };

    if (list.size() == 0) {
      p.sendMessage(
        cfg.get(ConfigKey.MUTE_LIST_EMPTY)
          .withPrefix()
          .withVariable("type", type.name())
          .withVariable("target", target.getName())
          .asScalar()
      );
      return;
    }

    TidyTable table = new TidyTable("|", fwTable);

    table.addLines(
      cfg.get(ConfigKey.MUTE_LIST_HEADLINE)
        .withPrefixes()
        .withVariable("type", type.name())
        .withVariable("target", target.getName())
        .asScalar()
    );

    String yesStr = cfg.get(ConfigKey.MUTE_LIST_YES).asScalar();
    String noStr = cfg.get(ConfigKey.MUTE_LIST_NO).asScalar();

    for (MuteModel mute : list) {
      TextComponent muteComp = table.addLine(
        cfg.get(ConfigKey.MUTE_LIST_ENTRY)
          .withPrefix()
          .withVariables(mutes.buildMuteVariables(mute))
          .withVariable("created_at", mute.getCreatedAtStr(true))
          .withVariable("is_active", mute.isActive() ? yesStr : noStr)
          .asScalar()
          .replaceAll("(§.)", mute.isRevoked() ? "$1§m" : "$1")
      );

      String command = "/muteinfo " + mute.getId();

      muteComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
        cfg.get(ConfigKey.MUTE_LIST_HOVER)
          .withVariable("command", command)
          .asScalar()
      )));

      muteComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
    }

    table.displayTo(p);
  }
}
