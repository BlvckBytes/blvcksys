package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Start and stop implemented animations for specific players.
*/
@AutoConstruct
public class AnimationHandler implements IAnimationHandler, Listener, IAutoConstructed {

  /*
   * Represents an active animation that's playing for a specific player
   *
   * type Animation that's playing
   * time Time variable, increases by one each tick and starts out as zero
   */
  @AllArgsConstructor
  private static class ActiveAnimation {
    AnimationType type;
    long time;
  }

  // Delay in ticks between internal animation tick routine calls
  private static final long TICK_DELAY = 5;

  // Maps players to their currently active animations
  private final Map<Player, List<ActiveAnimation>> animations;

  private final JavaPlugin plugin;
  private int tickHandle;

  public AnimationHandler(
    @AutoInject JavaPlugin plugin
  ) {
    this.plugin = plugin;
    this.tickHandle = -1;

    this.animations = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void startAnimation(Player target, AnimationType animation) {
    // Create an empty list initially
    if (!this.animations.containsKey(target))
      this.animations.put(target, new ArrayList<>());

    // Add this new animation and start the time out at zero
    this.animations.get(target).add(new ActiveAnimation(animation, 0));
  }

  @Override
  public boolean stopAnimation(Player target, AnimationType animation) {
    // No animations for this player
    if (!this.animations.containsKey(target))
      return false;

    // Remove all animations of this type
    this.animations.get(target).removeIf(anim -> anim.type.equals(animation));
    return true;
  }

  @Override
  public boolean stopAllAnimations(Player target) {
    List<ActiveAnimation> animations = this.animations.remove(target);
    return animations != null && animations.size() > 0;
  }

  @Override
  public void cleanup() {
    // Cancel the internal tick loop task
    if (this.tickHandle > 0)
      Bukkit.getScheduler().cancelTask(this.tickHandle);
  }

  @Override
  public void initialize() {
    // Start the internal ticking task
    this.tickHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 0L, TICK_DELAY);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  public void onQuit(PlayerQuitEvent e) {
    // Stop all animations for this player
    animations.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Tick all animations for all players
   */
  private void tick() {
    for (Map.Entry<Player, List<ActiveAnimation>> playerAnimations : animations.entrySet())
      for (ActiveAnimation animation : playerAnimations.getValue())
        tickAnimation(playerAnimations.getKey(), animation);
  }

  /**
   * Tick an animation for a specific player
   * @param p Target player
   * @param animation Animation that's playing
   */
  private void tickAnimation(Player p, ActiveAnimation animation) {
    p.sendMessage("Playing animation " + animation.type);
    // Increase the time tracking variable
    animation.time++;
  }
}
