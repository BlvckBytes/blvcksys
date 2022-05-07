package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Get a list of all available warps which shows additional
  information on hovering the individual warps.
*/
@AutoConstruct
public class WarpsCommand extends APlayerCommand {

  private final IPersistence pers;

  public WarpsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "warps",
      "List all existing warps",
      null
    );

    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    List<WarpModel> warps = pers.list(WarpModel.class);
    TextComponent res = new TextComponent(
      cfg.get(ConfigKey.WARP_LIST_PREFIX)
        .withPrefix()
        .withVariable("count", warps.size())
        .asScalar()
    );

    if (warps.size() == 0)
      res.addExtra(new TextComponent(cfg.get(ConfigKey.WARP_LIST_NO_ITEMS).asScalar()));

    for (int i = 0; i < warps.size(); i++) {
      WarpModel warp = warps.get(i);
      TextComponent warpComp = new TextComponent(
        cfg.get(ConfigKey.WARP_LIST_ITEM_FORMAT)
          .withVariable("name", warp.getName())
          .asScalar()
        + (i == warps.size() - 1 ? "" : ", ")
      );

      Location l = warp.getLoc();
      World w = l.getWorld();
      warpComp.setHoverEvent(new HoverEvent(
        HoverEvent.Action.SHOW_TEXT,
        new Text(
          cfg.get(ConfigKey.WARP_LIST_HOVER)
            .withVariable("created_at", warp.getCreatedAtStr())
            .withVariable("updated_at", warp.getUpdatedAtStr())
            .withVariable("creator", warp.getCreator().getName())
            .withVariable("world", w == null ? "?" : w.getName())
            .withVariable("location", "(" + l.getBlockX() + " | " + l.getBlockY() + " | " + l.getBlockZ() + ")")
            .asScalar()
        )
      ));

      warpComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + warp.getName()));

      res.addExtra(warpComp);
    }

    p.spigot().sendMessage(res);
  }
}