package me.blvckbytes.blvcksys.packets.communicators.objective;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Communicates creating/updating/removing scoreboard objectives to a client.
*/
public interface IObjectiveCommunicator {

  /**
   * Create and send a new objective packet
   * @param p Target player
   * @param identifier Unique identifier string for this objective
   * @param mode Packet mode
   * @param display Displayed name of this objective
   * @param unit Unit of scores
   */
  void sendObjective(
    Player p,
    String identifier,
    ObjectiveMode mode,
    @Nullable String display,
    @Nullable ObjectiveUnit unit
  );

  /**
   * Display a previously created objective
   * @param p Target player
   * @param identifier Unique identifier string of the objective
   * @param pos Position of the objective on the HUD
   */
  void displayObjective(Player p, String identifier, ObjectivePosition pos);

  /**
   * Update the score of an objective's member
   * @param p Target player
   * @param identifier Unique identifier string of the objective
   * @param name Name of the member who's score is going to be updated
   * @param delete Whether or not to delete this entry
   * @param score Actual score value
   */
  void updateScore(
    Player p,
    String identifier,
    String name,
    boolean delete,
    @Nullable Integer score
  );
}
