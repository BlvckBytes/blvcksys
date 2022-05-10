package me.blvckbytes.blvcksys.util;

import lombok.Getter;
import org.bukkit.profile.PlayerTextures;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/11/2022

  Provides convenient methods to work with a player's skin directly, without
  relying on any third-party web-APIs.
*/
@Getter
public class PlayerSkin {

  private final URL skin;
  private final PlayerTextures.SkinModel model;
  private final BufferedImage textures;

  /**
   * Create a new player skin utility instance from the player's
   * skin URL as well as the skin model type
   * @param skin URL to the skin's textures
   * @param model Type of skin
   * @throws IOException Error when fetching the image
   */
  public PlayerSkin(URL skin, PlayerTextures.SkinModel model) throws IOException {
    this.skin = skin;
    this.model = model;
    this.textures = ImageIO.read(skin);
  }

  /**
   * Get the specified textures in a flat, 2D image
   * @param section Section to get
   */
  public BufferedImage getSection(SkinSection section) {
    return section.cut(textures);
  }

  /**
   * Get a fully rendered, 3D image of the whole character
   */
  public BufferedImage getFullRender() {
    // Slim skins just omit one pixel from their arm-width
    int armWidth = SkinSection.ARMS_LEFT_FRONT.getWidth() - (model == PlayerTextures.SkinModel.SLIM ? 1 : 0);

    // Both arms and the body make up the image's width
    int width = SkinSection.BODY_FRONT.getWidth() + 2 * armWidth;

    // The head, the body and the feet make up the image's height
    int height = SkinSection.HEAD_FRONT.getHeight() +
                 SkinSection.BODY_FRONT.getHeight() +
                 SkinSection.LEGS_RIGHT_FRONT.getHeight();

    // Image's vertical center distance
    int vc = width / 2;

    // Set up a canvas to draw on
    BufferedImage result = new BufferedImage(width, height, textures.getType());
    Graphics2D g2d = result.createGraphics();

    List<Triple<SkinSection, Integer, Integer>> steps = List.of(

      ////////////////////////// First layer of textures //////////////////////////

      // Draw the head's front
      new Triple<>(
        SkinSection.HEAD_FRONT,
        vc - SkinSection.HEAD_FRONT.getWidth() / 2,
        0
      ),

      // Draw the body's front
      new Triple<>(
        SkinSection.BODY_FRONT,
        vc - SkinSection.BODY_FRONT.getWidth() / 2,
        SkinSection.HEAD_FRONT.getHeight()
      ),

      // Draw both arms's fronts
      new Triple<>(
        SkinSection.ARMS_LEFT_FRONT,
        vc - SkinSection.BODY_FRONT.getWidth() / 2 - armWidth,
        SkinSection.HEAD_FRONT.getHeight()
      ),
      new Triple<>(
        SkinSection.ARMS_RIGHT_FRONT,
        vc + SkinSection.BODY_FRONT.getWidth() / 2,
        SkinSection.HEAD_FRONT.getHeight()
      ),

      // Draw both legs's fronts
      new Triple<>(
        SkinSection.LEGS_LEFT_FRONT,
        vc - SkinSection.LEGS_LEFT_FRONT.getWidth(),
        SkinSection.HEAD_FRONT.getHeight() + SkinSection.BODY_FRONT.getHeight()
      ),
      new Triple<>(
        SkinSection.LEGS_RIGHT_FRONT,
        vc,
        SkinSection.HEAD_FRONT.getHeight() + SkinSection.BODY_FRONT.getHeight()
      ),

      ///////////////////////// Second layer of textures /////////////////////////

      // Draw the hat's front
      new Triple<>(
        SkinSection.HAT_FRONT,
        vc - SkinSection.HAT_FRONT.getWidth() / 2,
        0
      ),

      // Draw the jacket's front
      new Triple<>(
        SkinSection.JACKET_FRONT,
        vc - SkinSection.JACKET_FRONT.getWidth() / 2,
        SkinSection.HAT_FRONT.getHeight()
      ),

      // Draw both sleeves's fronts
      new Triple<>(
        SkinSection.SLEEVES_LEFT_FRONT,
        vc - SkinSection.JACKET_FRONT.getWidth() / 2 - armWidth,
        SkinSection.HAT_FRONT.getHeight()
      ),
      new Triple<>(
        SkinSection.SLEEVES_RIGHT_FRONT,
        vc + SkinSection.JACKET_FRONT.getWidth() / 2,
        SkinSection.HAT_FRONT.getHeight()
      ),

      // Draw both pants's fronts
      new Triple<>(
        SkinSection.PANTS_LEFT_FRONT,
        vc - SkinSection.PANTS_LEFT_FRONT.getWidth(),
        SkinSection.HAT_FRONT.getHeight() + SkinSection.JACKET_FRONT.getHeight()
      ),
      new Triple<>(
        SkinSection.PANTS_RIGHT_FRONT,
        vc,
        SkinSection.HAT_FRONT.getHeight() + SkinSection.JACKET_FRONT.getHeight()
      )
    );

    // Draw one step after the other in order, this allows for layering
    for (Triple<SkinSection, Integer, Integer> step : steps)
      g2d.drawImage(step.a().cut(textures), step.b(), step.c(), null);

    // TODO: Draw the matrix-transformed 3D-parts (perspective) of the textures

    g2d.dispose();
    return result;
  }
}
