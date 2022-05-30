package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
  protected boolean closed(GuiInstance<KitModel> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<KitModel> inst) {
    inst.addBorder(Material.BLACK_STAINED_GLASS_PANE);
    inst.addBack(36, kitsGui, null, AnimationType.SLIDE_RIGHT);

    for (ItemStack content : inst.getArg().getItems().getContents()) {

      if (content == null)
        continue;

      ItemMeta meta = content.getItemMeta();
      String name = meta == null ? null : meta.getDisplayName();

      inst.addPagedItem(s -> (
        new ItemStackBuilder(content, content.getAmount())
          // Keep either the name from ItemMeta or set a human readable type fallback
          .withName(
            (name != null && !name.isBlank()) ? ConfigValue.immediate(name) :
            cfg.get(ConfigKey.GUI_KIT_CONTENT_CONTENT_NAME)
              .withVariable(
                "hr_type",
                WordUtils.capitalizeFully(content.getType().name().replace("_", " "))
              )
          )
          .withLore(
            cfg.get(ConfigKey.GUI_KIT_CONTENT_CONTENT_LORE)
              .withVariable("name", inst.getArg().getName())
          )
          .build()
      ), null, null);
    }

    return true;
  }
}
