package me.blvckbytes.blvcksys.persistence.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Color;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Represents all available colors a particle effect can be played in.
*/
@Getter
@AllArgsConstructor
public enum ParticleEffectColor {

  WHITE(Color.WHITE),
  SILVER(Color.SILVER),
  GRAY(Color.GRAY),
  BLACK(Color.BLACK),
  RED(Color.RED),
  MAROON(Color.MAROON),
  YELLOW(Color.YELLOW),
  OLIVE(Color.OLIVE),
  LIME(Color.LIME),
  GREEN(Color.GREEN),
  AQUA(Color.AQUA),
  TEAL(Color.TEAL),
  BLUE(Color.BLUE),
  NAVY(Color.NAVY),
  FUCHSIA(Color.FUCHSIA),
  PURPLE(Color.PURPLE),
  ORANGE(Color.ORANGE)
  ;

  private final Color color;
}
