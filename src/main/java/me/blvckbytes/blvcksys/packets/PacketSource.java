package me.blvckbytes.blvcksys.packets;

import net.minecraft.network.NetworkManager;

import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Represents the source of a packet, which includes the network manager
  instance as well as a way to send packets programmatically.
*/
public record PacketSource(
  NetworkManager manager,
  Consumer<Object> send
) {}
