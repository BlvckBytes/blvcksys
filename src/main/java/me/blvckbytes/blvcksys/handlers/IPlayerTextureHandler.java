package me.blvckbytes.blvcksys.handlers;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.persistence.models.PlayerTextureModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Public interfaces which the player texture handler provides to other consumers.
 */
public interface IPlayerTextureHandler {

  /**
   * Search existing textures by their owner's name
   * @param name Name to search for
   * @param limit Maximum number of entries to fetch
   * @return List of matching entries
   */
  List<PlayerTextureModel> searchByName(String name, int limit);

  /**
   * Store a custom texture binding
   * @param name Custom name
   * @param textures Textures to store
   * @return True on success, false if that name/uuid combination was already stored
   */
  boolean storeCustom(String name, String textures);

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
  GameProfile getProfileOrDefault(@Nullable String name);
}
