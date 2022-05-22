package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
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
public class KitsGui extends AGui<Object> {

  private final IPersistence pers;
  private final TimeUtil time;

  @AutoInjectLate
  private KitContentGui kitContentGui;

  public KitsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPersistence pers,
    @AutoInject TimeUtil time,
    @AutoInject IPlayerTextureHandler textures
  ) {
    super(4, "10-16,19-25", i -> (
      cfg.get(ConfigKey.GUI_KITS_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.pers = pers;
    this.time = time;
  }

  @Override
  protected void prepare() {
    addBorder(Material.BLACK_STAINED_GLASS_PANE);
    addPagination("28", "31", "34");
  }

  @Override
  protected void closed(Player viewer) {}

  @Override
  protected void opening(Player viewer, GuiInstance<Object> inst) {
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
          )
          .build();

      }, e -> {
        if (e.type() == ClickType.LEFT || e.type() == ClickType.SHIFT_LEFT)
          e.gui().getViewer().performCommand("kit " + kit.getName());

        else if (e.type() == ClickType.RIGHT || e.type() == ClickType.SHIFT_RIGHT)
          e.gui().switchTo(kitContentGui, kit);
      }, 10);
    }
  }
}
