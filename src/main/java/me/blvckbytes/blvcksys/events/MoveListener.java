package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Listen to move events (x, y, z - ignoring yaw/pitch).
*/
@AutoConstruct
public class MoveListener implements Listener, IMoveListener {

  private final Map<Player, List<Runnable>> subs;

  public MoveListener() {
    this.subs = new HashMap<>();
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onMove(PlayerMoveEvent e) {
    // Didn't move
    if (e.getTo() == null)
      return;

    // Only looked around, didn't move
    // Don't use looking around as it's really heavy on performance
    if (calculateTotalAbsDelta(e.getFrom(), e.getTo()) == 0)
      return;

    List<Runnable> subscribers = subs.get(e.getPlayer());

    if (subscribers == null)
      return;

    // Execute all subscribers
    for (int i = subscribers.size() - 1; i >= 0; i--)
      subscribers.get(i).run();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Automatically unsubscribe all listeners on quitting
    subs.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public Runnable subscribe(Player target, Runnable callback) {
    // Create empty list initially
    if (!this.subs.containsKey(target))
      this.subs.put(target, new ArrayList<>());

    // Register callback
    this.subs.get(target).add(callback);
    return callback;
  }

  @Override
  public void unsubscribe(Player target, Runnable callback) {
    List<Runnable> subscribers = this.subs.get(target);

    // Unregister callback
    if (subscribers != null)
      subscribers.remove(callback);
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Calculate the total (dx + dy + dz) absolute (|d|) coordinate delta of a and b
   * @param a Location a
   * @param b Location b
   * @return Absolute delta added from all three axis
   */
  private double calculateTotalAbsDelta(Location a, Location b) {
    return (
      Math.abs(a.getX() - b.getX()) +
        Math.abs(a.getY() - b.getY()) +
        Math.abs(a.getZ() - b.getZ())
    );
  }
}
