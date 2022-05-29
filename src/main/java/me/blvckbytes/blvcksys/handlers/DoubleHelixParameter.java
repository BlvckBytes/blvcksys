package me.blvckbytes.blvcksys.handlers;

import org.bukkit.Color;
import org.bukkit.util.Vector;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Encapsulates the double helix effect's parameters
 */
public record DoubleHelixParameter(
  // Velocity (direction and speed) to animate in
  Vector velocity,

  // How many blocks are required to rotate a full turn
  double blocksPerWinding,

  // 2*radius will be the space between the two 180° out of phase helix's
  double radius,

  // Color of the helix
  Color color
) {}
