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
  private @Nullable Integer amplifier;
  private @Nullable Boolean ambient;
  private @Nullable Boolean particles;
  private @Nullable Boolean icon;

  /**
   * Convert the properties of this section to a PotionEffet object
   * @return A PotionEffect on success, empty if crucial data was missing
   */
  public Optional<PotionEffect> asEffect() {
    // Cannot create an effect object without the effect itself
    if (effect == null || duration == null)
      return Optional.empty();

    return Optional.of(new PotionEffect(
      effect, duration,
      // Default to no amplifier
      amplifier == null ? 0 : amplifier,
      // Default boolean flags to false
      ambient != null && ambient,
      particles != null && particles,
      icon != null && icon
    ));
  }

  /**
   * Compares all available values of this section against the
   * provided effect and checks if they match
   * @param effect Target effect
   */
  public boolean describesEffect(@Nullable PotionEffect effect) {
    if (effect == null)
      return false;

    if (this.effect != null && effect.getType() != this.effect)
      return false;

    if (this.duration != null && effect.getDuration() != this.duration)
      return false;

    if (this.amplifier != null && effect.getAmplifier() != this.amplifier)
      return false;

    if (this.ambient != null && effect.isAmbient() != this.ambient)
      return false;

    if (this.particles != null && effect.hasParticles() != this.particles)
      return false;

    if (this.icon != null && effect.hasIcon() != this.icon)
      return false;

    // All checks passed
    return true;
  }
}
