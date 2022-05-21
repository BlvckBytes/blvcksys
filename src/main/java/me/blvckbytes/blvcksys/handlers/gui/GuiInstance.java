package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  A personalized, live instance of a GUI template.
*/
@Getter
public class GuiInstance {

  private final Player viewer;
  private final Inventory inv;
  private final Function<Player, ConfigValue> title;
  private final Map<Integer, GuiItem> items;

  public GuiInstance(
    Player viewer,
    Inventory inv,
    Function<Player, ConfigValue> title,
    Map<Integer, GuiItem> items
  ) {
    this.viewer = viewer;
    this.title = title;
    this.inv = inv;
    this.items = new HashMap<>(items);
  }

  public void additem(
    BiFunction<Player, Integer, ItemStackBuilder> item,
    @Nullable BiConsumer<Player, Integer> onClick,
    @Nullable Integer updatePeriod
  ) {
    for (int i = 0; i < inv.getSize(); i++) {
      int slotNumber = i;

      if (items.containsKey(slotNumber))
        continue;

      items.put(slotNumber, new GuiItem((p) -> item.apply(p, slotNumber), onClick, updatePeriod));
      break;
    }
  }

  public void tick(long time) {
    for (Map.Entry<Integer, GuiItem> itemE : items.entrySet()) {
      GuiItem item = itemE.getValue();

      if (item.updatePeriod() == null)
        continue;

      if (time % item.updatePeriod() != 0)
        continue;

      inv.setItem(itemE.getKey(), item.item().apply(viewer).build());
    }
  }
}
