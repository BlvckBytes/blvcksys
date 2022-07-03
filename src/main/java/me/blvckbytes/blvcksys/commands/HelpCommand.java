package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/29/2022

  Show all commands the executing player is allowed to execute with all their
  colored and hoverable usages within a book that's automatically opened.
 */
@AutoConstruct
public class HelpCommand extends APlayerCommand implements IFontWidthTable {

  // Lines per page of a written book
  private static final int LINES_PER_PAGE = 14;

  // Dots (pixels) per line
  private static final int DOTS_PER_LINE = 112;

  // Maps each ASCII character to it's width in dots
  private static final int[] dotWidths = new int[127];

  static {
    populateDotWidths();
  }

  public HelpCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "help",
      "Shows a list of all commands you can execute",
      null
    );
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    p.openBook(buildHelpBook(p));
  }

  /**
   * Build a help book item containing all the lines of help-text
   * for a specific player, based on their permissions
   * @param p Target player
   * @return Book item
   */
  private ItemStack buildHelpBook(Player p) {
    ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
    BookMeta meta = (BookMeta) book.getItemMeta();

    if (meta != null) {
      meta.setTitle(getName());
      meta.setAuthor(p.getName());
      meta.spigot().addPage(buildHelpPages(p));
      book.setItemMeta(meta);
    }

    return book;
  }

  /**
   * Build a custom help screen based on a player's permissions
   * @param p Player to build this screen for
   * @return Array of pages to display inside the book
   */
  private BaseComponent[][] buildHelpPages(Player p) {
    List<List<BaseComponent>> commandWords = new LinkedList<>();

    // Iterate all known commands
    Collection<APlayerCommand> commands = APlayerCommand.getCommands();
    for (APlayerCommand command : commands) {
      // The player cannot execute this command, don't render it
      if (command.getRootPerm() != null && !p.hasPermission(command.getRootPerm()))
        continue;

      List<BaseComponent> words = new LinkedList<>();

      // Command description
      String[] descWords = command.getDescription().split(" ");
      for (int j = 0; j < descWords.length; j++) {
        if (j != 0) words.add(new TextComponent(" "));
        words.add(new TextComponent(descWords[j]));
      }

      // Linebreak
      words.add(new TextComponent("\n"));

      // Command usage
      BaseComponent[] usage = command.buildAdvancedUsage(null, true);
      words.addAll(Arrays.asList(usage));

      commandWords.add(words);
    }

    return paginateWords(commandWords);
  }

  /**
   * Paginate a list of a list of words, where each inner list represents a
   * command and commands are spaced by an empty line (or a page-break)
   * @param commandWords Command word list
   * @return Array of pages, where a page is an array of BaseComponents
   */
  private BaseComponent[][] paginateWords(List<List<BaseComponent>> commandWords) {
    List<BaseComponent[]> pages = new LinkedList<>();
    List<BaseComponent> pageBuffer = new LinkedList<>();

    int currPageLines = 0;
    for (int i = 0; i < commandWords.size(); i++) {
      List<BaseComponent> command = commandWords.get(i);
      int lines = computeLines(command);

      // Fits on this page
      if (currPageLines + lines <= LINES_PER_PAGE)
        currPageLines += lines;

      // Begin a new page
      else {
        pages.add(pageBuffer.toArray(BaseComponent[]::new));
        pageBuffer.clear();
        currPageLines = lines;
      }

      pageBuffer.addAll(command);

      // Last command, commit page
      if (i == commandWords.size() - 1) {
        pages.add(pageBuffer.toArray(BaseComponent[]::new));
        break;
      }

      // Space out commands
      currPageLines++;
      pageBuffer.add(new TextComponent("\n\n"));
    }

    return pages.toArray(BaseComponent[][]::new);
  }

  /**
   * Computes the number of lines the list of components will take
   * up when rendered in a book
   * @param words List of words and spaces
   * @return Number of lines
   */
  private int computeLines(List<BaseComponent> words) {
    int lines = 0, dots = 0;

    for (BaseComponent word : words) {
      String text = ChatColor.stripColor(word.toPlainText());

      int currDots = 0;
      char[] chars = text.toCharArray();
      for (int i = 0; i < chars.length; i++) {
        char c = chars[i];

        // Forced linebreak
        if (c == '\n') {
          currDots = 0;
          dots = 0;
          lines++;
          continue;
        }

        // Count dots, one dot of space between each char
        currDots += getDotWidth(c) + (i == chars.length - 1 ? 0 : 1);
      }

      // Word fits on the current line
      if (DOTS_PER_LINE - dots >= currDots)
        dots += currDots;

      // Word doesn't fit anymore and will wrap to the next line
      else {
        lines++;
        dots = currDots;
      }
    }

    // There are still dots left that take up their line
    if (dots > 0)
      lines++;

    return lines;
  }

  /**
   * Populate the dot-width LUT according to the exception characters's width
   * <a href="https://minecraft.fandom.com/wiki/Book_and_Quill">Information Source</a>
   */
  private static void populateDotWidths() {
    // Exceptions to a 5 wide char
    dotWidths[' '] = 3;
    dotWidths['!'] = 1;
    dotWidths['"'] = 3;
    dotWidths['\''] = 1;
    dotWidths['('] = 3;
    dotWidths[')'] = 3;
    dotWidths['*'] = 3;
    dotWidths[','] = 1;
    dotWidths['.'] = 1;
    dotWidths[':'] = 1;
    dotWidths[';'] = 1;
    dotWidths['<'] = 4;
    dotWidths['>'] = 4;
    dotWidths['@'] = 6;
    dotWidths['I'] = 3;
    dotWidths['['] = 3;
    dotWidths[']'] = 3;
    dotWidths['`'] = 2;
    dotWidths['f'] = 4;
    dotWidths['i'] = 1;
    dotWidths['k'] = 4;
    dotWidths['l'] = 2;
    dotWidths['t'] = 3;
    dotWidths['{'] = 3;
    dotWidths['|'] = 1;
    dotWidths['}'] = 3;
    dotWidths['~'] = 6;
  }

  /**
   * Get the dot width of a character within minecraft's dot-matrix font.
   * The space between characters is one dot wide.
   * @param c Character to check for
   * @return Width in dots, zero means unprintable
   */
  public int getDotWidth(char c) {
    // Unprintable, thus 0
    if (c < 32 || c == 127)
      return 0;

    c = switch (c) {
      case 'ü' -> 'u';
      case 'Ü' -> 'U';
      case 'ä' -> 'a';
      case 'Ä' -> 'A';
      case 'ö' -> 'o';
      case 'Ö' -> 'O';
      default -> c;
    };

    // Non-ascii character, assume 5
    if (c > 127)
      return 5;

    // Either return the exception or return 5 (as is the case for all printable chars not in the table)
    return dotWidths[c] == 0 ? 5 : dotWidths[c];
  }
}
