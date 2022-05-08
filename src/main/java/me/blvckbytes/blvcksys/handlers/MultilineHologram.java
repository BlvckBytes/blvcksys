package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.blvcksys.packets.communicators.hologram.IHologramCommunicator;
import org.bukkit.Location;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Holds multiple hologram lines and manages the layouting, deciding
  of recipients (based on visibility) as well as keeping the hologram's
  variables in sync.
 */
@Getter
@Setter
@AllArgsConstructor
public class MultilineHologram {

  // Specify the spacing between two lines on the y-axis here
  private static final double INTER_LINE_SPACING = 0.25D;

  private String name;
  private Location loc;
  private List<String> lines;
  private IHologramCommunicator holoComm;

  /**
   * Called whenever there's a chance to update this hologram, which
   * doesn't mean that on every tick changes have to occur.
   */
  public void tick() {
    System.out.println("Ticking hologram " + name + " with " + lines.size() + " lines");
  }

  /**
   * Called whenever this hologram should be destroyed for
   * all online players.
   */
  public void destroy() {
    System.out.println("Destroying hologram " + name + " with " + lines.size() + " lines");
  }
}
