package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Retrieve existing kits and check out their contents.
*/
@AutoConstruct
public class KitsGui extends AGui implements Listener, IAutoConstructed {

  private final IPersistence pers;
  private final TimeUtil time;

  public KitsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPersistence pers,
    @AutoInject TimeUtil time
  ) {
    super(3, cfg, plugin, p -> cfg.get(ConfigKey.GUI_KITS_TITLE));

    this.pers = pers;
    this.time = time;

    this.setupItems();
  }

  protected void setupItems() {
    withItem("0", (p, s) -> (
        new ItemStackBuilder(Material.REDSTONE)
          .withName(
            cfg.get(ConfigKey.GUI_KITS_REDSTONE_NAME)
              .withVariable("viewer", p.getName())
          )
          .withLore(
            cfg.get(ConfigKey.GUI_KITS_REDSTONE_LORE)
              .withVariable("slot", s)
              .withVariable("viewer", p.getName())
          )
      ),
      (p, s) -> {
        p.sendMessage("§aYou clicked the redstone item!");
        redraw(p, "0");
      }
    );

    withItem("1-8", (p, s) -> new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE), (p, s) -> {
      p.sendMessage("§bYou clicked a glass pane!");
    });
  }

  @Override
  protected void closed(Player viewer) {
  }

  @Override
  protected void opening(Player viewer, GuiInstance inst) {
    List<KitModel> kits = pers.list(KitModel.class);

    for (KitModel kit : kits) {
      inst.additem((p, s) -> {

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
      }, (p, s) -> p.performCommand("kit " + kit.getName()), 10);
    }
  }

  @Override
  public void cleanup() {
    stopTicker();
  }

  @Override
  public void initialize() {}
}
