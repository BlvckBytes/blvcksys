package me.blvckbytes.blvcksys.handlers.gui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record GuiItem (
  Function<Player, ItemStackBuilder> item,
  BiConsumer<Player, Integer> onClick,
  @Nullable Integer updatePeriod
) {}
