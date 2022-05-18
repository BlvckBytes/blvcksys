package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Public interfaces which a live variable supplier has to implement.
*/
public interface ILiveVariableSupplier {

  /**
   * Resolve a live variable in a player's context
   * @param p Player to resolve for
   * @param variable Variable to resolve
   * @return String value to substitute
   */
  String resolveVariable(Player p, LiveVariable variable);
}
