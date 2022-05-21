package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Retrieve existing kits and check out their contents.
*/
@AutoConstruct
public class KitsGui extends AGui {

  private final Map<Player, Integer> clickCounts;

  public KitsGui(
    @AutoInject IConfig cfg
  ) {
    super(3, cfg, p -> cfg.get(ConfigKey.GUI_KITS_TITLE));
    this.clickCounts = new HashMap<>();
  }

  @Override
  protected void setupItems(int rows) {
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
              .withVariable("num_clicks", clickCounts.get(p))
          )
      ),
      (p, s) -> {
        p.sendMessage("§aYou clicked the redstone item!");
        clickCounts.put(p, clickCounts.get(p) + 1);
        redraw(p, "0");
      }
    );

    withItem("1-8", (p, s) -> new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE), (p, s) -> {
      p.sendMessage("§bYou clicked a glass pane!");
    });
  }

  @Override
  protected void closed(Player viewer) {
    clickCounts.remove(viewer);
  }

  @Override
  protected void opening(Player viewer) {
    clickCounts.put(viewer, 0);
  }
}
