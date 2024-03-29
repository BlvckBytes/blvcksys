package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.PlayerIgnoreModel;
import org.bukkit.OfflinePlayer;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/19/2022

  Public interfaces which the ignore handler provides to other consumers.
 */
public interface IIgnoreHandler {

  /**
   * Get whether the executor ignores the target's chat messages
   * @param executor Executing player
   * @param target Target to ignore
   */
  boolean getChatIgnore(OfflinePlayer executor, OfflinePlayer target) throws PersistenceException;

  /**
   * Get whether the executor ignores the target's private messages
   * @param executor Executing player
   * @param target Target to ignore
   */
  boolean getMsgIgnore(OfflinePlayer executor, OfflinePlayer target) throws PersistenceException;

  /**
   * Set whether the executor ignores the target's chat messages
   * @param executor Executing player
   * @param target Target to ignore
   * @param value Value to set
   */
  void setChatIgnore(OfflinePlayer executor, OfflinePlayer target, boolean value) throws PersistenceException;

  /**
   * Set whether the executor ignores the target's private messages
   * @param executor Executing player
   * @param target Target to ignore
   * @param value Value to set
   */
  void setMsgIgnore(OfflinePlayer executor, OfflinePlayer target, boolean value) throws PersistenceException;

  /**
   * Get all ignores of a player which are currently active
   * @param executor Executing player
   * @return List of ignores which have at least one property set to true
   */
  List<PlayerIgnoreModel> listActiveIgnores(OfflinePlayer executor);
}
