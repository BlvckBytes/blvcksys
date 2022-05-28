package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.commands.IGiveCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.ICrateHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.models.CrateItemModel;
import me.blvckbytes.blvcksys.persistence.models.CrateModel;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
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

  private final ICrateHandler crateHandler;
  private final CrateContentGui crateContentGui;
  private final IGiveCommand give;

  public CrateDrawGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject ICrateHandler crateHandler,
    @AutoInject CrateContentGui crateContentGui,
    @AutoInject IGiveCommand give
  ) {
    super(5, "", i -> (
      cfg.get(ConfigKey.GUI_CRATE_DRAW_NAME).
        withVariable("name", i.getArg().getName())
    ), plugin, cfg, textures);

    this.crateHandler = crateHandler;
    this.crateContentGui = crateContentGui;
    this.give = give;
  }

  @Override
  protected void prepare() {
    addFill(Material.BLACK_STAINED_GLASS_PANE);
  }

  @Override
  protected void closed(GuiInstance<CrateModel> inst) {}

  @Override
  protected void opening(Player viewer, GuiInstance<CrateModel> inst) {
    CrateModel crate = inst.getArg();

    // Get the drawing layout from the crate or use a fallback value
    CrateDrawLayout layout = crate.getLayout();
    System.out.println(layout);
    if (layout == null)
      layout = CrateDrawLayout.HORIZONTAL_LINE;

    List<CrateItemModel> itemModels = crateHandler.getItems(crate.getName()).orElse(null);

    // Has no contents to draw from
    if (itemModels == null || itemModels.size() == 0) {
      viewer.closeInventory();
      viewer.sendMessage(
        cfg.get(ConfigKey.GUI_CRATE_DRAW_NO_ITEMS)
          .withPrefix()
          .withVariable("name", crate.getName())
          .asScalar()
      );
      return;
    }

    List<Integer> animSlots = inst.getTemplate().slotExprToSlots(layout.getItemSlots());
    int relOut = animSlots.indexOf(layout.getOutputSlot());

    Tuple<List<ItemStack>, CrateItemModel> loopData = createItemLoop(crate, itemModels, animSlots.size(), relOut);

    // The layout specified an invalid output slot or the item could not be drawn
    if (relOut < 0 || loopData == null) {
      viewer.closeInventory();
      viewer.sendMessage(
        cfg.get(ConfigKey.GUI_CRATE_DRAW_NO_ITEMS)
          .withPrefix()
          .withVariable("name", crate.getName())
          .asScalar()
      );
      return;
    }

    // Resize to only show the rows required by the layout
    inst.resize(layout.getRowsRequired(), false);

    inst.fixedItem(layout.getMarkerSlots(), i -> (
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
      inst.fixedItem(slot, i -> {
        int speedTicks = SPEED_TICKS[currSpeed.get()];
        int calls = totalCalls.incrementAndGet();
        int slotIndex = animSlots.indexOf(slot);
        int slotOffset = offset.get();

        // One full iteration means that all animated slots have been called once
        int iterNum = calls / animSlots.size();

        // Advance to the next speed when the current speed's number of iterations has been reached
        if (!done.get()) {
          if ((iterNum / speedTicks - 1) >= SPEED_ITERS[currSpeed.get()]) {
            // No next speed, done
            if (currSpeed.get() == SPEED_ITERS.length - 1) {
              done.set(true);

              // Hand out the prize
              give.giveItemsOrDrop(viewer, loopData.b().getItem());

              viewer.sendMessage(
                cfg.get(ConfigKey.GUI_CRATE_DRAW_PRIZE)
                  .withPrefix()
                  .withVariable("item", crateContentGui.getItemName(loopData.b()))
                  .asScalar()
              );

              // Close the GUI automatically after a short amount of time
              Bukkit.getScheduler().runTaskLater(plugin, inst::close, 30);
            } else {
              currSpeed.incrementAndGet();
              totalCalls.set(0);
            }
          }

          // Wait till as much time elapsed as the current speed dictates
          else if (iterNum % speedTicks == 0 && calls % animSlots.size() == 0)
            offset.incrementAndGet();
        }

        return loopData.a().get(wrapSlot(slotIndex - slotOffset, loopData.a().size()));
      }, null, 1);
    }
  }

  /**
   * Wrap the target slot of an item loop within the size's range
   * @param slot Target slot
   * @param size List size
   * @return Wrapped slot
   */
  private int wrapSlot(int slot, int size) {
    if (slot >= 0)
      return slot % size;

    // Add the size until slot is not a multiple of size anymore,
    // which then basically results in size - (-slot % size)
    int x = -1 * (slot / size) + ((-1 * slot) % size == 0 ? 0 : 1);
    return slot + x * size;
  }

  /**
   * Create the loop of items which is to be animated within the GUI by cloning the available
   * list of items until there are enough total items to take up all available slots. While creating
   * this list, the price is chosen internally and the final randomized loop is rotated in a way that
   * after rotating as many times as all added up speed iterations, the item will end up in the output slot.
   * @param crate Crate that contains these items
   * @param itemModels Items existing within this crate
   * @param slots Number of slots to animate
   * @return List of items as well as the chosen prize
   */
  private Tuple<List<ItemStack>, CrateItemModel> createItemLoop(CrateModel crate, List<CrateItemModel> itemModels, int slots, int outputOffs) {
    List<ItemStack> items = itemModels.stream()
      .map(model -> crateContentGui.appendDecoration(crate, model))
      .toList();

    CrateItemModel prize = crateHandler.drawItem(crate.getName()).orElse(null);
    if (prize == null)
      return null;

    // Get the prize's corresponding transformed item
    ItemStack prizeItem = items.get(itemModels.indexOf(prize));

    // Make sure there are enough items in the loop to wrap once
    List<ItemStack> itemLoop = new ArrayList<>();
    while (itemLoop.size() < slots)
      itemLoop.addAll(items);

    // Randomize the loop
    Collections.shuffle(itemLoop);

    // Get the index of the price item (first occurrence) within the randomized item loop
    int prizeIndex = itemLoop.indexOf(prizeItem);

    // Count the total number of movement animations
    int totalIters = 0;
    for (int iters : SPEED_ITERS)
      totalIters += iters;

    // Shift the target index into the output slot, then do totalIters rotations
    int slotOffs = outputOffs - prizeIndex;
    int rotBy = slotOffs - totalIters;

    // Apply number of rotations by offsetting each item
    ItemStack[] rotatedLoop = new ItemStack[itemLoop.size()];
    for (int i = 0; i < itemLoop.size(); i++)
      rotatedLoop[wrapSlot(i + rotBy, itemLoop.size())] = itemLoop.get(i);

    return new Tuple<>(Arrays.stream(rotatedLoop).toList(), prize);
  }
}
