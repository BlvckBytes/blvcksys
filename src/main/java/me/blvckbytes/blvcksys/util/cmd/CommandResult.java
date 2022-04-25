package me.blvckbytes.blvcksys.util.cmd;

/**
 * Represents the result of an executed player-command
 * @param error Type of error that occurred
 * @param text Text representation of this error, or just an argument
 */
public record CommandResult(
  CommandError error,
  String text
) {}
