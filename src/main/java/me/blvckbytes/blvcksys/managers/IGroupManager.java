package me.blvckbytes.blvcksys.managers;

import org.bukkit.entity.Player;

import java.util.List;

public interface IGroupManager {

  /**
   * Get a list of all available groups
   * @return List of groups
   */
  List<Group> getGroups();

  /**
   * Get a group by it's name (ignores casing)
   * @param name Name of the group
   * @return Group if found, null otherwise
   */
  Group getGroup(String name);

  /**
   * Create and store a new group
   * @param group Group to create
   */
  void createGroup(Group group);

  /**
   * Add a player to a given group
   * @param p Player to add
   * @param group Group to add to
   * @return True on success, false on already existing membership
   */
  boolean addToGroup(Player p, Group group);

  /**
   * Remove a player from a given group, if there's a membership
   * @param p Player to remove
   * @param group Group to remove from
   * @return True on success, false on missing membership
   */
  boolean removeFromGroup(Player p, Group group);
}
