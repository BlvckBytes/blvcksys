package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/26/2022

  Stores the completion of a task within a stage of a quest for a
  player. The task is identified by a unique token and the counter
  property counts the number of completions, as some tasks may require
  multiple executions to reach completion.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuestTaskModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  private OfflinePlayer player;

  // Unique token which identifies this task
  // Format: <quest_name>__<stage_name>__<task_index>
  @ModelProperty(isUnique = true)
  private String token;

  // How often this task has been completed
  @ModelProperty
  private int count;

}
