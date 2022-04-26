package me.blvckbytes.blvcksys.config;

import lombok.Getter;
import org.bukkit.entity.Player;

public enum PlayerPermission {

  //=========================================================================//
  //                               FLY Command                               //
  //=========================================================================//

  COMMAND_FLY("bvs.fly"),
  COMMAND_FLY_OTHERS("bvs.fly.others"),

  //=========================================================================//
  //                               GIVE Command                              //
  //=========================================================================//

  COMMAND_GIVE("bvs.give"),
  COMMAND_GIVE_OTHERS("bvs.give.others"),

  //=========================================================================//
  //                              INJECT Command                             //
  //=========================================================================//

  COMMAND_INJECT("bvs.inject"),

  //=========================================================================//
  //                                Chat Colors                              //
  //=========================================================================//

  // Format: <prefix>.<color_name>
  CHAT_COLOR_PREFIX("bvs.chatcolor.")
  ;

  @Getter
  private final String value;

  PlayerPermission(String value) {
    this.value = value;
  }

  /**
   * Checks whether or not this player has the permission
   * @param p Player to check
   */
  public boolean has(Player p) {
    return p.hasPermission(value);
  }

  /**
   * Checks whether or not this player has the permission and supports
   * adding a suffix to it (concrete value of a template permission)
   * @param p Player to check
   * @param suffix Suffix to add to the permission's string
   */
  public boolean has(Player p, String suffix) {
    return p.hasPermission(joinPermissions(value, suffix));
  }

  /**
   * Safely join two permissions to always end up with a proper notation
   * @param a Permission A
   * @param b Permission B
   * @return Permission A joined with permission B
   */
  private String joinPermissions(String a, String b) {
    // Colliding dots, remove trailing dot from a
    if (a.endsWith(".") && b.startsWith("."))
      return a.substring(0, a.length() - 1) + b;

    // Missing dot, add trailing dot to a
    if (!a.endsWith(".") && !b.startsWith("."))
      return a + "." + b;

    // Just join the strings and the dot will match
    return a + b;
  }
}
