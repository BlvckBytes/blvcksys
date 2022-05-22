package me.blvckbytes.blvcksys.handlers.gui;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Represents an item which resides in a managed GUI.
*/
public record GuiItem<T> (
  // Item supplier function
  Function<GuiInstance<T>, ItemStack> item,

  // Click event consumer
  Consumer<GuiClickEvent<T>> onClick,

  // How often this item should be updated, in ticks, null means never
  @Nullable Integer updatePeriod
) {}
