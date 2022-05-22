package me.blvckbytes.blvcksys.handlers.gui;

import org.bukkit.event.inventory.ClickType;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Used as a callback parameter when a GUI item has been clicked.
*/
public record GuiClickEvent<T> (
  GuiInstance<T> gui,
  Integer slot,
  ClickType type
) {}
