package me.blvckbytes.blvcksys.commands.exceptions;

import lombok.Getter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Represents an exception that occurred while invoking a command
 */
public class CommandException extends RuntimeException {

  @Getter
  private final BaseComponent text;

  public CommandException(String text) {
    this.text = new TextComponent(text);
  }

  public CommandException(BaseComponent text) {
    this.text = text;
  }
}
