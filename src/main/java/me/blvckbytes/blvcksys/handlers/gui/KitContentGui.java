package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Displays the contents of a kit.
*/
@AutoConstruct
public class KitContentGui extends AGui<KitModel> {

  @AutoInjectLate
  private KitsGui kitsGui;

  public KitContentGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_KIT_CONTENT_TITLE)
        .withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);
  }

  @Override
  protected void prepare() {
    addBorder(Material.BLACK_STAINED_GLASS_PANE);
    addBack("0", kitsGui, null);
  }

  @Override
  protected void closed(Player viewer) {}

  @Override
  protected void opening(Player viewer, GuiInstance<KitModel> inst) {
    for (ItemStack content : inst.getArg().getItems().getContents()) {
      if (content != null)
        inst.addPagedItem(i -> content, null, null);
    }
  }
}
