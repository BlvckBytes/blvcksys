package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.CooldownSessionModel;
import org.bukkit.entity.Player;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Handles storing cooldowns persistently.
*/
@AutoConstruct
public class PersistentCooldownHandler implements ICooldownHandler {

  private final IPersistence pers;

  public PersistentCooldownHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
  }

  @Override
  public Optional<Long> getCooldownRemaining(Player p, ICooldownable cooldownable) {
    long rem = CooldownSessionModel.getCooldownRemaining(p, pers, cooldownable.generateToken());
    return rem <= 0 ? Optional.empty() : Optional.of(rem);
  }

  @Override
  public void createCooldownFor(Player p, ICooldownable cooldownable) {
    CooldownSessionModel cooldown = new CooldownSessionModel(
      p,
      cooldownable.getDurationSeconds(),
      cooldownable.generateToken()
    );

    pers.store(cooldown);
  }
}
