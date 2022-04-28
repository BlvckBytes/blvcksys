package me.blvckbytes.blvcksys.packets.communicators.signeditor;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Communicates opening a sign editor GUI and retrieving the entered lines of text.
*/
public interface ISignEditorCommunicator {

  /**
   * Open a new sign editor GUI and retrieve it's result after committing
   * @param p Target player
   * @param lines Lines to be displayed initially
   * @param submit Lines after committing
   */
  void openSignEditor(Player p, String[] lines, Consumer<String[]> submit);
}
