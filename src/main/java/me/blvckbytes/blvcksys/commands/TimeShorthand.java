package me.blvckbytes.blvcksys.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Shorthand for settable times.
*/
@AllArgsConstructor
public enum TimeShorthand {

  // 1:00
  DAY(1000),

  // 22:00
  NIGHT(22 * 1000)
  ;

  @Getter
  private final int time;
}