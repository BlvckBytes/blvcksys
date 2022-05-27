package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.handlers.TriResult;
import net.minecraft.util.Tuple;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Public interfaces which the survey command provides to other consumers.
*/
public interface ISurveyCommand {

  /**
   * Get a list of possible answers
   */
  Set<String> getAnswers();

  /**
   * Place a vote on an answer
   * @param p Player that's voting
   * @param answer Answer to place on
   * @return Optional result, empty if there's no vote going on, otherwise
   *         SUCC means first vote success, EMPTY means already voted but
   *         the vote has been moved, ERR means the answer is invalid. The second
   *         parameter of the tuple is the exact answer.
   */
  Optional<Tuple<TriResult, String>> placeVote(Player p, String answer);

  /**
   * Whether there's an active survey going on
   */
  boolean isActive();
}
