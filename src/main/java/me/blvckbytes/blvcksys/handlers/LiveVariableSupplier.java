package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Supplies live variable values that are evaluated in the context of a player.
*/
@AutoConstruct
public class LiveVariableSupplier implements ILiveVariableSupplier {

  private final Map<LiveVariable, Function<Player, String>> suppliers;

  public LiveVariableSupplier() {
    this.suppliers = new HashMap<>();

    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    // Player specific
    this.suppliers.put(LiveVariable.PLAYER_NAME, Player::getName);
    this.suppliers.put(LiveVariable.PLAYER_EXPERIENCE, p -> String.valueOf(p.getLevel()));
    this.suppliers.put(LiveVariable.WORLD_NAME, p -> p.getWorld().getName());

    // Date and time
    this.suppliers.put(LiveVariable.CURRENT_TIME, p -> timeFormat.format(new Date()));
    this.suppliers.put(LiveVariable.CURRENT_DATE, p -> dateFormat.format(new Date()));
    this.suppliers.put(LiveVariable.CURRENT_DAY, p -> switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
      case 1 -> "Sonntag";
      case 2 -> "Montag";
      case 3 -> "Dienstag";
      case 4 -> "Mittwoch";
      case 5 -> "Donnerstag";
      case 6 -> "Freitag";
      case 7 -> "Samstag";
      default -> "?";
    });
  }

  @Override
  public String resolveVariable(Player p, LiveVariable variable) {
    if (!this.suppliers.containsKey(variable))
      return variable.getPlaceholder();
    return this.suppliers.get(variable).apply(p);
  }
}
