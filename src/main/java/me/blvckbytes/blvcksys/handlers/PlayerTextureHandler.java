package me.blvckbytes.blvcksys.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.models.PlayerTextureModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.Triple;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Stores a player's UUID and skin textures, resolved by their name.
*/
@AutoConstruct
public class PlayerTextureHandler implements IPlayerTextureHandler, IAutoConstructed {

  // Path of the local head database file to get most of the pre-defined heads
  // from, relative to the resources folder
  private static final String DATABASE_FILE = "head_database.csv";

  private final IPersistence pers;
  private final ILogger logger;
  private final JavaPlugin plugin;

  // Mapping names to textures
  private final Map<String, List<PlayerTextureModel>> cache;

  public PlayerTextureHandler(
    @AutoInject IPersistence pers,
    @AutoInject ILogger logger,
    @AutoInject JavaPlugin plugin
  ) {
    this.pers = pers;
    this.logger = logger;
    this.plugin = plugin;

    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public List<PlayerTextureModel> searchByName(String name, int limit) {
    return pers.find(
      new QueryBuilder<>(
        PlayerTextureModel.class,
        "name", EqualityOperation.CONT_IC, name.trim()
      )
        .limit(limit)
    );
  }

  @Override
  public boolean storeCustom(String name, String textures) {
    try {
      pers.store(new PlayerTextureModel(name, UUID.randomUUID(), true, textures));
      return true;
    } catch (DuplicatePropertyException e) {
      return false;
    }
  }

  @Override
  public Optional<PlayerTextureModel> getTextures(String name, boolean forceUpdate) {
    return cache.getOrDefault(name.toLowerCase(), new ArrayList<>())
      .stream()
      .filter(e -> e.getName().equalsIgnoreCase(name))
      .min((a, b) -> Boolean.compare(a.isRandomUuid(), b.isRandomUuid()))
      .or(() -> {
        // Try to resolve from db
        if (!forceUpdate) {
          Optional<PlayerTextureModel> res = pers.findFirst(buildQuery(name));

          if (res.isPresent()) {
            if (!cache.containsKey(name.toLowerCase()))
              cache.put(name.toLowerCase(), new ArrayList<>());
            cache.get(name.toLowerCase()).add(res.get());
            return res;
          }
        }

        // Cannot resolve this name
        Triple<UUID, String, String> result = resolveSkinTextures(name).orElse(null);
        if (result == null)
          return Optional.empty();

        // Delete any exact results that may exist
        pers.delete(new QueryBuilder<>(
          PlayerTextureModel.class,
          "name", EqualityOperation.EQ_IC, name
        ).or("uuid", EqualityOperation.EQ, result.a()));

        // Store result
        PlayerTextureModel model = new PlayerTextureModel(result.c(), result.a(), false, result.b());
        pers.store(model);

        if (!cache.containsKey(name.toLowerCase()))
          cache.put(name.toLowerCase(), new ArrayList<>());
        cache.get(name.toLowerCase()).add(model);
        return Optional.of(model);
      });
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

  @Override
  public void cleanup() {}

  @Override
  public void initialize() {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      Map<String, String> heads = loadDatabaseFile();

      // Check if there are already more heads loaded, then skip this madness
      int numExisting = pers.count(PlayerTextureModel.class);
      if (numExisting >= heads.size())
        return;

      // Load head by head into DB
      for (Map.Entry<String, String> entry : heads.entrySet()) {
        boolean succ = storeCustom(entry.getKey(), entry.getValue());

        if (succ)
          logger.logInfo("Loaded head \"" + entry.getKey() + "\" from file into database.");
        else
          logger.logDebug("Head \"" + entry.getKey() + "\" from file already loaded.");
      }
    });
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Loads the local head database file into memory while using a hashmap
   * to overwrite duplicate keys, as I'm not that interested in most head
   * variations anyways.
   */
  private Map<String, String> loadDatabaseFile() {
    Map<String, String> entries = new HashMap<>();

    try {
      InputStream is = getClass().getClassLoader().getResourceAsStream(DATABASE_FILE);

      if (is == null)
        return entries;

      InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
      BufferedReader br = new BufferedReader(isr);

      while (br.ready()) {
        // Layout: category;incrementing id;representitive name;textures url part;? (always zero);more category information
        String[] data = br.readLine().split(";");

        // Malformed data
        if (data.length != 6)
          continue;

        String name = data[2];
        StringBuilder nameBuf = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
          char c = name.charAt(i);

          // Skip unwanted symbols
          if (c == '.' || c == '"' || c == ' ' || c == ')')
            continue;

          // Transform chars after opening brackets uppercase
          if (c == '(') {
            // Last char reached, stop
            if (i == name.length() - 1)
              break;

            // Advance to the next char
            c = name.charAt(++i);

            // Transform to upper-case
            if (c >= 'a' && c <= 'z')
              c -= 32;
          }

          nameBuf.append(c);
        }

        String texture = data[3];

        // Format and encode json value
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/%s\"}}}".formatted(texture);
        String encodedJson = Base64.getEncoder().encodeToString(json.getBytes());

        entries.put(nameBuf.toString(), encodedJson);
      }

      br.close();
      isr.close();
    } catch (Exception e) {
      logger.logError(e);
    }

    return entries;
  }

  /**
   * Find a player's skin textures entry, always take the most recent result,
   * if there are multiple, preferring non-random UUID entries
   * @param name Name of the target player
   */
  private QueryBuilder<PlayerTextureModel> buildQuery(String name) {
    return new QueryBuilder<>(
      PlayerTextureModel.class,
      "name", EqualityOperation.EQ_IC, name
    )
      .orderBy("randomUuid", true)
      .orderBy("createdAt", false)
      .orderBy("updatedAt", false);
  }

  /**
   * Resolves the skin textures as well as the UUID from a player's name
   * @param name Name of the target player
   * @return A triple of the target's UUID, their skin texture property value and the exact name
   */
  private Optional<Triple<UUID, String, String>> resolveSkinTextures(String name) {
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
      JsonObject playerObj = playerDb.getAsJsonObject("data").getAsJsonObject("player");
      UUID id = UUID.fromString(playerObj.get("id").getAsString());
      String nameExact = playerObj.get("username").getAsString();

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

      return Optional.of(new Triple<>(id, textures, nameExact));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }
}
