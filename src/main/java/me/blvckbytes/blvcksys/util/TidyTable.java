package me.blvckbytes.blvcksys.util;

import me.blvckbytes.blvcksys.commands.IFontWidthTable;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/16/2022

  Create a table that consists of multiple lines, where each
  line separates columns by a separator token. Then, make sure
  that all columns have the exact same width by padding shorter
  cells with extra spaces as symetrically as possible.
*/
public class TidyTable {

  private final String separatorToken;
  private final List<TextComponent> lines;
  private final IFontWidthTable fwTable;

  /**
   * Create a new tidy table using a special column separator token
   * @param separatorToken Token separating columns
   * @param fwTable Font width table
   */
  public TidyTable(String separatorToken, IFontWidthTable fwTable) {
    this.separatorToken = separatorToken;
    this.lines = new ArrayList<>();
    this.fwTable = fwTable;
  }

  /**
   * Add multiple lines separated by newline characters
   * @param text Text to add
   */
  public void addLines(String text) {
    for (String line : text.split("\n"))
      lines.add(new TextComponent(line));
  }

  /**
   * Add a single line
   * @param text Text to add
   * @return Internally created component, returned for further hover/click bindings
   */
  public TextComponent addLine(String text) {
    TextComponent line = new TextComponent(text);
    lines.add(line);
    return line;
  }

  /**
   * Display this table to a player
   * @param p Target player
   */
  public void displayTo(Player p) {
    List<Integer> dotWidths = new ArrayList<>();

    for (TextComponent line : lines) {
      if (!line.getText().contains(separatorToken))
        continue;

      String[] cols = ChatColor.stripColor(line.getText()).split(Pattern.quote(separatorToken));

      for (int i = 0; i < cols.length; i++) {
        String col = cols[i];

        int dotWidth = col.length() - 1;
        for (char c : col.toCharArray())
          dotWidth += fwTable.getDotWidth(c);

        if (i >= dotWidths.size())
          dotWidths.add(dotWidth);
        else if (dotWidth > dotWidths.get(i))
          dotWidths.set(i, dotWidth);
      }
    }

    for (TextComponent line : lines)
      p.spigot().sendMessage(padLineCells(dotWidths, line));
  }

  /**
   * Pad a line's cells with dots and spaces to fit the max cell widths as
   * close as possible. Sometimes, the delta is uneven, and it's only possible
   * to pad evenly. Then, the result will be off by a pixel.
   * @param maxCellWidths Max cell width list
   * @param line Line to pad
   * @return Padded line
   */
  private TextComponent padLineCells(List<Integer> maxCellWidths, TextComponent line) {

    int numToks = line.getText().length() - line.getText().replace(separatorToken, "").length();
    String[] cols = line.getText().split(Pattern.quote(separatorToken));
    StringBuilder newLine = new StringBuilder();

    for (int i = 0; i < cols.length; i++) {
      String col = cols[i];
      String colNC = ChatColor.stripColor(col);

      int dotWidth = colNC.length() - 1;
      for (char c : colNC.toCharArray())
        dotWidth += fwTable.getDotWidth(c);

      int dotDelta = (maxCellWidths.get(i) - dotWidth);
      int spaces = dotDelta / (fwTable.getDotWidth(' ') + 1);
      int dots = (int) Math.ceil((dotDelta % (fwTable.getDotWidth(' ') + 1)) / 2D);

      if (spaces > 0 || dots > 0) {
        int preD = (int) Math.floor(dots / 2D);
        int postD = (int) Math.ceil(dots / 2D);

        int preS = (int) Math.floor(spaces / 2D);
        int postS = (int) Math.ceil(spaces / 2D);

        // The first column should be left-aligned
        if (i == 0) {
          postD += preD;
          postS += preS;
          preS = preD = 0;
        }

        col = ".".repeat(preD) + " ".repeat(preS) + col + " ".repeat(postS) + ".".repeat(postD);
      }

      // Also append trailing separator tokens (when there's no "cell content" remaining)
      newLine.append(col).append((i == cols.length - 1 && cols.length == numToks + 1) ? "" : separatorToken);
    }

    line.setText(newLine.toString());
    return line;
  }
}
