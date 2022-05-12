package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/12/2022

  A frame which is used to display graphics ingame and is the main
  member of it's corresponding frame group.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageFrameModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty
  private Location loc;

  @ModelProperty
  private ImageFrameType type;

  @ModelProperty(isNullable = true)
  private String resource;

  @ModelProperty
  private OfflinePlayer creator;
}
