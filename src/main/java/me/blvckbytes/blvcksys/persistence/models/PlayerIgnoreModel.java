package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/19/2022

  A player may ignore another player on multiple different levels.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerIgnoreModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  private OfflinePlayer creator;

  @ModelProperty(isUnique = true)
  private OfflinePlayer target;

  @ModelProperty
  private boolean ignoresChat;

  @ModelProperty
  private boolean ignoresMsg;
}
