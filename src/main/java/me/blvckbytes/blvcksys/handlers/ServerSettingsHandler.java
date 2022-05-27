package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.ServerSettingsModel;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Handles initially creating and altering the server settings.
*/
@AutoConstruct
public class ServerSettingsHandler implements IServerSettingsHandler, IAutoConstructed, Listener {

  private final JavaPlugin plugin;
  private final IPersistence pers;
  private ServerSettingsModel cache;

  public ServerSettingsHandler(
    @AutoInject IPersistence pers,
    @AutoInject JavaPlugin plugin
  ) {
    this.pers = pers;
    this.plugin = plugin;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public int getAttackSpeed() {
    return getOrCreate().getAttackSpeed();
  }

  @Override
  public void setAttackSpeed(int value) {
    ServerSettingsModel settings = getOrCreate();
    settings.setAttackSpeed(value);
    pers.store(settings);

    // Alter the value for all currently online players
    for (Player t : Bukkit.getOnlinePlayers())
      alterAttackSpeed(t);
  }

  @Override
  public void initialize() {
    load();
  }

  @Override
  public void cleanup() {}

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    alterAttackSpeed(e.getPlayer());
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> alterAttackSpeed(e.getPlayer()), 5);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Alter a player's attack speed to the configured value
   * @param p Target player
   */
  private void alterAttackSpeed(Player p) {
    AttributeInstance ai = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
    if (ai != null)
      ai.setBaseValue(getAttackSpeed());
  }

  /**
   * Gets either the cached settings or loads them from persistence
   */
  private ServerSettingsModel getOrCreate() {
    return cache == null ? load() : cache;
  }

  /**
   * Loads the settings from persistence
   */
  private ServerSettingsModel load() {
    List<ServerSettingsModel> l = pers.list(ServerSettingsModel.class);

    // Not yet created, create
    if (l.size() == 0) {
      cache = ServerSettingsModel.createDefault();
      pers.store(cache);
      return cache;
    }

    // Use first result and store into cache
    cache = l.get(0);
    return cache;
  }
}
