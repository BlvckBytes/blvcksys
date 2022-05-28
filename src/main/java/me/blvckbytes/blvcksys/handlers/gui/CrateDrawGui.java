package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.CrateHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Plays the drawing animation for a crate to a the viewer.
*/
@AutoConstruct
public class CrateDrawGui extends AGui<CrateModel> {

  // The ticks indicate how much time in ticks needs to elapse before the animation
  // is advanced by one frame, where as the iters hold a count of total advancements
  // which need to pass, until the next speed is being used
  private static final int[] SPEED_TICKS = { 1,   2,  3, 4, 5, 10 },
                             SPEED_ITERS = { 30, 30, 20, 5, 4,  3 };

  private final CrateHandler crateHandler;
  private final CrateContentGui crateContentGui;

  public CrateDrawGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject CrateHandler crateHandler,
    @AutoInject CrateContentGui crateContentGui
  ) {
    super(5, "", i -> (
      cfg.get(ConfigKey.GUI_CRATE_DRAW_NAME).
        withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.crateHandler = crateHandler;
    this.crateContentGui = crateContentGui;
  }

  @Override
  protected void prepare() {
    addFill(Material.BLACK_STAINED_GLASS_PANE);
  }

  @Override
  protected void closed(GuiInstance<CrateModel> inst) {}

  @Override
  protected void opening(Player viewer, GuiInstance<CrateModel> inst) {
    // Get the drawing layout from the crate or use a fallback value
    CrateDrawLayout layout = inst.getArg().getLayout();
    if (layout == null)
      layout = CrateDrawLayout.HORIZONTAL_LINE;

    List<Integer> animSlots = inst.getTemplate().slotExprToSlots(layout.getItemSlots());
    List<ItemStack> itemLoop = createItemLoop(inst.getArg(), animSlots.size()).orElse(null);

    // Has no contents to draw from
    if (itemLoop == null) {
      viewer.closeInventory();
      viewer.sendMessage(
        cfg.get(ConfigKey.GUI_CRATE_DRAW_NO_ITEMS)
          .withPrefix()
          .withVariable("name", inst.getArg().getName())
          .asScalar()
      );
      return;
    }

    // Resize to only show the rows required by the layout
    inst.resize(layout.getRowsRequired(), false);

    fixedItem(layout.getMarkerSlots(), i -> (
      new ItemStackBuilder(Material.PURPLE_STAINED_GLASS_PANE)
        .withName(cfg.get(ConfigKey.GUI_CRATE_DRAW_INDICATOR_NAME))
        .withLore(cfg.get(ConfigKey.GUI_CRATE_DRAW_INDICATOR_LORE))
        .build()
    ), null);

    // Slot animation state, where totalCalls is relative to each speed, marked by currSpeed and the
    // offset indicates the offset within the item loop for all slots
    AtomicInteger totalCalls = new AtomicInteger(0), currSpeed = new AtomicInteger(0), offset = new AtomicInteger(0);
    AtomicBoolean done = new AtomicBoolean(false);

    // Set a fixed item into each slot of the animation which updates on every tick
    for (int slot : animSlots) {
      fixedItem(slot, i -> {
        int speedTicks = SPEED_TICKS[currSpeed.get()];
        int calls = totalCalls.incrementAndGet();
        int slotIndex = animSlots.indexOf(slot);
        int slotOffset = offset.get();

        // One full iteration means that all animated slots have been called once
        int iterNum = calls / animSlots.size();

        // Advance to the next speed when the current speed's number of iterations has been reached
        if (!done.get() && iterNum / speedTicks >= SPEED_ITERS[currSpeed.get()]) {
          // No next speed, done
          if (currSpeed.incrementAndGet() == SPEED_ITERS.length) {
            viewer.closeInventory();
            done.set(true);
          }

          // Reset call counter for the next speed
          else
            totalCalls.set(0);
        }

        // Wait till as much time elapsed as the current speed dictates
        else if (iterNum % speedTicks == 0 && calls % animSlots.size() == 0)
          offset.incrementAndGet();

        return itemLoop.get((slotIndex + slotOffset) % itemLoop.size());
      }, null, 1);
    }
  }

  /**
   * Create the loop of items which is to be animated within the GUI by cloning the available
   * list of items until there are enough total items to take up all available slots.
   * @param crate Crate to fetch the items from
   * @param slots Number of slots to animate
   * @return Optional list of items, empty if there weren't any items within this crate
   */
  private Optional<List<ItemStack>> createItemLoop(CrateModel crate, int slots) {
    List<CrateItemModel> itemModels = crateHandler.getItems(crate.getName()).orElse(null);

    if (itemModels == null || itemModels.size() == 0)
      return Optional.empty();

    List<ItemStack> items = itemModels.stream()
      .map(model -> crateContentGui.appendDecoration(crate, model))
      .toList();

    // Make sure there are enough items in the loop to wrap once
    List<ItemStack> itemLoop = new ArrayList<>();
    while (itemLoop.size() < slots)
      itemLoop.addAll(items);
    Collections.shuffle(itemLoop);

    return Optional.of(itemLoop);
  }
}
