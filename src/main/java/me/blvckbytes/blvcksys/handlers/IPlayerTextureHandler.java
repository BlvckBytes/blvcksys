package me.blvckbytes.blvcksys.handlers;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.persistence.models.PlayerTextureModel;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Public interfaces which the player texture handler provides to other consumers.
 */
public interface IPlayerTextureHandler {

  /**
   * Get the texture information of a given player by their name
   * @param name Name of the player
   * @param forceUpdate Whether to force a cache-update
   * @return Optional textures, empty if that name couldn't be resolved
   */
  Optional<PlayerTextureModel> getTextures(String name, boolean forceUpdate);

  /**
   * Utility method to get a game profile directly, or fall back to a default
   * profile, which consists of a random UUID and the requested name
   * @param name Name of the player
   * @return Fetched GameProfile if the name exists, default profile otherwise
   */
  GameProfile getProfileOrDefault(String name);
}
