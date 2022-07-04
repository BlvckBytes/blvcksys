package me.blvckbytes.blvcksys.config.sections;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.AConfigSection;

import java.util.HashMap;
import java.util.Map;

@Getter
public class GuiLayoutSection extends AConfigSection {

  private int rows;
  private boolean fill;
  private boolean border;
  private boolean animated;

  @CSMap(k = String.class, v = String.class)
  private Map<String, String> slots;

  public GuiLayoutSection() {
    this.rows = 1;
    this.slots = new HashMap<>();
  }
}
