package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.bukkit.Color;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  Represents the properties of a single potion effect.
*/
@Getter
public class QuestPotionParameterEffectSection extends AConfigSection {

  // Type of the potion effect
  private @Nullable PotionEffectType effectType;

  // Type of potion
  private @Nullable PotionType potionType;

  // Custom color of the potion
  private @Nullable Color color;

  // Amplifier number, starting from 1
  private @Nullable Integer amplifier;

  // Duration of the effect, measured in ticks
  private @Nullable Integer duration;

}
