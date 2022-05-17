package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IHomeHandler;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  List all existing homes of yourself or another player.
*/
@AutoConstruct
public class HomesCommand extends APlayerCommand {

  private final IHomeHandler homes;

  public HomesCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IHomeHandler homes
  ) {
    super(
      plugin, logger, cfg, refl,
      "homes",
      "Move an existing home to your current location",
      null,
      new CommandArgument("[player]", "Name of the player to list homes from", PlayerPermission.COMMAND_HOMES_OTHERS)
    );

    this.homes = homes;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//


  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      suggestOfflinePlayers(args, currArg);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0, p);
    List<HomeModel> list = homes.listHomes(target);

    TextComponent head = new TextComponent(
      cfg.get(target.equals(p) ? ConfigKey.HOMES_LIST_PREFIX_SELF : ConfigKey.HOMES_LIST_PREFIX_OTHERS)
        .withPrefix()
        .withVariable("target", target.getName())
        .withVariable("count", list.size())
        .asScalar()
    );

    for (int i = 0; i < list.size(); i++) {
      HomeModel home = list.get(i);

      TextComponent homeComp = new TextComponent(
        cfg.get(ConfigKey.HOMES_LIST_ITEM_FORMAT)
          .withVariable("name", home.getName())
        + (i != list.size() - 1 ? ", " : "")
      );

      Location l = home.getLoc();
      World w = l.getWorld();

      homeComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(
        cfg.get(ConfigKey.HOMES_LIST_HOVER)
          .withVariable("created_at", home.getCreatedAtStr())
          .withVariable("updated_at", home.getUpdatedAtStr())
          .withVariable("world", w == null ? "/" : w.getName())
          .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
          .asScalar()
      )));

      homeComp.setClickEvent(new ClickEvent(
        ClickEvent.Action.RUN_COMMAND,
        "/home " + home.getName() + (target.equals(p) ? "" : " " + target.getName())
      ));

      head.addExtra(homeComp);
    }

    p.spigot().sendMessage(head);
  }
}
