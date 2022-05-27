package me.blvckbytes.blvcksys.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Public interfaces which the animation handler provides to other consumers.
 */
public interface IAnimationHandler {

  /**
   * Start playing a specific animation on a player's location
   * @param target Target player
   * @param animation Target animation
   */
  void startAnimation(Player target, List<Player> receicers, AnimationType animation, @Nullable Object parameter);

  /**
   * Start playing a specific animation on a fixed location
   * @param loc Location to play at
   * @param animation Target animation
   */
  void startAnimation(Location loc, List<Player> receicers,AnimationType animation, @Nullable Object parameter);

  /**
   * Stop a specific animation that runs on a player's location
   * @param target Target player
   * @param animation Target animation
   * @return Whether this animation existed and was deleted
   */
  boolean stopAnimation(Player target, AnimationType animation);

  /**
   * Stop a specific animation that runs on a fixed location
   * @param loc Location playing at
   * @param animation Target animation
   * @return Whether this animation existed and was deleted
   */
  boolean stopAnimation(Location loc, AnimationType animation);

  /**
   * Stop all animations that run on a player
   * @param target Target player
   * @return Whether any animations existed and were deleted
   */
  boolean stopAllAnimations(Player target);
}
