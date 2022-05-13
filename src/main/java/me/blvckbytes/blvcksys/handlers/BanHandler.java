package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  Manages creating, updating and deleting bans as well as checking
  login attempts and making sure to cancel disallowed players.
*/

@AutoConstruct
public class BanHandler implements IBanHandler, Listener {

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onLogin(PlayerLoginEvent e) {

  }
}
