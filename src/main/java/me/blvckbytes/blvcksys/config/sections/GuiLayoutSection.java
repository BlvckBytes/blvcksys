package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/05/2022

  Represents a section containing parameters a GUI layout may have.
*/
@Getter
public class GuiLayoutSection extends AConfigSection {

  private int rows;
  private boolean fill;
  private boolean border;
  private boolean animated;
  private String paginated;

  @CSMap(k = String.class, v = String.class)
  private Map<String, String> slots;

  public GuiLayoutSection() {
    this.rows = 1;
    this.paginated = "";
    this.slots = new HashMap<>();
  }
}
