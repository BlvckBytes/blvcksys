package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IObjectiveHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.handlers.IPreferencesHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/23/2022

  View and alter your preferences.
*/
@AutoConstruct
public class PreferencesGui extends AGui<Object> {

  private final IPreferencesHandler prefs;
  private final IObjectiveHandler obj;

  public PreferencesGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IPreferencesHandler prefs,
    @AutoInject IObjectiveHandler obj
  ) {
    super(4, "", i -> (
      cfg.get(ConfigKey.GUI_PREFERENCES_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.prefs = prefs;
    this.obj = obj;
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addFill(Material.BLACK_STAINED_GLASS_PANE);

    inst.fixedItem(11, () -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_MSG_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_MSG_LORE)
            .withVariable("state", inst.statePlaceholder(!prefs.isMsgDisabled(p)))
        )
        .build()
    ), null);

    inst.fixedItem(13, () -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_CHAT_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_CHAT_LORE)
            .withVariable("state", !prefs.isChatHidden(p))
        )
        .build()
    ), null);

    inst.fixedItem(15, () -> (
      new ItemStackBuilder(Material.LADDER)
        .withName(cfg.get(ConfigKey.GUI_PREFERENCES_SCOREBOARD_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_PREFERENCES_SCOREBOARD_LORE)
            .withVariable("state", obj.getSidebarVisibility(p))
        )
        .build()
    ), null);

    inst.addStateToggle(20, 11, () -> !prefs.isMsgDisabled(p), s -> prefs.setMsgDisabled(p, s));
    inst.addStateToggle(22, 13, () -> !prefs.isChatHidden(p), s -> prefs.setChatHidden(p, s));
    inst.addStateToggle(24, 15, () -> obj.getSidebarVisibility(p), s -> obj.setSidebarVisibility(p, !s));

    return true;
  }
}
