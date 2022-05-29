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
  // Vertically standing, rotating cone
  PURPLE_ROTATING_CONE(Particle.REDSTONE, new Particle.DustOptions(Color.PURPLE, .55F)),

  // Two helixes, 180° out of phase, travelling along the specified vector, fallback color orange
  DOUBLE_HELIX(Particle.REDSTONE, new Particle.DustOptions(Color.ORANGE, .55F))
  ;

  private final Particle particle;
  private final Particle.DustOptions options;
}
