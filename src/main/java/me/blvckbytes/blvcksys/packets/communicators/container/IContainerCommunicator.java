package me.blvckbytes.blvcksys.packets.communicators.container;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Communicates opening a virtual, fully functional container to a player.
*/
public interface IContainerCommunicator {

  boolean openContainer(Player p, ContainerType type, String title);

}
