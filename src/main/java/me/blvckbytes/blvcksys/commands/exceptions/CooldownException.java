package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigValue;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  A cooldown was still active while the executor tried to use this command.
*/
public class CooldownException extends CommandException {

  // Conversion constants for quick access
  private static final long MIN_S   = 60,
                            HOUR_S  = MIN_S  * 60,
                            DAY_S   = HOUR_S * 24,
                            WEEK_S  = DAY_S  * 7,
                            MONTH_S = WEEK_S * 30;

  // Lookup "table" for timespan characters and timespan durations, in ascending order
  private static final char[] spanC = new char[] { 's', 'm', 'h', 'd', 'w', 'm' };
  private static final long[] spanD = new long[] { 1, MIN_S, HOUR_S, DAY_S, WEEK_S, MONTH_S };

  /**
   * Create a new cooldown exception which automatically formats
   * the provided duration in seconds to a more human readable time string
   * @param cval Config value which supports the message to print to the user
   * @param duration Duration in seconds
   */
  public CooldownException(ConfigValue cval, long duration) {
    super(
      cval
        .withVariable("duration", formatDuration(duration))
        .asScalar()
    );
  }

  /**
   * Format a duration in seconds to a time string
   * containing months, weeks, days, hours, minutes and seconds
   * @param duration Duration in seconds
   * @return Formatted duration string
   */
  private static String formatDuration(long duration) {
    StringBuilder sb = new StringBuilder();
    long durS = duration;

    // Use min to not crash on uneven numbers of array entries
    for (int i = Math.min(spanC.length, spanD.length) - 1; i >= 0; i--) {
      // Duration of the current span in seconds
      long currD = spanD[i];

      // Get the current quotient and store the remainder for the next iteration
      long currQuot = durS / currD;
      durS = durS % currD;

      // Do not display zero quotients
      if (currQuot == 0)
        continue;

      // Append <space><quotient><span character>
      sb.append(' ');
      sb.append(currQuot);
      sb.append(spanC[i]);
    }

    return sb.toString().trim();
  }
}
