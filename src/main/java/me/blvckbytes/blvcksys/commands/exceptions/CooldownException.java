package me.blvckbytes.blvcksys.commands.exceptions;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;

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
  private static final char[] spanC = new char[] { 's', 'h', 'm', 'd', 'w', 'm' };
  private static final long[] spanD = new long[] { 1, MIN_S, HOUR_S, DAY_S, WEEK_S, MONTH_S };

  public CooldownException(IConfig cfg, long expiry) {
    super(
      cfg.get(ConfigKey.ERR_COOLDOWN)
        .withVariable("duration", formatDuration(expiry - System.currentTimeMillis()))
        .withPrefix()
        .asScalar()
    );
  }

  /**
   * Format a duration in milliseconds to a time string
   * containing months, weeks, days, hours, minutes and seconds
   * @param duration Duration in milliseconds
   * @return Formatted duration string
   */
  private static String formatDuration(long duration) {
    StringBuilder sb = new StringBuilder();
    long durS = duration / 1000;

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
