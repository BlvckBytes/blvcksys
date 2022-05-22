package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.persistence.models.KitModel;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Public interfaces which the kit command provides to other consumers.
*/
public interface IKitCommand {

  /**
   * Register an interest in successfully performed kit requests
   * @param callback Callback containing the executor and their requested kit
   */
  void registerRequestInterest(BiConsumer<Player, KitModel> callback);
}
