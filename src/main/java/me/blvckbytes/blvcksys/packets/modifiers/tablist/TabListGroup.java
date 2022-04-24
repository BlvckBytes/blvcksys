package me.blvckbytes.blvcksys.packets.modifiers.tablist;

import org.bukkit.ChatColor;

/**
 * Represents a group within the tab list
 * @param groupName Name of the team, has to be unique
 * @param prefix Prefix prepended to the name
 * @param suffix Suffix appended to the name
 * @param nameColor Color of the name itself
 * @param priority Priority, 0 means highest
 */
public record TabListGroup(
  String groupName,
  String prefix,
  String suffix,
  ChatColor nameColor,
  int priority
) {}
