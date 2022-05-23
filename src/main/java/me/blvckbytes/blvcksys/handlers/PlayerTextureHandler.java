package me.blvckbytes.blvcksys.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.PlayerTextureModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Stores a player's UUID and skin textures, resolved by their name.
*/
@AutoConstruct
public class PlayerTextureHandler implements IPlayerTextureHandler {

  private final IPersistence pers;
  private final ILogger logger;

  // Mapping names to textures
  private final Map<String, PlayerTextureModel> cache;

  public PlayerTextureHandler(
    @AutoInject IPersistence pers,
    @AutoInject ILogger logger
  ) {
    this.pers = pers;
    this.logger = logger;

    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<PlayerTextureModel> getTextures(String name, boolean forceUpdate) {
    // Try to resolve from cache
    if (cache.containsKey(name.toLowerCase()))
      return Optional.of(cache.get(name.toLowerCase()));

    // Try to resolve from db
    if (!forceUpdate) {
      Optional<PlayerTextureModel> res = pers.findFirst(buildQuery(name));

      if (res.isPresent()) {
        cache.put(name.toLowerCase(), res.get());
        return res;
      }
    }

    // Cannot resolve this name
    Tuple<UUID, String> result = resolveSkinTextures(name).orElse(null);
    if (result == null)
      return Optional.empty();

    // Delete any exact results that may exist
    pers.delete(new QueryBuilder<>(
      PlayerTextureModel.class,
      "name", EqualityOperation.EQ_IC, name
    ).or("uuid", EqualityOperation.EQ, result.a()));

    // Store result
    PlayerTextureModel model = new PlayerTextureModel(name, result.a(), result.b());
    pers.store(model);

    cache.put(name.toLowerCase(), model);
    return Optional.of(model);
  }

  @Override
  public GameProfile getProfileOrDefault(@Nullable String name) {
    if (name == null)
      return new GameProfile(UUID.randomUUID(), "");

    Optional<PlayerTextureModel> textures = getTextures(name, false);

    if (textures.isPresent())
      return textures.get().toProfile();

    return new GameProfile(UUID.randomUUID(), name);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Find a player's skin textures entry, always take the most recent result,
   * if there are multiple
   * @param name Name of the target player
   */
  private QueryBuilder<PlayerTextureModel> buildQuery(String name) {
    return new QueryBuilder<>(
      PlayerTextureModel.class,
      "name", EqualityOperation.EQ_IC, name
    )
      .orderBy("createdAt", false)
      .orderBy("updatedAt", false);
  }

  /**
   * Resolves the skin textures as well as the UUID from a player's name
   * @param name Name of the target player
   * @return A tuple of the target's UUID and their skin texture property value
   */
  private Optional<Tuple<UUID, String>> resolveSkinTextures(String name) {
    try {
      URL playerDbURL = new URL("https://playerdb.co/api/player/minecraft/" + name);

      // Create a new GET request
      HttpURLConnection connection = (HttpURLConnection) playerDbURL.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      // Check that the status code actually represents success in order to avoid exceptions
      int code = connection.getResponseCode();
      if (code != 200)
        return Optional.empty();

      // Read the body contents into a string
      InputStreamReader sr = new InputStreamReader(connection.getInputStream());
      BufferedReader br = new BufferedReader(sr);
      String body = br.lines().collect(Collectors.joining());

      br.close();
      sr.close();

      // Get the player's UUID from player-db
      JsonObject playerDb = JsonParser.parseString(body).getAsJsonObject();
      UUID id = UUID.fromString(
        playerDb.getAsJsonObject("data")
          .getAsJsonObject("player")
          .get("id")
          .getAsString()
      );

      // Get the player's profile from mojang
      URL mojangURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
      JsonObject mojang = JsonParser.parseString(IOUtils.toString(mojangURL, StandardCharsets.UTF_8)).getAsJsonObject();

      // Find the textures property among all properties
      String textures = null;
      JsonArray props = mojang.getAsJsonArray("properties");
      for (JsonElement el : props) {
        if (!el.isJsonObject())
          continue;

        JsonObject prop = el.getAsJsonObject();
        if (!prop.get("name").getAsString().equals("textures"))
          continue;

        textures = prop.get("value").getAsString();
      }

      return Optional.of(new Tuple<>(id, textures));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }
}
