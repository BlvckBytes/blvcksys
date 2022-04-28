package me.blvckbytes.blvcksys.packets.communicators.bookeditor;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Communicates opening a book editor GUI and retrieving the entered text.
*/
public interface IBookEditorCommunicator {

  /**
   * Open a new book editor GUI and retrieve it's pages after committing
   * @param p Target player
   * @param pages Initial pages to display
   * @param submit Book pages after committing
   * @return Success state
   */
  boolean openBookEditor(Player p, List<String> pages, Consumer<List<String>> submit);
}
