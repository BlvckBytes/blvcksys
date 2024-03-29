package me.blvckbytes.blvcksys.commands;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.ChatUtil;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  6reated On: 05/01/2022

  Open an empty inventory to quickly dispose unneeded items. The trash-can empties
  itself after a specified timeout of not being restored. When the module gets unloaded,
  all trashcans are dumped back into their owner's inventories.
 */
@AutoConstruct
public class TrashCommand extends APlayerCommand implements Listener, IAutoConstructed {

  @AllArgsConstructor
  private static class TrashCan {
    private Inventory inv;
    private int deletionTimeout;
  }

  // Timeout for automatic trash deletion in seconds
  private final static long TRASH_TIMEOUT_S = 15;

  // Mapping players to their trash-can inventories
  private final Map<Player, TrashCan> trashCans;

  private final ChatUtil chat;
  private final IGiveCommand give;

  public TrashCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject ChatUtil chat,
    @AutoInject IGiveCommand give
  ) {
    super(
      plugin, logger, cfg, refl,
      "trash",
      "A fast way to dispose of items",
      PlayerPermission.COMMAND_TRASH.toString()
    );

    this.chat = chat;
    this.give = give;

    this.trashCans = new HashMap<>();
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    openTrashCan(p);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    // Empty out all trash cans before they go out of memory
    for (Map.Entry<Player, TrashCan> can : trashCans.entrySet()) {

      // Give all items back (or drop them)
      int totalDropped = 0;
      for (ItemStack drop : can.getValue().inv.getContents()) {
        if (drop != null && drop.getType() != Material.AIR)
          totalDropped += this.give.giveItemsOrDrop(can.getKey(), drop);
      }

      // Inform about dump
      can.getKey().sendMessage(
        cfg.get(ConfigKey.TRASH_DUMPED)
          .withPrefix()
          .asScalar()
      );

      // Inform about dropped items
      if (totalDropped > 0)
        can.getKey().sendMessage(
          cfg.get(ConfigKey.TRASH_DUMP_DROPPED)
            .withPrefix()
            .withVariable("dropped", totalDropped)
            .asScalar()
        );
    }
  }

  @Override
  public void initialize() {}

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onInvClose(InventoryCloseEvent e) {
    // Not a player
    if (!(e.getPlayer() instanceof Player p))
      return;

    // Has no trash can
    TrashCan can = this.trashCans.get(p);
    if (can == null)
      return;

    // Didn't close the trash can
    if (!e.getInventory().equals(can.inv))
      return;

    // No items in the trash
    if (can.inv.isEmpty())
      return;

    ChatUtil.ChatPrompt prompt = chat.beginPrompt(
      p, null,
      cfg.get(ConfigKey.TRASH_CONFIRMATION)
        .withPrefixes()
        .withVariable("timeout", TRASH_TIMEOUT_S),
      cfg.get(ConfigKey.CHATBUTTONS_EXPIRED),
      List.of(
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_YES), null, () -> {
          // Cancel the deletion timeout
          Bukkit.getScheduler().cancelTask(can.deletionTimeout);

          // Re-open the inventory
          p.openInventory(can.inv);

          // Inform about the clear cancel
          p.sendMessage(
            cfg.get(ConfigKey.TRASH_CLEARED_CANCELLED)
              .withPrefix()
              .asScalar()
          );
        }),
        new Triple<>(cfg.get(ConfigKey.CHATBUTTONS_NO), null, () -> {
          // Cancel the deletion timeout
          Bukkit.getScheduler().cancelTask(can.deletionTimeout);

          // Clear the inventory
          can.inv.clear();

          // Inform about manual-clear
          p.sendMessage(
            cfg.get(ConfigKey.TRASH_CLEARED_MANUAL)
              .withPrefix()
              .asScalar()
          );
        })
      )
    );

    // Stop previous timeout
    if (can.deletionTimeout > 0)
      Bukkit.getScheduler().cancelTask(can.deletionTimeout);

    // Create deletion timeout
    can.deletionTimeout = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      // Re-set the timeout handle
      can.deletionTimeout = -1;

      // Inform about auto-clear
      p.sendMessage(
        cfg.get(ConfigKey.TRASH_CLEARED_AUTOMATIC)
          .withPrefix()
          .asScalar()
      );

      // Invalidate buttons
      chat.expirePrompt(prompt);

      // Clear the inventory
      can.inv.clear();
    }, TRASH_TIMEOUT_S * 20);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private void openTrashCan(Player p) {
    // Create a new trash-can if it doesn't exist
    if (!this.trashCans.containsKey(p))
      this.trashCans.put(p, new TrashCan(createTrashCan(p), -1));

    p.openInventory(this.trashCans.get(p).inv);
  }

  private Inventory createTrashCan(Player p) {
    return Bukkit.createInventory(
      p, 9 * 3,
      cfg.get(ConfigKey.TRASH_INV_TITLE)
        .withVariable("owner", p.getName())
        .asScalar()
    );
  }
}
