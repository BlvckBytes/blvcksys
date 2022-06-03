package me.blvckbytes.blvcksys.packets.communicators.armorstand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Represents all customizable properties of an armor stand created by packets.
*/
@Getter
@Setter
@AllArgsConstructor
public class ArmorStandProperties {
  // Whether the name is visible
  private boolean nameVisible;

  // Whether the armor stand is visible
  private boolean visible;

  // Whether the location should be shifted up to the name internally
  private boolean shifted;

  // Displayname of the armor stand
  private @Nullable String name;

  /**
   * Create a new hologram armor stand by it's displayed text
   * @param text Text to display
   */
  public ArmorStandProperties(String text) {
    this.name = text;
    this.nameVisible = true;
    this.shifted = true;
    this.visible = false;
  }
}
