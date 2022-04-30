package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Public interfaces which the animation handler provides to other consumers.
 */
public interface IAnimationHandler {

  /**
   * Start playing a specific animation on a player
   * @param target Target player
   * @param animation Target animation
   */
  void startAnimation(Player target, AnimationType animation);

  /**
   * Stop a specific animation that runs on a player
   * @param target Target player
   * @param animation Target animation
   * @return Whether this animation existed and was deleted
   */
  boolean stopAnimation(Player target, AnimationType animation);

  /**
   * Stop all animations that run on a player
   * @param target Target player
   * @return Whether any animations existed and were deleted
   */
  boolean stopAllAnimations(Player target);
}
