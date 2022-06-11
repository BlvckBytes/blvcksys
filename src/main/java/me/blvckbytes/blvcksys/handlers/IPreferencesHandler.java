package me.blvckbytes.blvcksys.handlers;

import net.minecraft.util.Tuple;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Public interfaces which the preferences handler provides to other consumers.
*/
public interface IPreferencesHandler {

  /**
   * Get the scoreboard hidden preference
   * @param p Target player
   */
  boolean isScoreboardHidden(Player p);

  /**
   * Set the scoreboard hidden preference
   * @param p Target player
   * @param hidden Whether the scoreboard should be hidden
   */
  void setScoreboardHidden(Player p, boolean hidden);

  /**
   * Get the show home lasers preference
   * @param p Target player
   */
  boolean showHomeLasers(Player p);

  /**
   * Set the show home lasers preference
   * @param p Target player
   * @param shown Whether the home lasers should be shown
   */
  void setShowHomeLasers(Player p, boolean shown);

  /**
   * Get the chat hidden preference
   * @param p Target player
   */
  boolean isChatHidden(Player p);

  /**
   * Set the chat hidden preference
   * @param p Target player
   * @param hidden Whether the scoreboard should be hidden
   */
  void setChatHidden(Player p, boolean hidden);

  /**
   * Get the msg disabled preference
   * @param p Target player
   */
  boolean isMsgDisabled(Player p);

  /**
   * Set the msg disabled preference
   * @param p Target player
   * @param disabled Whether private messages should be disabled
   */
  void setMsgDisabled(Player p, boolean disabled);

  /**
   * Set the arrow trail particle effect
   * @param p Target player
   * @param particle Particle to be played
   * @param color Particle color if applies, null otherwise
   */
  void setArrowTrail(Player p, @Nullable Particle particle, @Nullable Color color);

  /**
   * Get the currently applied arrow trail effect
   * @param p Target player
   */
  Tuple<@Nullable Particle, @Nullable Color> getArrowTrail(Player p);
}
