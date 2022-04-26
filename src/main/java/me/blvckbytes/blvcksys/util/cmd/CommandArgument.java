package me.blvckbytes.blvcksys.util.cmd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.PlayerPermission;

/**
 * Represents an argument a command can be invoked with
 */
@Getter
@AllArgsConstructor
public class CommandArgument {

  private String name;
  private String description;

  private PlayerPermission permission;

  public CommandArgument(String name, String description) {
    this(name, description, null);
  }
}
