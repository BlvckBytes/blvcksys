package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IIgnoreHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
  protected boolean closed(GuiInstance<OfflinePlayer> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<OfflinePlayer> inst) {
    Player p = inst.getViewer();

    inst.addFill(new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(ConfigValue.immediate(" ")).build());
    inst.addBack("27", ignoresGui, null, AnimationType.SLIDE_RIGHT);

    inst.fixedItem("12", () -> (
      new ItemStackBuilder(Material.NAME_TAG)
        .withName(cfg.get(ConfigKey.GUI_IGNORE_MSG_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_IGNORE_MSG_LORE)
            .withVariable("state", statePlaceholderED(ignore.getMsgIgnore(p, inst.getArg())))
            .withVariable("target", inst.getArg().getName())
        )
        .build()
    ), null, null);

    inst.fixedItem("14", () -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(cfg.get(ConfigKey.GUI_IGNORE_CHAT_NAME))
        .withLore(
          cfg.get(ConfigKey.GUI_IGNORE_CHAT_LORE)
            .withVariable("state", statePlaceholderED(ignore.getChatIgnore(p, inst.getArg())))
            .withVariable("target", inst.getArg().getName())
        )
        .build()
    ), null, null);

    inst.addStateToggle("21", "12", () -> ignore.getMsgIgnore(p, inst.getArg()), s -> ignore.setMsgIgnore(p, inst.getArg(), !s));
    inst.addStateToggle("23", "14", () -> ignore.getChatIgnore(p, inst.getArg()), s -> ignore.setChatIgnore(p, inst.getArg(), !s));

    return true;
  }
}
