package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Retrieve existing kits and check out their contents.
*/
@AutoConstruct
public class KitsGui extends AGui {

  private final IConfig cfg;
  private final IPersistence pers;
  private final TimeUtil time;

  public KitsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPersistence pers,
    @AutoInject TimeUtil time
  ) {
    super(4, "10-16,19-25", p -> (
      cfg.get(ConfigKey.GUI_KITS_TITLE)
        .withVariable("viewer", p.getName())
    ), plugin);

    this.pers = pers;
    this.time = time;
    this.cfg = cfg;

    this.setupFixedItems();
  }

  protected void setupFixedItems() {
    fixedItem("0-9,17,18,26,27-35", g -> (
      new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE)
    ), null);

    fixedItem("28", g -> (
      new ItemStackBuilder(Material.ARROW)
        .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_NAME))
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_LORE))
    ), e -> {
      e.gui().previousPage();
      e.gui().redraw("31");
    });

    fixedItem("31", g -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(
          cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_NAME)
            .withVariable("curr_page", g.getCurrentPage())
            .withVariable("num_pages", g.getNumPages())
        )
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_LORE))
    ), null);

    fixedItem("34", g -> (
      new ItemStackBuilder(Material.GUNPOWDER)
        .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_LORE))
    ), e -> {
      e.gui().nextPage();
      e.gui().redraw("31");
    });
  }

  @Override
  protected void closed(Player viewer) {}

  @Override
  protected void opening(Player viewer, GuiInstance inst) {
    List<KitModel> kits = pers.list(KitModel.class);

    for (KitModel kit : kits) {
      inst.addPagedItem(g -> {

        // FIXME: Fetching the cooldown from DB on every update period is quite inefficient...
        long rem = kit.getCooldownRemaining(viewer, pers);

        return new ItemStackBuilder(kit.getRepresentitiveItem(), 1)
            .withName(
              cfg.get(ConfigKey.GUI_KITS_KIT_NAME)
                .withVariable("name", kit.getName())
            )
            .withLore(
              cfg.get(ConfigKey.GUI_KITS_KIT_LORE)
                .withVariable("num_items", kit.getNumItems())
                .withVariable("cooldown", rem < 0 ? "/" : time.formatDuration(rem))
            );

      }, e -> {
        if (e.type() == ClickType.LEFT || e.type() == ClickType.SHIFT_LEFT)
          e.gui().getViewer().performCommand("kit " + kit.getName());

        // TODO: Show a preview of the kit when right-clicking
//        else if (e.type() == ClickType.RIGHT || e.type() == ClickType.SHIFT_RIGHT)
      }, 10);
    }
  }
}
