package me.blvckbytes.blvcksys.util.cmd;

/**
 * Represents the result of an executed player-command
 * @param error Type of error that occurred
 * @param args Arguments containing metadata
 */
public record CommandResult(
  CommandError error,
  Object ...args
) {}
