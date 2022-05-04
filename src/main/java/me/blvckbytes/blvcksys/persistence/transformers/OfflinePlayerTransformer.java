package me.blvckbytes.blvcksys.persistence.transformers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Handles transforming bukkit offline-players.
*/
public class OfflinePlayerTransformer implements IDataTransformer<UUID, OfflinePlayer> {

  @Override
  public OfflinePlayer revive(UUID data) {
    return Bukkit.getOfflinePlayer(data);
  }

  @Override
  public UUID replace(OfflinePlayer data) {
    return data.getUniqueId();
  }
}
