package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Plays the drawing animation for a crate to a the viewer.
*/
@AutoConstruct
public class CrateDrawGui extends AGui<CrateModel> {

  public CrateDrawGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures
  ) {
    super(5, "", i -> (
      cfg.get(ConfigKey.GUI_CRATE_DRAW_NAME).
        withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);
  }

  @Override
  protected void prepare() {
    addBorder(Material.BLACK_STAINED_GLASS_PANE);
    fixedItem("20,21,23,24", i -> new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).build(), null);
    fixedItem("4,22", i -> (
      new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DRAW_INDICATOR_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DRAW_INDICATOR_LORE))
        .build()
    ), null);
  }

  @Override
  protected void closed(GuiInstance<CrateModel> inst) {}

  @Override
  protected void opening(Player viewer, GuiInstance<CrateModel> inst) {}
}
