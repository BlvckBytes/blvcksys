package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Specifies a bitmask value which corresponds to a flag (symbol)
  that can be displayed below the player's name as an indicator.
*/
@AllArgsConstructor
public enum BelowNameFlag {

  // Is currently teleporting
  TELEPORTING(ChatColor.DARK_PURPLE, '✈', 1 << 0)
  ;

  @Getter
  private final ChatColor color;

  @Getter
  private final char symbol;

  @Getter
  private final int value;
}
