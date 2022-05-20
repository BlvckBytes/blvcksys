package me.blvckbytes.blvcksys.handlers;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Holds a location and the npc's skin in order to display that
  fake NPC to nearby players. Keeps track of whether the client
  knows this npc and sends destroy signals on destroy to only
  those clients.
 */
@Getter
public class FakeNpc {

  // Specify the max. squared distance between the npc and any
  // given recipient that receives updates here
  private static final double RECIPIENT_MAX_DIST_SQ = Math.pow(30, 2);

  private final List<Player> actives;

  private Location loc;
  private String skin;

  public FakeNpc(Location loc, String skin) {
    this.loc = loc;
    this.skin = skin;
    this.actives = new ArrayList<>();
  }

  /**
   * Called whenever the npc is moved
   */
  public void setLoc(Location loc) {
    this.loc = loc;
  }

  /**
   * Called whenever the npc changes it's skin
   */
  public void setSkin(String skin) {
    this.skin = skin;
  }

  /**
   * Called periodically to update the npc
   */
  public void tick() {

  }

  /**
   * Called when the end of this npc's lifespan has been reached
   * and all fake instances need to be undone
   */
  public void destroy() {

  }
}
