package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents a custom effect applied to a potion item stack.
*/
@Getter
public class ItemStackCustomEffectSection extends AConfigSection {

  private @Nullable PotionEffectType effect;
  private @Nullable Integer duration;
  private Integer amplifier;
  private Boolean ambient;
  private Boolean particles;
  private Boolean icon;

  public ItemStackCustomEffectSection() {
    this.amplifier = 0;
    this.ambient = true;
    this.particles = true;
    this.icon = true;
  }

  /**
   * Convert the properties of this section to a PotionEffet object
   * @return A PotionEffect on success, empty if crucial data was missing
   */
  public Optional<PotionEffect> asEffect() {
    // Cannot create an effect object without the effect itself
    if (effect == null || duration == null)
      return Optional.empty();
    return Optional.of(new PotionEffect(effect, duration, amplifier, ambient, particles, icon));
  }
}
