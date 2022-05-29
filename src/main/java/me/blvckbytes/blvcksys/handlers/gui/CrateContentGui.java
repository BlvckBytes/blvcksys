package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import net.minecraft.util.Tuple;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Displays the contents of a crate.
*/
@AutoConstruct
public class CrateContentGui extends AGui<Tuple<CrateModel, Boolean>> {

  private final ICrateHandler crateHandler;
  private final CrateItemDetailGui detailGui;

  public CrateContentGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject CrateItemDetailGui detailGui
  ) {
    super(6, "10-16,19-25,28-34,37-43", i -> (
      cfg.get(ConfigKey.GUI_CRATE_CONTENT_NAME)
        .withVariable("name", i.getArg().a().getName())
    ), plugin, cfg, textures);

    this.crateHandler = crateHandler;
    this.detailGui = detailGui;
  }

  @Override
  protected boolean closed(GuiInstance<Tuple<CrateModel, Boolean>> inst) {
    return false;
  }

  @Override
  protected boolean opening(Player viewer, GuiInstance<Tuple<CrateModel, Boolean>> inst) {
    inst.addBorder(Material.BLACK_STAINED_GLASS_PANE);
    inst.addPagination(46, 49, 52);

    CrateModel crate = inst.getArg().a();
    boolean editMode = inst.getArg().b();

    List<CrateItemModel> items = crateHandler.getItems(crate.getName())
      .orElse(new ArrayList<>());

    if (items.size() == 0) {
      inst.addPagedItem((i, s) -> (
          new ItemStackBuilder(Material.BARRIER)
            .withName(cfg.get(ConfigKey.GUI_CRATE_CONTENT_NONE_NAME))
            .withLore(cfg.get(ConfigKey.GUI_CRATE_CONTENT_NONE_LORE))
            .build()
        ), null, null
      );
      return true;
    }

    for (CrateItemModel content : items) {

      if (content.getItem() == null)
        continue;

      inst.addPagedItem((i, s) -> appendDecoration(crate, content), e -> {
        if (!editMode)
          return;

        e.getGui().switchTo(AnimationType.SLIDE_LEFT, detailGui, new Tuple<>(crate, content));
      }, null);
    }

    return true;
  }

  /**
   * Get an item's name by either it's item meta or by transforming
   * the material type to a human readable string
   * @param content Item to get the name for
   * @return Name to display
   */
  public ConfigValue getItemName(CrateItemModel content) {
    ItemMeta meta = content.getItem().getItemMeta();
    String metaName = meta == null ? null : meta.getDisplayName();

    return (metaName != null && !metaName.isBlank()) ? ConfigValue.immediate(metaName) :
      cfg.get(ConfigKey.GUI_CRATE_CONTENT_CONTENT_NAME)
        .withVariable(
          "hr_type",
          WordUtils.capitalizeFully(content.getItem().getType().name().replace("_", " "))
        );
  }

  /**
   * Appends name and lore decoration to a crate's item in order to display it properly
   * @param crate Containing crate
   * @param content Item to decorate
   * @return Decorated item
   */
  public ItemStack appendDecoration(CrateModel crate, CrateItemModel content) {
    return new ItemStackBuilder(content.getItem(), content.getItem().getAmount())
      // Keep either the name from ItemMeta or set a human readable type fallback
      .withName(getItemName(content))
      .withLore(
        cfg.get(ConfigKey.GUI_CRATE_CONTENT_CONTENT_LORE)
          .withVariable("name", crate.getName())
          .withVariable("probability", Math.round(content.getProbability() * 100F) / 100F)
      )
      .build();
  }
}
