package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.commands.IKitCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.util.TimeUtil;
import net.minecraft.util.Tuple;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Retrieve existing kits and check out their contents.
*/
@AutoConstruct
public class KitsGui extends AGui<Object> implements Listener {

  // Mapping players to a map of their kits and a tuple of <cacheCreation, remainingSeconds>
  private final Map<Player, Map<KitModel, Tuple<Long, Long>>> cooldownCaches;

  private final IPersistence pers;
  private final TimeUtil time;

  @AutoInjectLate
  private KitContentGui kitContentGui;

  public KitsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPersistence pers,
    @AutoInject TimeUtil time,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IKitCommand kits
  ) {
    super(4, "10-16,19-25", i -> (
      cfg.get(ConfigKey.GUI_KITS_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg, textures);

    this.pers = pers;
    this.time = time;

    this.cooldownCaches = new HashMap<>();

    // Invalidate the cooldown cache whenever a kit has been requested
    kits.registerRequestInterest((p, kit) -> {
      if (cooldownCaches.containsKey(p)) {
        System.out.println(
          cooldownCaches.get(p).remove(kit)
        );
      }
    });
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

    // Create the cooldown map initially
    if (!cooldownCaches.containsKey(viewer))
      cooldownCaches.put(viewer, new HashMap<>());
    Map<KitModel, Tuple<Long, Long>> cooldownCache = cooldownCaches.get(viewer);

    // Add all kits by their representative item
    for (KitModel kit : kits) {
      inst.addPagedItem(g -> {
        // Cache this kit's cooldown, if absent
        if (!cooldownCache.containsKey(kit)) {
          cooldownCache.put(kit, new Tuple<>(
            System.currentTimeMillis(),
            kit.getCooldownRemaining(viewer, pers)
          ));
        }

        // Calculate the remaining time from the cache's information
        Tuple<Long, Long> remInfo = cooldownCache.get(kit);
        long rem = remInfo.b() - ((System.currentTimeMillis() - remInfo.a()) / 1000);

        return new ItemStackBuilder(kit.getRepresentativeItem(), 1)
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
        // Left click performs a kit request
        if (e.type() == ClickType.LEFT || e.type() == ClickType.SHIFT_LEFT)
          e.gui().getViewer().performCommand("kit " + kit.getName());

        // Right click performs a switch to the kit content preview
        else if (e.type() == ClickType.RIGHT || e.type() == ClickType.SHIFT_RIGHT)
          e.gui().switchTo(kitContentGui, kit);
      }, 10);
    }
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    cooldownCaches.remove(e.getPlayer());
  }
}
