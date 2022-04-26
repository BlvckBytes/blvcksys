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

  COMMAND_INJECT("bvs.inject")
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
}
