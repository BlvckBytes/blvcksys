package me.blvckbytes.blvcksys.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/10/2022

  The skin is a 64 x 64 pixel PNG image, which consists of multiple sections,
  making up the individual parts of a player's skin. It's just like any other
  spritesheet. This enum maps the smallest usable regions to human readable
  shorthands, starting out from the top left corner, where each paragraph is
  a row of content going from top to bottom.

  Credits where credit's due: https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/skins/2872189-skin-guide-pre-post-1-8
*/
@Getter
@AllArgsConstructor
public enum SkinSection {

  // EMPTY(0, 0, 8, 8)
  HEAD_TOP(8, 0, 8, 8),
  HEAD_BOTTOM(16, 0, 8, 8),
  // EMPTY(24, 0, 8, 8)
  // EMPTY(32, 0, 8, 8)
  HAT_TOP(40, 0, 8, 8),
  HAT_BOTTOM(48, 0, 8, 8),
  // EMPTY(56, 0, 8, 8)

  HEAD_RIGHT(0, 8, 8, 8),
  HEAD_FRONT(8, 8, 8, 8),
  HEAD_LEFT(16, 8, 8, 8),
  HEAD_BACK(24, 8, 8, 8),
  HAT_RIGHT(32, 8, 8, 8),
  HAT_FRONT(40, 8, 8, 8),
  HAT_LEFT(48, 8, 8, 8),
  HAT_BACK(56, 8, 8, 8),

  // EMPTY(0, 16, 4, 4)
  LEGS_RIGHT_TOP(4, 16, 4, 4),
  LEGS_RIGHT_BOTTOM(8, 16, 4, 4),
  // EMPTY(12, 16, 4, 4)
  // EMPTY(16, 16, 4, 4)
  BODY_TOP(20, 16, 8, 4),
  BODY_BOTTOM(28, 16, 8, 4),
  // EMPTY(36, 16, 4, 4)
  // EMPTY(40, 16, 4, 4)
  ARMS_RIGHT_TOP(44, 16, 4, 4),
  ARMS_RIGHT_BOTTOM(48, 16, 4, 4),
  // EMPTY(52, 16, 4, 4)
  // EMPTY(56, 16, 4, 4)
  // EMPTY(60, 16, 4, 4)

  LEGS_RIGHT_OUTSIDE(0, 20, 4, 12),
  LEGS_RIGHT_FRONT(4, 20, 4, 12),
  LEGS_RIGHT_INSIDE(8, 20, 4, 12),
  LEGS_RIGHT_BACK(12, 20, 4, 12),
  BODY_RIGHT(16, 20, 4, 12),
  BODY_FRONT(20, 20, 8, 12),
  BODY_LEFT(28, 20, 4, 12),
  BODY_BACK(32, 20, 8, 12),
  ARMS_RIGHT_OUTSIDE(40, 20, 4, 12),
  ARMS_RIGHT_FRONT(44, 20, 4, 12),
  ARMS_RIGHT_INSIDE(48, 20, 4, 12),
  ARMS_RIGHT_BACK(52, 20, 4, 12),
  // EMPTY(56, 20, 4, 12)
  // EMPTY(60, 20, 4, 12)

  // EMPTY(0, 32, 4, 4)
  PANTS_RIGHT_TOP(4, 32, 4, 4),
  PANTS_RIGHT_BOTTOM(8, 32, 4, 4),
  // EMPTY(12, 32, 4, 4)
  // EMPTY(16, 32, 4, 4)
  JACKET_TOP(20, 32, 8, 4),
  JACKET_BOTTOM(28, 32, 8, 4),
  // EMPTY(36, 32, 4, 4)
  // EMPTY(40, 32, 4, 4)
  SLEEVES_RIGHT_TOP(44, 32, 4, 4),
  SLEEVES_RIGHT_BOTTOM(48, 16, 4, 4),
  // EMPTY(52, 32, 4, 4)
  // EMPTY(56, 32, 4, 4)
  // EMPTY(60, 32, 4, 4)

  PANTS_RIGHT_OUTSIDE(0, 36, 4, 12),
  PANTS_RIGHT_FRONT(4, 36, 4, 12),
  PANTS_RIGHT_INSIDE(8, 36, 4, 12),
  PANTS_RIGHT_BACK(12, 36, 4, 12),
  JACKET_RIGHT(16, 36, 4, 12),
  JACKET_FRONT(20, 36, 8, 12),
  JACKET_LEFT(28, 36, 4, 12),
  JACKET_BACK(32, 36, 8, 12),
  SLEEVES_RIGHT_OUTSIDE(40, 36, 4, 12),
  SLEEVES_RIGHT_FRONT(44, 36, 4, 12),
  SLEEVES_RIGHT_INSIDE(48, 36, 4, 12),
  SLEEVES_RIGHT_BACK(52, 36, 4, 12),
  // EMPTY(56, 36, 4, 12)
  // EMPTY(60, 36, 4, 12)

  // EMPTY(0, 48, 4, 4)
  PANTS_LEFT_TOP(4, 48, 4, 4),
  PANTS_LEFT_BOTTOM(8, 48, 4, 4),
  // EMPTY(12, 48, 4, 4)
  // EMPTY(16, 48, 4, 4)
  LEGS_LEFT_TOP(20, 48, 4, 4),
  LEGS_LEFT_BOTTOM(24, 48, 4, 4),
  // EMPTY(28, 48, 4, 4)
  // EMPTY(32, 48, 4, 4)
  ARMS_LEFT_TOP(36, 48, 4, 4),
  ARMS_LEFT_BOTTOM(40, 48, 4, 4),
  // EMPTY(44, 48, 4, 4)
  // EMPTY(48, 48, 4, 4)
  SLEEVES_LEFT_TOP(52, 48, 4, 4),
  SLEEVES_LEFT_BOTTOM(56, 48, 4, 4),
  // EMPTY(60, 48, 4, 4)

  PANTS_LEFT_OUTSIDE(0, 52, 4, 12),
  PANTS_LEFT_FRONT(4, 52, 4, 12),
  PANTS_LEFT_INSIDE(8, 52, 4, 12),
  PANTS_LEFT_BACK(12, 52, 4, 12),
  LEGS_LEFT_OUTSIDE(16, 52, 4, 12),
  LEGS_LEFT_FRONT(20, 52, 4, 12),
  LEGS_LEFT_INSIDE(24, 52, 4, 12),
  LEGS_LEFT_BACK(28, 52, 4, 12),
  ARMS_LEFT_OUTSIDE(32, 52, 4, 12),
  ARMS_LEFT_FRONT(36, 52, 4, 12),
  ARMS_LEFT_INSIDE(40, 52, 4, 12),
  ARMS_LEFT_BACK(44, 52, 4, 12),
  SLEEVES_LEFT_OUTSIDE(48, 52, 4, 12),
  SLEEVES_LEFT_FRONT(52, 52, 4, 12),
  SLEEVES_LEFT_INSIDE(56, 52, 4, 12),
  SLEEVES_LEFT_BACK(60, 52, 4, 12),
  ;

  private final int x, y, width, height;

  /**
   * Cut out this section of a skin from a skin's "sprite sheet"
   * @param input Full skin image
   * @return Cut out section
   * @throws RasterFormatException When the image was too small
   */
  public BufferedImage cut(BufferedImage input) throws RasterFormatException {
    return input.getSubimage(x, y, width, height);
  }
}
