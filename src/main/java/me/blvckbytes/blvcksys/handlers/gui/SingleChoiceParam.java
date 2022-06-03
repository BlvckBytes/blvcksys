package me.blvckbytes.blvcksys.handlers.gui;

import net.minecraft.util.Tuple;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

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

  @Nullable Function<String, List<Tuple<Object, ItemStack>>> customFilter,

  // Selection callback, provides the bound object and the GUI ref
  BiConsumer<Object, GuiInstance<SingleChoiceParam>> selected,

  // Inventory close callback, providing a ref to the closed GUI
  @Nullable Consumer<GuiInstance<SingleChoiceParam>> closed,

  // Back button, providing a ref to the GUI about to navigate away from
  @Nullable Consumer<GuiInstance<SingleChoiceParam>> backButton
) {}
