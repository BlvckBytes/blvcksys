package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/31/2022

  A color, based on it's R, G and B components, each ranging from 0 to 255.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColorModel extends APersistentModel {

  @ModelProperty
  private Integer r, g, b;
}
