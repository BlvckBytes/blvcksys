package me.blvckbytes.blvcksys.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class PlayerPermissionsChangedEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  @Getter
  private final Player player;

  @Getter
  private final List<String> active, added, removed;

  /**
   * This event is fired whenever a player's permissions have been changed
   * @param player Target player
   * @param active All currently active permissions
   * @param added Permissions that have been added
   * @param removed Permissions that have been removed
   */
  public PlayerPermissionsChangedEvent(
    Player player,
    List<String> active,
    List<String> added,
    List<String> removed
  ) {
    this.player = player;
    this.active = active;
    this.added = added;
    this.removed = removed;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
