package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Manages creating, update, listing and deleting homes of individual
  players by their home-name.
 */
@AutoConstruct
public class HomeHandler implements IHomeHandler, IAutoConstructed, Listener {

  // Time in ticks between home laser visualization ticks
  private static final long LASER_TICKER_PERIOD_T = 5L;

  // Max distance between the player and their home, combined of both the x and z axies
  private static final double LASER_MAX_XZ_RAD = 100;

  // Mapping players to their list of homes, where each home may have
  // a hologram instance (used with the laser). If that's null, the player
  // isn't near enough to the home
  private final Map<OfflinePlayer, Map<HomeModel, @Nullable Tuple<MultilineHologram, FakeArmorStand>>> cache;

  private final IPersistence pers;
  private final JavaPlugin plugin;
  private final IHologramHandler hologramHandler;
  private final IArmorStandHandler armorStandHandler;
  private final IPreferencesHandler preferencesHandler;
  private final IConfig cfg;

  private BukkitTask laserTicker;
  private long tickerTime;

  public HomeHandler(
    @AutoInject IPersistence pers,
    @AutoInject JavaPlugin plugin,
    @AutoInject IHologramHandler hologramHandler,
    @AutoInject IPreferencesHandler preferencesHandler,
    @AutoInject IArmorStandHandler armorStandHandler,
    @AutoInject IConfig cfg
  ) {
    this.pers = pers;
    this.plugin = plugin;
    this.hologramHandler = hologramHandler;
    this.preferencesHandler = preferencesHandler;
    this.armorStandHandler = armorStandHandler;
    this.cfg = cfg;

    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<HomeModel> createHome(OfflinePlayer creator, String name, Location loc) throws PersistenceException {
    try {
      HomeModel home = HomeModel.createDefault(creator, name, loc);
      pers.store(home);
      cacheHome(creator, home);
      return Optional.of(home);
    } catch (DuplicatePropertyException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean updateLocation(OfflinePlayer creator, String name, Location loc) throws PersistenceException {
    HomeModel home = findHome(creator, name).orElse(null);

    if (home == null)
      return false;

    home.setLoc(loc);
    pers.store(home);

    // Update the laser's text to the new location
    Tuple<MultilineHologram, FakeArmorStand> holo = cache.get(creator).get(home);
    if (holo != null)
      holo.a().updateLines(buildLaserLines(home));

    return true;
  }

  @Override
  public boolean updateIcon(OfflinePlayer creator, String name, Material icon) throws PersistenceException {
    HomeModel home = findHome(creator, name).orElse(null);

    if (home == null)
      return false;

    home.setIcon(icon);
    pers.store(home);

    // Update the laser's icon
    if (cache.containsKey(creator)) {
      Tuple<MultilineHologram, FakeArmorStand> holo = cache.get(creator).get(home);
      if (holo != null)
        updateIcon(home, holo);
    }

    return true;
  }

  @Override
  public boolean updateColor(OfflinePlayer creator, String name, ChatColor color) throws PersistenceException {
    HomeModel home = findHome(creator, name).orElse(null);

    if (home == null)
      return false;

    home.setColor(color);
    pers.store(home);

    // Update the laser's text to the new color
    Tuple<MultilineHologram, FakeArmorStand> holo = cache.get(creator).get(home);
    if (holo != null)
      holo.a().updateLines(buildLaserLines(home));

    return true;
  }

  @Override
  public boolean deleteHome(OfflinePlayer creator, String name) throws PersistenceException {
    if (pers.delete(buildQueryFor(creator, name)) > 0) {
      if (cache.containsKey(creator)) {
        for (Iterator<HomeModel> homeI = cache.get(creator).keySet().iterator(); homeI.hasNext();) {
          HomeModel home = homeI.next();

          if (!home.getName().equalsIgnoreCase(name))
            continue;

          destroyLaserHologram(cache.get(creator).get(home));
          homeI.remove();
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int countHomes(OfflinePlayer creator) throws PersistenceException {
    return pers.count(buildQueryFor(creator, null));
  }

  @Override
  public List<HomeModel> listHomes(OfflinePlayer creator) throws PersistenceException {
    // No homes cached at all or some homes are still missing, fetch from DB
    if (!cache.containsKey(creator) || cache.get(creator).size() != countHomes(creator)) {
      List<HomeModel> homes = pers.find(buildQueryFor(creator, null));

      cache.remove(creator);
      homes.forEach(h -> cacheHome(creator, h));

      return homes;
    }

    // Return from cache
    return List.copyOf(cache.get(creator).keySet());
  }

  @Override
  public Optional<HomeModel> findHome(OfflinePlayer creator, String name) throws PersistenceException {
    return cache.getOrDefault(creator, new HashMap<>())
      .keySet().stream()
      .filter(h -> h.getName().equalsIgnoreCase(name))
      .findFirst()
      .or(() -> (
        pers.findFirst(buildQueryFor(creator, name)))
        .map(h -> {
          cacheHome(creator, h);
          return h;
        })
      );
  }

  @Override
  public void cleanup() {
    if (this.laserTicker != null)
      this.laserTicker.cancel();
  }

  @Override
  public void initialize() {
    this.laserTicker = Bukkit.getScheduler().runTaskTimer(plugin, this::tickLasers, 0L, LASER_TICKER_PERIOD_T);

    // Load all homes of this player on a reload
    for (Player t : Bukkit.getOnlinePlayers())
      listHomes(t);
  }

  //=========================================================================//
  //                                Listener                                 //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    // Load all homes of this player on join, to guarantee laser visibility
    listHomes(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Update the icon a home laser hologram displays
   * @param home Home to supply the icon
   * @param holo Hologram to update
   */
  private void updateIcon(HomeModel home, Tuple<MultilineHologram, FakeArmorStand> holo) {
    ArmorStandProperties props = holo.b().getProps();
    Material icon = home.getIcon();

    props.setHelmet(new ItemStack(icon, 1));
    Location loc = holo.a().getNextLocation();

    // Non-blocks need a different treatment, as armor stands
    // wear items completely different than blocks
    if (!icon.isBlock()) {
      props.setSmall(false);
      props.setHeadPose(new EulerAngle(Math.PI / 2, 0, 0));

      // Move in the opposite of the x/z-viewing direction to center
      // the tilted down head on the main laser axis again
      Vector shift = loc.getDirection().normalize().multiply(-.8);
      // Shift up to account for the tilted head
      shift.setY(.3);
      loc.add(shift);
    }

    // Undo modifications otherwise
    else {
      props.setSmall(true);
      props.setHeadPose(null);
    }

    holo.b().setProps(props);
    holo.b().setLoc(loc);
  }

  /**
   * Saves a home into cache for a specific player
   * @param creator Target player
   * @param home Home to cache
   */
  private void cacheHome(OfflinePlayer creator, HomeModel home) {
    // Don't cache for offline players
    if (!creator.isOnline())
      return;

    if (!cache.containsKey(creator))
      cache.put(creator, new HashMap<>());
    cache.get(creator).put(home, null);
  }

  /**
   * Build a personalized query to receive only results in regards to this creator
   * @param creator creator to receive results for
   * @param name Name of a specific home, null to receive all existing homes
   */
  private QueryBuilder<HomeModel> buildQueryFor(OfflinePlayer creator, @Nullable String name) {
    QueryBuilder<HomeModel> query = new QueryBuilder<>(
      HomeModel.class,
      "creator__uuid", EqualityOperation.EQ, creator.getUniqueId()
    );

    if (name != null)
      query.and("name", EqualityOperation.EQ_IC, name);

    return query;
  }

  /**
   * Builds the lines the laser hologram will display
   * @param home Home which supplies the data
   * @return Lines to display
   */
  private List<String> buildLaserLines(HomeModel home) {
    return cfg.get(ConfigKey.HOMES_LASER_LINES)
      .withVariable("name", home.getName())
      .withVariable("color", home.getColor())
      .withVariable("x", home.getLoc().getBlockX())
      .withVariable("y", home.getLoc().getBlockY())
      .withVariable("z", home.getLoc().getBlockZ())
      .asList();
  }

  /**
   * Destroys a laser's hologram
   * @param holo Hologram to destroy
   */
  private void destroyLaserHologram(@Nullable Tuple<MultilineHologram, FakeArmorStand> holo) {
    if (holo == null)
      return;

    hologramHandler.destroyTemporary(holo.a());
    armorStandHandler.destroyTemporary(holo.b());
  }

  /**
   * Ticks all homes to visualize them as lasers to the owner,
   * if the owner is close by.
   */
  private void tickLasers() {

    // Only re-calculate every second
    if (tickerTime % 20 == 0) {

      // Iterate all players within the cache
      for (Iterator<OfflinePlayer> playerI = cache.keySet().iterator(); playerI.hasNext();) {
        OfflinePlayer op = playerI.next();
        Player online = op.getPlayer();
        Map<HomeModel, @Nullable Tuple<MultilineHologram, FakeArmorStand>> homes = cache.get(op);

        // Remove offline players from the cache and destroy all existing holograms
        if (online == null) {
          homes.values().forEach(this::destroyLaserHologram);

          playerI.remove();
          continue;
        }

        // Update all k-v pairs for this player
        homes.entrySet().forEach(entry -> {
          HomeModel home = entry.getKey();

          // Doesn't want lasers rendered
          if (!preferencesHandler.showHomeLasers(online)) {
            // Remove existing lasers, if applicable
            if (entry.getValue() != null) {
              destroyLaserHologram(entry.getValue());
              entry.setValue(null);
            }

            return;
          }

          double xzDist = (
            Math.abs(home.getLoc().getX() - online.getLocation().getX()) +
            Math.abs(home.getLoc().getZ() - online.getLocation().getZ())
          );

          // The laser is enabled if the distance is within the radius
          if (xzDist <= LASER_MAX_XZ_RAD) {
            if (entry.getValue() == null) {
              Location loc = home.getLoc().clone();
              loc.setY(online.getEyeLocation().getY());

              // Create the hologram
              MultilineHologram mHolo = hologramHandler.createTemporary(
                loc, List.of(online), buildLaserLines(home)
              );

              ArmorStandProperties props = new ArmorStandProperties();
              props.setSmall(true);
              props.setShifted(true);

              // Create the icon armor stand with it's basic properties
              FakeArmorStand as = armorStandHandler.createTemporary(
                mHolo.getNextLocation(), List.of(online), props
              );

              // Update the icon and store within cache
              Tuple<MultilineHologram, FakeArmorStand> holo = new Tuple<>(mHolo, as);
              updateIcon(home, holo);
              entry.setValue(holo);
            }
          }

          // Destroy existing holograms which are out of reach
          else if (entry.getValue() != null) {
            destroyLaserHologram(entry.getValue());
            entry.setValue(null);
          }
        });
      }
    }

    // Loop all players and their homes in the cache
    cache.forEach((op, value) -> {
      // Not online anymore
      Player online = op.getPlayer();
      if (online == null)
        return;

      value.forEach((home, holo) -> {
        // Out of reach or disabled
        if (holo == null)
          return;

        // Holos should always be on the head height of the player
        double dY = Math.abs(holo.a().getLoc().getY() - online.getEyeLocation().getY());
        Location loc = home.getLoc().clone();

        // Only move the hologram vertically if there's actually a noticable difference
        if (1.5 - dY <= 0.05) {
          loc.setY(online.getEyeLocation().getY());
          holo.a().setLoc(loc);
          updateIcon(home, holo);
        }

        // Cannot draw the laser without a world
        World w = loc.getWorld();
        if (w == null)
          return;

        // Draw a full vertical laser
        for (double y = w.getMinHeight(); y <= w.getMaxHeight(); y += 0.1) {
          online.spawnParticle(
            Particle.REDSTONE,
            new Location(w, loc.getX(), y, loc.getZ()), 1,
            new Particle.DustOptions(colorFromChatColor(home.getColor()), .8F)
          );
        }
      });
    });

    tickerTime += LASER_TICKER_PERIOD_T;
  }

  /**
   * Translates chat colors to bukkit colors
   * @param color Chat color to translate
   * @return Translated bukkit color
   */
  private Color colorFromChatColor(ChatColor color) {
    return switch (color) {
      case BLACK -> Color.fromRGB(0, 0, 0);
      case DARK_BLUE -> Color.fromRGB(0, 0, 182);
      case DARK_GREEN -> Color.fromRGB(85, 187, 54);
      case DARK_AQUA -> Color.fromRGB(85, 187, 188);
      case RED -> Color.fromRGB(234, 80, 73);
      case DARK_RED -> Color.fromRGB(174, 35, 23);
      case DARK_PURPLE -> Color.fromRGB(174, 35, 184);
      case GOLD -> Color.fromRGB(251, 167, 0);
      case GRAY -> Color.fromRGB(190, 190, 190);
      case DARK_GRAY -> Color.fromRGB(64, 64, 64);
      case GREEN -> Color.fromRGB(128, 250, 96);
      case BLUE -> Color.fromRGB(64, 64, 244);
      case AQUA -> Color.fromRGB(128, 250, 252);
      case LIGHT_PURPLE -> Color.fromRGB(234, 80, 246);
      case YELLOW -> Color.fromRGB(255, 254, 102);
      default -> Color.fromRGB(255, 255, 255);
    };
  }
}
