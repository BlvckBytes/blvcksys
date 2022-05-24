package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.blvckbytes.blvcksys.events.InventoryManipulationEvent;
import org.bukkit.event.inventory.ClickType;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Used as a callback parameter when a GUI item has been clicked.
*/
@Getter
@RequiredArgsConstructor
public class GuiClickEvent<T> {
  private final GuiInstance<T> gui;
  private final InventoryManipulationEvent manipulation;

  @Setter
  private boolean permitUse = false;
}
