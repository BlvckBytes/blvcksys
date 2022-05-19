package me.blvckbytes.blvcksys.packets.communicators.hud;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/19/2022

  Communicates displaying messages on the HUD of a player's client.
*/
public interface IHudCommunicator {

  /**
   * Sends a title message consisting of two lines below each other right
   * in the center of the game window, in rather big fonts.
   * @param p Receiving player
   * @param line1 First line to display
   * @param line2 Second line to display
   * @param fadeIn Time in ticks it takes to fade the message in
   * @param stay Time in ticks the message should be displayed
   * @param fadeOut Time in ticks it takes to fade out the message
   */
  void sendTitle(
    Player p,
    String line1, String line2,
    int fadeIn, int stay, int fadeOut
  );

  /**
   * Sends a action bar message consisting of a single line of text
   * right over the item hotbar.
   * @param p Receiving player
   * @param text Line to display
   */
  void sendActionBar(Player p, String text);
}
