package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Get a list of all available kits which shows additional
  personalized information on hovering the individual kits.
*/
@AutoConstruct
public class KitsCommand extends APlayerCommand {

  private final IPersistence pers;

  public KitsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "kits",
      "List all existing kits",
      null
    );

    this.pers = pers;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    List<KitModel> kits = pers.list(KitModel.class);

    TextComponent res = new TextComponent(
      cfg.get(ConfigKey.KIT_LIST_PREFIX)
        .withPrefix()
        .withVariable("count", kits.size())
        .asScalar()
    );

    if (kits.size() == 0)
      res.addExtra(new TextComponent(cfg.get(ConfigKey.KIT_LIST_NO_ITEMS).asScalar()));

    for (int i = 0; i < kits.size(); i++) {
      KitModel kit = kits.get(i);
      TextComponent kitComp = new TextComponent(
        cfg.get(ConfigKey.KIT_LIST_ITEM_FORMAT)
          .withVariable("name", kit.getName())
          .asScalar()
          + (i == kits.size() - 1 ? "" : ", ")
      );

      kitComp.setHoverEvent(new HoverEvent(
        HoverEvent.Action.SHOW_TEXT,
        new Text(
          cfg.get(ConfigKey.KIT_LIST_HOVER)
            .withVariable("created_at", kit.getCreatedAtStr())
            .withVariable("updated_at", kit.getUpdatedAtStr())
            .withVariable("creator", kit.getCreator().getName())
            .withVariable("num_items", kit.getNumItems())
            .withVariable("cooldown_dur", "§cTO BE IMPLEMENTED")
            .asScalar()
        )
      ));

      kitComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit " + kit.getName()));

      res.addExtra(kitComp);
    }

    p.spigot().sendMessage(res);
  }
}
