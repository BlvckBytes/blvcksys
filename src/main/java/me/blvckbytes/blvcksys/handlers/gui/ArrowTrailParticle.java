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

  EXPLOSION(Particle.EXPLOSION_NORMAL, Material.TNT),
  SPARK(Particle.FIREWORKS_SPARK, Material.FIREWORK_ROCKET),
  REDSTONE(Particle.REDSTONE, Material.REDSTONE),
  SPLASH(Particle.WATER_SPLASH, Material.LILY_PAD),
  LAVA(Particle.LAVA, Material.LAVA_BUCKET),
  LAVA_DRIP(Particle.DRIP_LAVA, Material.LAVA_BUCKET),
  CRIT(Particle.CRIT, Material.WOODEN_SWORD),
  CRIT_MAGIC(Particle.CRIT_MAGIC, Material.NETHERITE_SWORD),
  WATER_DROP(Particle.WATER_DROP, Material.WATER_BUCKET),
  NOTE(Particle.NOTE, Material.NOTE_BLOCK),
  HEART(Particle.DAMAGE_INDICATOR, Material.RED_DYE),
  SMOKE(Particle.SMOKE_NORMAL, Material.CAMPFIRE),
  SPELL(Particle.SPELL, Material.POTION),
  SPELL_WITCH(Particle.SPELL_WITCH, Material.WITCH_SPAWN_EGG),
  SPELL_MOB(Particle.SPELL_MOB, Material.ZOMBIE_HEAD),
  ENCHANTMENT_TABLE(Particle.ENCHANTMENT_TABLE, Material.ENCHANTING_TABLE),
  FLAME(Particle.FLAME, Material.FLINT_AND_STEEL),
  SNOWBALL(Particle.SNOWBALL, Material.SNOWBALL),
  SLIME(Particle.SLIME, Material.SLIME_BALL)
  ;

  private final Particle particle;
  private final Material icon;
}
