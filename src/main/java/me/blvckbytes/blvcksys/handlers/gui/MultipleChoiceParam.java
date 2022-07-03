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
  Created On: 06/30/2022

  The parameter used to open a new multiple choice GUI.
*/
public record MultipleChoiceParam(
  // Type to choose, displayed in the title
  String type,

  // List of choices, objects represented by itemstacks
  List<Tuple<Object, ItemStack>> representitives,

  // Provider for standard parameters used in GUIs
  IStdGuiParamProvider paramProvider,

  // Used to transform selected items
  @Nullable Function<ItemStack, ItemStack> selectionTransform,

  // Custom external filtering function
  @Nullable Function<String, List<Tuple<Object, ItemStack>>> customFilter,

  // Selection callback, provides the bound object and the GUI ref
  BiConsumer<List<Object>, GuiInstance<MultipleChoiceParam>> selected,

  // Inventory close callback, providing a ref to the closed GUI
  @Nullable Consumer<GuiInstance<MultipleChoiceParam>> closed,

  // Back button, providing a ref to the GUI about to navigate away from
  @Nullable Consumer<GuiInstance<MultipleChoiceParam>> backButton
) {}
