package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Particle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Identifies implemented animations that can be played on a target player.
*/
@Getter
@AllArgsConstructor
public enum AnimationType {
  // Rotating cone around the target player
  PURPLE_ROTATING_CONE(Particle.REDSTONE, new Particle.DustOptions(Color.PURPLE, 0.55F))
  ;

  private final Particle particle;
  private final Particle.DustOptions options;
}
