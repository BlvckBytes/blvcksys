package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.handlers.TriResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  The parameter used to open a new yes/no GUI.
*/
public record YesNoParam(
  // Type of yes/no to be displayed in the title
  String type,
  // Three states: SUCC=yes, ERR=no, EMPTY=inv closed
  BiConsumer<TriResult, GuiInstance<?>> choice,
  // Lore to be displayed on the yes button
  ConfigValue yesLore,
  // Lore to be displayed on the no button
  ConfigValue noLore,
  // Back button, providing a ref to the GUI about to navigate away from
  @Nullable Consumer<GuiInstance<YesNoParam>> backButton
) {}
