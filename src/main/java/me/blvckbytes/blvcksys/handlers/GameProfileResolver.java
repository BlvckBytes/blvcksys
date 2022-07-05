package me.blvckbytes.blvcksys.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/05/2022

  Handles resolving game profiles by name through REST-based APIs.
*/
@AutoConstruct
public class GameProfileResolver implements IGameProfileResolver {

  private static final String PLAYERDB_URL = "https://playerdb.co/api/player/minecraft/%s";
  private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s";

  private final ILogger logger;
  private final JavaPlugin plugin;

  public GameProfileResolver(
    @AutoInject ILogger logger,
    @AutoInject JavaPlugin plugin
  ) {
    this.logger = logger;
    this.plugin = plugin;
  }

  @Override
  public void resolve(String name, Consumer<@Nullable GameProfile> profile) {
    // Fetch asynchronously, as APIs may be slow to respond and thus block
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      // Then re-sync before invoking the callback
      resolveSync(name, ret -> {
        Bukkit.getScheduler().runTask(plugin, () -> profile.accept(ret));
      });
    });
  }

  /**
   * Resolve a gameprofile synchronously, relative to the calling thread
   * @param name Target owner's name
   * @param profile Profile callback, null if not resolvable
   */
  private void resolveSync(String name, Consumer<@Nullable GameProfile> profile) {
    try {
      URL playerDbURL = new URL(PLAYERDB_URL.formatted(name));

      // Create a new GET request
      HttpURLConnection connection = (HttpURLConnection) playerDbURL.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      // Check that the status code actually represents success in order to avoid exceptions
      int code = connection.getResponseCode();
      if (code != 200) {
        profile.accept(null);
        return;
      }

      // Read the body contents into a string
      InputStreamReader sr = new InputStreamReader(connection.getInputStream());
      BufferedReader br = new BufferedReader(sr);
      String body = br.lines().collect(Collectors.joining());

      br.close();
      sr.close();

      // Get the player's UUID from player-db
      JsonObject playerDb = JsonParser.parseString(body).getAsJsonObject();
      JsonObject playerObj = playerDb.getAsJsonObject("data").getAsJsonObject("player");
      UUID id = UUID.fromString(playerObj.get("id").getAsString());
      String nameExact = playerObj.get("username").getAsString();

      // Get the player's profile from mojang
      URL mojangURL = new URL(SESSION_URL.formatted(id));
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

      // No textures provided in response JSON
      if (textures == null) {
        profile.accept(null);
        return;
      }

      GameProfile prof = new GameProfile(id, nameExact);
      prof.getProperties().put("textures", new Property("textures", textures));
      profile.accept(prof);
    } catch (Exception e) {
      logger.logError(e);
      profile.accept(null);
    }
  }
}
