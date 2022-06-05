package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.gui.ItemStackBuilder;
import me.blvckbytes.blvcksys.persistence.models.PlayerStatsModel;
import me.blvckbytes.blvcksys.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Supplies live variable values that are evaluated in the context of a player.
*/
@AutoConstruct
public class LiveVariableSupplier implements ILiveVariableSupplier, IAutoConstructed {

  // Time in ticks between polls of the top players
  private static final long TOP_POLL_T = 20 * 3;

  // Name template for the top kills armor stands
  private static final String TOP_KILLS_AS_NAME = "top_kills_%d";

  private final Map<LiveVariable, Function<Player, String>> suppliers;
  private final IPlayerStatsHandler playerStats;
  private final JavaPlugin plugin;
  private final IConfig cfg;
  private final IArmorStandHandler armorStandHandler;
  private final IPlayerTextureHandler textureHandler;

  private List<PlayerStatsModel> top5KillingPlayers;
  private BukkitTask pollingHandle;

  public LiveVariableSupplier(
    @AutoInject IPlayerStatsHandler playerStats,
    @AutoInject TimeUtil timeUtil,
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg,
    @AutoInject IArmorStandHandler armorStandHandler,
    @AutoInject IPlayerTextureHandler textureHandler
  ) {
    this.playerStats = playerStats;
    this.plugin = plugin;
    this.cfg = cfg;
    this.armorStandHandler = armorStandHandler;
    this.textureHandler = textureHandler;

    this.top5KillingPlayers = new ArrayList<>();
    this.suppliers = new HashMap<>();

    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    // Player specific
    this.suppliers.put(LiveVariable.PLAYER_NAME, Player::getName);
    this.suppliers.put(LiveVariable.PLAYER_EXPERIENCE, p -> String.valueOf(p.getLevel()));
    this.suppliers.put(LiveVariable.WORLD_NAME, p -> p.getWorld().getName());
    this.suppliers.put(LiveVariable.PLAYER_KILLS, p -> String.valueOf(playerStats.getStats(p).getKills()));
    this.suppliers.put(LiveVariable.PLAYER_DEATHS, p -> String.valueOf(playerStats.getStats(p).getDeaths()));
    this.suppliers.put(LiveVariable.PLAYER_KD, p -> String.valueOf(playerStats.calculateKD(p)));
    this.suppliers.put(LiveVariable.PLAYER_MONEY, p -> String.valueOf(playerStats.getStats(p).getMoney()));
    this.suppliers.put(LiveVariable.PLAYER_PLAYTIME, p -> timeUtil.formatDuration(playerStats.getStats(p).getPlaytimeSeconds(), true));

    // Date and time
    this.suppliers.put(LiveVariable.CURRENT_TIME, p -> timeFormat.format(new Date()));
    this.suppliers.put(LiveVariable.CURRENT_DATE, p -> dateFormat.format(new Date()));
    this.suppliers.put(LiveVariable.CURRENT_DAY, p -> switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
      case 1 -> "Sonntag";
      case 2 -> "Montag";
      case 3 -> "Dienstag";
      case 4 -> "Mittwoch";
      case 5 -> "Donnerstag";
      case 6 -> "Freitag";
      case 7 -> "Samstag";
      default -> "?";
    });

    // Globals
    this.suppliers.put(LiveVariable.TOP_KILLS_VALUE_1, p -> getTopKillerKills(0));
    this.suppliers.put(LiveVariable.TOP_KILLS_VALUE_2, p -> getTopKillerKills(1));
    this.suppliers.put(LiveVariable.TOP_KILLS_VALUE_3, p -> getTopKillerKills(2));
    this.suppliers.put(LiveVariable.TOP_KILLS_VALUE_4, p -> getTopKillerKills(3));
    this.suppliers.put(LiveVariable.TOP_KILLS_VALUE_5, p -> getTopKillerKills(4));
    this.suppliers.put(LiveVariable.TOP_KILLS_PLAYER_1, p -> getTopKillerName(0));
    this.suppliers.put(LiveVariable.TOP_KILLS_PLAYER_2, p -> getTopKillerName(1));
    this.suppliers.put(LiveVariable.TOP_KILLS_PLAYER_3, p -> getTopKillerName(2));
    this.suppliers.put(LiveVariable.TOP_KILLS_PLAYER_4, p -> getTopKillerName(3));
    this.suppliers.put(LiveVariable.TOP_KILLS_PLAYER_5, p -> getTopKillerName(4));
  }

  @Override
  public String resolveVariable(Player p, LiveVariable variable) {
    if (!this.suppliers.containsKey(variable))
      return variable.getPlaceholder();
    return this.suppliers.get(variable).apply(p);
  }

  @Override
  public void cleanup() {
    if (pollingHandle != null)
      pollingHandle.cancel();
  }

  @Override
  public void initialize() {
    pollingHandle = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      this.top5KillingPlayers = playerStats.getTop5Ranked(PlayerStatistic.KILLS);
      updateTopKillingArmorStands();
    }, 0L, TOP_POLL_T);
  }

  /**
   * Get the top n-th killer's name
   */
  private String getTopKillerName(int index) {
    if (top5KillingPlayers.size() <= index)
      return cfg.get(ConfigKey.TOP5_NAME_EMPTY).asScalar();
    return top5KillingPlayers.get(index).getOwner().getName();
  }

  /**
   * Get the top n-th killer's kills
   */
  private String getTopKillerKills(int index) {
    if (top5KillingPlayers.size() <= index)
      return "/";
    return String.valueOf(top5KillingPlayers.get(index).getKills());
  }

  /**
   * Update the top killing armor stands by updating the skull on
   * their head to the textures of that player
   */
  private void updateTopKillingArmorStands() {
    for (int i = 0; i < 5; i++) {
      String asName = TOP_KILLS_AS_NAME.formatted(i + 1);
      String killer = top5KillingPlayers.size() > i ? top5KillingPlayers.get(i).getOwner().getName() : null;

      armorStandHandler.getProperties(asName)
        .ifPresent(asp -> {

          // Set either skeleton skulls for vacant places or the player head of the killer
          asp.setHelmet(
            killer == null ?
              new ItemStack(Material.SKELETON_SKULL) :
              new ItemStackBuilder(textureHandler.getProfileOrDefault(killer))
                .build()
          );

          // Only update within cache, no need to persist this information
          armorStandHandler.setProperties(asName, asp, false);
        });
    }
  }
}
