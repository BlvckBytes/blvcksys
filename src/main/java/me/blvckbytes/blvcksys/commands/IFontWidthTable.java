package me.blvckbytes.blvcksys.commands;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/16/2022

  Public interfaces which the font width table provides to other consumers.
*/
public interface IFontWidthTable {

  /**
   * Get the dot width of a character within minecraft's dot-matrix font.
   * The space between characters is one dot wide.
   * @param c Character to check for
   * @return Width in dots, zero means unprintable
   */
  int getDotWidth(char c);
}
