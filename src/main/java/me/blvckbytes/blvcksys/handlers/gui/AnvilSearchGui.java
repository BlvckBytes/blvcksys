package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.packets.ModificationPriority;
import me.blvckbytes.blvcksys.packets.PacketSource;
import me.blvckbytes.blvcksys.packets.communicators.fakeitem.IFakeItemCommunicator;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInItemName;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/30/2022

  Offers the viewer an anvil inventory to type searches into while their
  own inventory is used to display results live, using fake items.
*/
@AutoConstruct
public class AnvilSearchGui extends AGui<SingleChoiceParam> implements IPacketModifier {

  // Whether the player has made a selection yet
  private final Set<Player> madeSelection;

  // Mapping players to their current fake inventory layout (results)
  private final Map<Player, Map<Integer, Object>> currentInventory;

  private final MCReflect refl;
  private final ILogger logger;
  private final IFakeItemCommunicator fakeItem;

  public AnvilSearchGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IPacketInterceptor packetInterceptor,
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger,
    @AutoInject IFakeItemCommunicator fakeItem
  ) {
    super(3, "", i -> (
      ConfigValue.immediate(i.getArg().type())
    ), plugin, cfg, textures, InventoryType.ANVIL);

    this.refl = refl;
    this.logger = logger;
    this.fakeItem = fakeItem;

    this.madeSelection = new HashSet<>();
    this.currentInventory = new HashMap<>();

    packetInterceptor.register(this, ModificationPriority.LOW);
  }

  @Override
  protected boolean closed(GuiInstance<SingleChoiceParam> inst) {
    Player p = inst.getViewer();

    // Restore the inventory contents again by updating the inv
    Bukkit.getScheduler().runTask(plugin, p::updateInventory);

    if (!madeSelection.remove(p) && inst.getArg().closed() != null)
      inst.getArg().closed().accept(inst);

    currentInventory.remove(p);
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<SingleChoiceParam> inst) {
    IStdGuiItemsProvider itemsProvider = inst.getArg().itemsProvider();

    // This item serves as a placeholder to get the typing functionality up and working, while
    // it also informes the player about the concept of filtering
    inst.fixedItem("0", () -> (
      itemsProvider.getItem(StdGuiItem.SEARCH_PLACEHOLDER, null)
    ), null, null);

    // Back button
    if (inst.getArg().backButton() != null) {
      inst.addBack("1", itemsProvider, e -> {
        madeSelection.add(inst.getViewer());
        inst.getArg().backButton().accept(inst);
      });
    }

    return true;
  }

  //=========================================================================//
  //                                Interceptor                              //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(UUID sender, PacketSource ps, Packet<?> incoming) {
    Player p = Bukkit.getPlayer(sender);
    if (
      // Only listen for players which are in an active instance
      p != null &&
      getActiveInstances().containsKey(p) &&
      incoming instanceof PacketPlayInItemName pin
    ) {
      GuiInstance<SingleChoiceParam> inst = findInst(p).orElse(null);

      // Has an open instance
      if (inst != null) {
        try {
          // Filter the representitives by the search string
          String search = refl.getFieldByType(pin, String.class, 0);
          List<Tuple<Object, ItemStack>> results = filterRepresentitives(inst, search);

          if (!currentInventory.containsKey(p))
            currentInventory.put(p, new HashMap<>());

          // Set results
          int j, invSize = p.getInventory().getSize();
          for (j = 0; j < Math.min(results.size(), invSize); j++) {
            int slot = translateSlot(j);
            Tuple<Object, ItemStack> item = results.get(j);

            fakeItem.setFakeSlot(p, item.b(), slot);
            currentInventory.get(p).put(slot, item.a());
          }

          // Clear all remaining slots
          for (; j < invSize; j++) {
            int slot = translateSlot(j);

            fakeItem.setFakeSlot(p, null, slot);
            currentInventory.get(p).remove(slot);
          }

        } catch (Exception e) {
          logger.logError(e);
        }

        // Drop the packet
        return null;
      }
    }
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    return outgoing;
  }

  //=========================================================================//
  //                                  Listener                               //
  //=========================================================================//

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // Not their own inv (which is used for the fake items)
    if (e.getClickedInventory() != p.getInventory())
      return;

    // Has no active instance
    GuiInstance<SingleChoiceParam> inst = findInst(p).orElse(null);
    if (inst == null)
      return;

    // Always cancel clicks
    e.setCancelled(true);

    // Has no items displayed yet
    if (!currentInventory.containsKey(p))
      return;

    // Clicked on a vacant slot
    Object selection = currentInventory.get(p).get(e.getSlot());
    if (selection == null)
      return;

    // Call the selection callback
    madeSelection.add(p);
    inst.getArg().selected().accept(selection, inst);
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Translates a player inventory slot so that the slots start in the top
   * left corner and go down in serpentines. Usually, the hotbar leftmost slot
   * is zero and it then wraps around to the top left corner, which looks off.
   * @param slot Slot to translate
   * @return Translated slot
   */
  private int translateSlot(int slot) {
    int size = 4 * 9;

    // Out of bounds, leave as is
    if (slot < 0 || slot >= size)
      return slot;

    // Just shift down all slots by a row and wrap around
    return (slot + 9) % size;
  }

  /**
   * Get the active instance of this GUI from a player
   * @param p Target player
   * @return Optional instance, empty if there is none
   */
  private Optional<GuiInstance<SingleChoiceParam>> findInst(Player p) {
    if (!getActiveInstances().containsKey(p))
      return Optional.empty();
    return getActiveInstances().get(p).stream().findFirst();
  }

  /**
   * Filter the available representitive items according to the provided search term
   * @param inst Instance ref to get the representitives from
   * @param search Search term to filter by
   * @return Filtered list to display
   */
  private List<Tuple<Object, ItemStack>> filterRepresentitives(GuiInstance<SingleChoiceParam> inst, String search) {
    if (inst.getArg().customFilter() != null)
      return inst.getArg().customFilter().apply(search);

   return inst.getArg().representitives().stream()
    .filter(t -> {
      ItemMeta meta = t.b().getItemMeta();
      String name = meta == null ? t.b().getType().toString() : meta.getDisplayName();
      return name.toLowerCase().contains(search.trim().toLowerCase());
    })
    .toList();
  }
}
