package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IIgnoreHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.PlayerIgnoreModel;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/23/2022

  View and manage a specific player ignore.
*/
@AutoConstruct
public class IgnoreDetailGui extends AGui<OfflinePlayer> {

  private final IIgnoreHandler ignore;

  @AutoInjectLate
  private IgnoresGui ignoresGui;

  public IgnoreDetailGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IIgnoreHandler ignore
  ) {
    super(4, "", i -> (
      cfg.get(ConfigKey.GUI_IGNORE_TITLE)
        .withVariable("target", i.getArg().getName())
    ), plugin, cfg, textures);

    this.ignore = ignore;
  }

  @Override
  protected void prepare() {
    addFill(Material.BLACK_STAINED_GLASS_PANE);
    addBack("27", ignoresGui, null, AnimationType.SLIDE_RIGHT);

    fixedItem("12", i -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_IGNORE_MSG_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_IGNORE_MSG_LORE)
            .withVariable("state", statePlaceholder(ignore.getMsgIgnore(i.getViewer(), i.getArg())))
            .withVariable("target", i.getArg().getName())
        )
        .build()
    ), null);

    fixedItem("14", i -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_IGNORE_CHAT_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_IGNORE_CHAT_LORE)
            .withVariable("state", statePlaceholder(ignore.getChatIgnore(i.getViewer(), i.getArg())))
            .withVariable("target", i.getArg().getName())
        )
        .build()
    ), null);

    addStateToggle("21", "12", i -> ignore.getMsgIgnore(i.getViewer(), i.getArg()), (s, i) -> ignore.setMsgIgnore(i.getViewer(), i.getArg(), !s));
    addStateToggle("23", "14", i -> ignore.getChatIgnore(i.getViewer(), i.getArg()), (s, i) -> ignore.setChatIgnore(i.getViewer(), i.getArg(), !s));
  }

  @Override
  protected void closed(Player viewer) {
  }

  @Override
  protected void opening(Player viewer, GuiInstance<OfflinePlayer> inst) {
  }
}
