package me.blvckbytes.blvcksys.util.cmd;

public record CommandResult(
  CommandError error,
  Object ...args
) {}
