package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
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
import java.util.stream.Collectors;

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
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addFill(new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(ConfigValue.immediate(" ")).build());
    inst.addPagination(28, 31, 34);

    inst.setPageContents(() -> {
      List<PlayerIgnoreModel> active = ignore.listActiveIgnores(p);

      // No ignores available
      if (active.size() == 0) {
        return List.of(
          new GuiItem(s -> (
            new ItemStackBuilder(Material.BARRIER)
              .withName(cfg.get(ConfigKey.GUI_IGNORES_NONE_NAME))
              .withLore(cfg.get(ConfigKey.GUI_IGNORES_NONE_LORE))
              .build()
            ), null, null
          )
        );
      }

      return active.stream()
        .map(ignore -> new GuiItem(
          s -> (
            new ItemStackBuilder(textures.getProfileOrDefault(ignore.getTarget().getName()))
              .withName(
                cfg.get(ConfigKey.GUI_IGNORES_PLAYER_NAME)
                  .withVariable("name", ignore.getTarget().getName())
              )
              .withLore(
                cfg.get(ConfigKey.GUI_IGNORES_PLAYER_LORE)
                  .withVariable("msg_state", statePlaceholderED(ignore.isIgnoresMsg()))
                  .withVariable("chat_state", statePlaceholderED(ignore.isIgnoresChat()))
              )
              .build()
          ),
          e -> inst.switchTo(AnimationType.SLIDE_LEFT, ignoreDetailGui, ignore.getTarget()),
          null
        ))
        .collect(Collectors.toList());
    });

    return true;
  }
}
