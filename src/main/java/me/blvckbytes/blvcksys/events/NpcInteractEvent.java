package me.blvckbytes.blvcksys.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.persistence.models.NpcModel;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Represents an interaction between a player and an NPC (fake player).
*/
@Getter
@AllArgsConstructor
public class NpcInteractEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final NpcInteraction type;
  private final boolean sneaking;
  private final String npcName;

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
