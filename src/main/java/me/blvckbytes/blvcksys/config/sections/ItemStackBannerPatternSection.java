package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;
import me.blvckbytes.blvcksys.config.ConfigValue;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/28/2022

  Represents a banner pattern set on a banner item stack.
*/
@Getter
public class ItemStackBannerPatternSection extends AConfigSection {

  private @Nullable ConfigValue pattern;
  private @Nullable ConfigValue color;

  /**
   * Convert the properties of this section to a Pattern object
   * @param variables Variables to apply while evaluating values
   */
  public @Nullable Pattern asPattern(@Nullable Map<String, String> variables) {
    PatternType pattern = this.pattern == null ? null : this.pattern.withVariables(variables).asScalar(PatternType.class);
    DyeColor color = this.color == null ? null : this.color.withVariables(variables).asScalar(DyeColor.class);

    // Cannot construct a pattern with missing data
    if (pattern == null || color == null)
      return null;

    return new Pattern(color, pattern);
  }
}
