package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Particle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  All available arrow trail particles with their icon.
*/
@Getter
@AllArgsConstructor
public enum ArrowTrailParticle {

  REDSTONE(Particle.REDSTONE, Material.REDSTONE),
  LAVA(Particle.LAVA, Material.LAVA_BUCKET),
  LAVA_DRIP(Particle.DRIP_LAVA, Material.LAVA_BUCKET),
  CRIT(Particle.CRIT, Material.WOODEN_SWORD),
  WATER_DRIP(Particle.DRIP_WATER, Material.WATER_BUCKET),
  WATER_BUBBLE(Particle.WATER_BUBBLE, Material.WATER_BUCKET),
  NOTE(Particle.NOTE, Material.NOTE_BLOCK),
  HEART(Particle.HEART, Material.RED_DYE)
  ;

  private final Particle particle;
  private final Material icon;
}
