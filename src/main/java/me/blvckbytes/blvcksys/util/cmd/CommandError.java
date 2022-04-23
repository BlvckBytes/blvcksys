package me.blvckbytes.blvcksys.util.cmd;

/**
 * The resulting status of a player-command execution
 */
public enum CommandError {
  USAGE_MISMATCH,
  PLAYER_NOT_ONLINE,
  CUSTOM_ERROR,
  INT_UNPARSEABLE,
  NONE
}
