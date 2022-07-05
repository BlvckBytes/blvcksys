package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents a custom effect applied to a potion item stack.
*/
@Getter
public class ItemStackCustomEffectSection extends AConfigSection {

  private @Nullable ConfigValue effect;
  private @Nullable ConfigValue duration;
  private @Nullable ConfigValue amplifier;
  private @Nullable Boolean ambient;
  private @Nullable Boolean particles;
  private @Nullable Boolean icon;

  /**
   * Convert the properties of this section to a PotionEffet object
   * @return A PotionEffect on success, empty if crucial data was missing
   * @param variables Variables to apply while evaluating values
   */
  public Optional<PotionEffect> asEffect(@Nullable Map<String, String> variables) {
    // Cannot create an effect object without the effect itself
    PotionEffectType type = this.effect == null ? null : this.effect.withVariables(variables).asScalar(PotionEffectType.class);

    if (type == null || duration == null)
      return Optional.empty();

    Integer amplifier = this.amplifier == null ? 0 : this.amplifier.withVariables(variables).asScalar(Integer.class);
    Integer duration = this.duration.withVariables(variables).asScalar(Integer.class);

    return Optional.of(new PotionEffect(
      type, duration == null ? 20 * 60 : duration,
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

    PotionEffectType type = this.effect == null ? null : this.effect.asScalar(PotionEffectType.class);
    if (type != null && effect.getType() != type)
      return false;

    Integer duration = this.duration == null ? 0 : this.duration.asScalar(Integer.class);
    if (duration != null && effect.getDuration() != duration)
      return false;

    Integer amplifier = this.amplifier == null ? 0 : this.amplifier.asScalar(Integer.class);
    if (amplifier != null && effect.getAmplifier() != amplifier)
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
