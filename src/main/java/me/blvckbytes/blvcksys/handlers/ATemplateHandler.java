package me.blvckbytes.blvcksys.handlers;

import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  The base of a handler which requires to process live variable templates
  in an efficient manner.
*/
public abstract class ATemplateHandler {

  protected final ILiveVariableSupplier varSupp;

  public ATemplateHandler(
    ILiveVariableSupplier varSupp
  ) {
    this.varSupp = varSupp;
  }

  /**
   * Evaluate a line template consisting of strings and variable
   * type placeholders for a given player
   * @param p Target player
   * @param template Template to evaluate
   * @return Resulting string to display
   */
  protected String evaluateLineTemplate(Player p, List<Object> template) {
    StringBuilder sb = new StringBuilder();

    // Iterate all parts of this template
    for (Object part : template) {
      // Append the string as is
      if (part instanceof String s) {
        sb.append(s);
        continue;
      }

      // Resolve this variable and append the result
      if (part instanceof LiveVariable hv)
        sb.append(varSupp.resolveVariable(p, hv));
    }

    return sb.toString();
  }

  /**
   * Append an object (String or variable type) to a list of objects (the template)
   * @param template Template to append to
   * @param append Object to append
   */
  private void appendToTemplate(List<Object> template, Object append) {
    // Append the initial element or variables as they are
    if (template.size() == 0 || !(append instanceof String sc)) {
      template.add(append);
      return;
    }

    // Concat the last entry with the new, current entry
    int lastIndex = template.size() - 1;
    if (template.get(lastIndex) instanceof String sl) {
      String newLast = sl + sc;
      template.remove(lastIndex);
      template.add(newLast);
    }

    // Last entry was no string, just append
    else
      template.add(sc);
  }

  /**
   * Build a line template for a given player
   * @param line Line template
   * @return A tuple of the minimum update period (-1 for no variables) and the template part-list
   */
  protected Tuple<Long, List<Object>> buildLineTemplate(String line) {
    List<Object> res = new ArrayList<>();
    long minUpdatePeriod = -1;

    // Char iteration state machine
    int lastOpenCurly = -1;

    char[] lineChars = line.toCharArray();
    for (int i = 0; i < lineChars.length; i++) {
      char c = lineChars[i];

      // Found a variable notation begin
      if (c == '{') {

        // Already found a previous begin, push that range
        if (lastOpenCurly >= 0)
          appendToTemplate(res, line.substring(lastOpenCurly, i));

        lastOpenCurly = i;

        // Only continue if '{' isn't the whole string
        if (lineChars.length != 1)
          continue;
      }

      // Currently, there's an open curly bracket waiting for being closed
      if (lastOpenCurly >= 0) {

        // Variable notation found in range [lastOpenCurly,i]
        if (c == '}') {
          String varNotation = line.substring(lastOpenCurly, i + 1);
          LiveVariable var = LiveVariable.fromPlaceholder(varNotation);

          // Variable unknown, append unaltered notation
          if (var == null)
            appendToTemplate(res, varNotation);

            // Add variable type as placeholder
          else {
            appendToTemplate(res, var);

            // Update the minimum update period either initially or if the current
            // variable requires a tighter time-frame
            if (minUpdatePeriod < 0 || var.getUpdatePeriodTicks() < minUpdatePeriod)
              minUpdatePeriod = var.getUpdatePeriodTicks();
          }

          lastOpenCurly = -1;
        }

        // The last open curly never closed again, just add that range as is
        else if (i == lineChars.length - 1)
          appendToTemplate(res, line.substring(lastOpenCurly));
      }

      // Append all chars outside of variables
      else
        appendToTemplate(res, String.valueOf(c));
    }

    return new Tuple<>(minUpdatePeriod, res);
  }
}
