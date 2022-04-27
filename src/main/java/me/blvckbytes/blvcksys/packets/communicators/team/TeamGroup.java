package me.blvckbytes.blvcksys.packets.communicators.team;

import org.bukkit.ChatColor;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Represents a group within the tab list
*/
public record TeamGroup(
  String groupName,
  String prefix,
  String suffix,
  ChatColor nameColor,
  int priority
) {}
