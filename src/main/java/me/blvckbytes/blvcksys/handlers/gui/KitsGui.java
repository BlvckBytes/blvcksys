package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.commands.IKitCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.events.NpcInteractEvent;
import me.blvckbytes.blvcksys.events.NpcInteraction;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.KitModel;
import me.blvckbytes.blvcksys.util.TimeUtil;
import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Retrieve existing kits and check out their contents.
*/
@AutoConstruct
public class KitsGui extends AGui<Object> implements Listener {

  // Name of the NPC that will trigger opening this GUI
  private final static String NPC_NAME = "kits_gui";

  // Mapping players to a map of their kits and a tuple of <cacheCreation, remainingSeconds>
  private final Map<Player, Map<KitModel, Tuple<Long, Long>>> cooldownCaches;

  private final IPersistence pers;
  private final TimeUtil time;
  private final IStdGuiItemProvider stdGuiItemProvider;

  @AutoInjectLate
  private KitContentGui kitContentGui;

  public KitsGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPersistence pers,
    @AutoInject TimeUtil time,
    @AutoInject IKitCommand kits,
    @AutoInject IStdGuiItemProvider stdGuiItemProvider
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      cfg.get(ConfigKey.GUI_KITS_TITLE)
        .withVariable("viewer", i.getViewer().getName())
    ), plugin, cfg);

    this.pers = pers;
    this.time = time;

    this.cooldownCaches = new HashMap<>();
    this.stdGuiItemProvider = stdGuiItemProvider;

    // Invalidate the cooldown cache whenever a kit has been requested
    kits.registerRequestInterest((p, kit) -> {
      if (cooldownCaches.containsKey(p))
        cooldownCaches.get(p).remove(kit);
    });
  }

  @Override
  protected boolean closed(GuiInstance<Object> inst) {
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<Object> inst) {
    Player p = inst.getViewer();

    inst.addBorder(stdGuiItemProvider);

    // Paginator
    inst.addPagination("37", "40", "43", stdGuiItemProvider);

    List<KitModel> kits = pers.list(KitModel.class);

    // Create the cooldown map initially
    if (!cooldownCaches.containsKey(p))
      cooldownCaches.put(p, new HashMap<>());
    Map<KitModel, Tuple<Long, Long>> cooldownCache = cooldownCaches.get(p);

    // Add all kits by their representative item
    inst.setPageContents(() -> (
      kits.stream()
        .map(kit -> new GuiItem(
          s -> {
            // Cache this kit's cooldown, if absent
            if (!cooldownCache.containsKey(kit)) {
              cooldownCache.put(kit, new Tuple<>(
                System.currentTimeMillis(),
                kit.getCooldownRemaining(p, pers)
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
            ClickType click = e.getClick();

            // Left click performs a kit request
            if (click == ClickType.LEFT || click == ClickType.SHIFT_LEFT)
              p.performCommand("kit " + kit.getName());

              // Right click performs a switch to the kit content preview
            else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT)
              inst.switchTo(AnimationType.SLIDE_LEFT, kitContentGui, kit);
          }, 10
        ))
        .collect(Collectors.toList())
    ));

    return true;
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    cooldownCaches.remove(e.getPlayer());
  }

  @EventHandler
  public void onNpcInteract(NpcInteractEvent e) {
    if (!e.getNpcName().equals(NPC_NAME))
      return;

    if (e.getType() != NpcInteraction.INTERACTED)
      return;

    show(e.getPlayer(), null, AnimationType.SLIDE_DOWN);
  }
}
