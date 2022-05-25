package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IIgnoreHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.PlayerIgnoreModel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/23/2022

  View and manage your player ignores.
*/
@AutoConstruct
public class IgnoresGui extends AGui<Object> {

  private final IIgnoreHandler ignore;
  private final IgnoreDetailGui ignoreDetailGui;

  public IgnoresGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IIgnoreHandler ignore,
    @AutoInject IgnoreDetailGui ignoreDetailGui
  ) {
    super(4, "10-16,19-25", i -> (
      cfg.get(ConfigKey.GUI_IGNORES_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.ignore = ignore;
    this.ignoreDetailGui = ignoreDetailGui;
  }

  @Override
  protected void prepare() {
    addFill(Material.BLACK_STAINED_GLASS_PANE);
    addPagination("28", "31", "34");
  }

  @Override
  protected void closed(GuiInstance<Object> inst) {
  }

  @Override
  protected void opening(Player viewer, GuiInstance<Object> inst) {
    List<PlayerIgnoreModel> active = ignore.listActiveIgnores(viewer);

    if (active.size() == 0) {
      inst.addPagedItem((i, s) -> (
        new ItemStackBuilder(Material.BARRIER)
          .withName(cfg.get(ConfigKey.GUI_IGNORES_NONE_NAME))
          .withLore(cfg.get(ConfigKey.GUI_IGNORES_NONE_LORE))
          .build()
        ), null, null
      );
      return;
    }

    for (PlayerIgnoreModel ignore : active) {
      inst.addPagedItem((i, s) -> (
        new ItemStackBuilder(textures.getProfileOrDefault(ignore.getTarget().getName()))
          .withName(
            cfg.get(ConfigKey.GUI_IGNORES_PLAYER_NAME)
              .withVariable("name", ignore.getTarget().getName())
          )
          .withLore(
            cfg.get(ConfigKey.GUI_IGNORES_PLAYER_LORE)
              .withVariable("msg_state", statePlaceholder(ignore.isIgnoresMsg()))
              .withVariable("chat_state", statePlaceholder(ignore.isIgnoresChat()))
          )
          .build()
      ), e -> e.getGui().switchTo(e.getGui(), AnimationType.SLIDE_LEFT, ignoreDetailGui, ignore.getTarget()), null);
    }
  }
}