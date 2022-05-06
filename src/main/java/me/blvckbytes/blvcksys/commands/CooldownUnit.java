package me.blvckbytes.blvcksys.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Shorthand for cooldown timespan units.
*/
@Getter
@AllArgsConstructor
public enum CooldownUnit {

  SECONDS (1),
  MINUTES (60),
  HOURS (MINUTES.seconds * 60),
  DAYS (HOURS.seconds * 24),
  WEEKS (DAYS.seconds * 7),
  MONTHS (WEEKS.seconds * 30),
  YEARS (MONTHS.seconds * 365)
  ;

  private final int seconds;
}
