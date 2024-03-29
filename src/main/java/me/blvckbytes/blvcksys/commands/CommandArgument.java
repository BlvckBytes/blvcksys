package me.blvckbytes.blvcksys.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.PlayerPermission;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Represents an argument a command can be invoked with
 */
@Getter
@AllArgsConstructor
public class CommandArgument {

  private String name;
  private String description;

  private String permission;

  public CommandArgument(String name, String description) {
    this(name, description, null);
  }
}
