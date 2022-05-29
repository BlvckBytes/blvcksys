package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.util.Triple;
import net.minecraft.util.Tuple;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  The parameter used to open a new single choice GUI.
*/
public record SingleChoiceParam(
  // Type to choose, displayed in the title
  String type,

  // List of choices, objects represented by itemstacks
  List<Tuple<Object, ItemStack>> representitives,

  // Selection callback, returns whether to close the GUI
  BiFunction<Object, Inventory, Boolean> selected,

  // Inventory close callback
  Runnable closed,

  // Back button
  @Nullable Consumer<Inventory> backButton
) {}
