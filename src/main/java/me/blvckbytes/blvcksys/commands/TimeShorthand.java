package me.blvckbytes.blvcksys.commands;

import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Shorthand for settable times.
*/
public enum TimeShorthand {

  // 06:00
  MORNING(6 * 1000),

  // 11:00
  DAY(11 * 1000),

  // 22:00
  NIGHT(22 * 1000)
  ;

  @Getter
  private final int time;

  TimeShorthand(int time) {
    this.time = time;
  }
}