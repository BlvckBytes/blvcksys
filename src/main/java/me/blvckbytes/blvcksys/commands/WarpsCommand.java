package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@AutoConstruct
public class WarpsCommand extends APlayerCommand {

  private static final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  private final IPersistence pers;

  public WarpsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    super(
      plugin, logger, cfg, refl,
      "warps",
      "List all existing warps",
      null
    );

    this.pers = pers;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    List<WarpModel> warps = pers.list(WarpModel.class);

    p.sendMessage("Num warps: " + warps.size());

    for (WarpModel warp : warps) {

      World w = warp.getLoc().getWorld();
      Date ca = warp.getCreatedAt();
      Date ua = warp.getUpdatedAt();

      p.sendMessage("---------------[" + warp.getName() + "]-----------------");
      p.sendMessage("ID: " + warp.getId());
      p.sendMessage("Created at: " + (ca == null ? null : df.format(ca)));
      p.sendMessage("Updated at: " + (ua == null ? null : df.format(ua)));
      p.sendMessage("Creator Name: " + warp.getCreator().getName());
      p.sendMessage("Creator ID: " + warp.getCreator().getUniqueId());
      p.sendMessage("Loc X: " + warp.getLoc().getX());
      p.sendMessage("Loc Y: " + warp.getLoc().getY());
      p.sendMessage("Loc Z: " + warp.getLoc().getZ());
      p.sendMessage("Loc YAW: " + warp.getLoc().getYaw());
      p.sendMessage("Loc PITCH: " + warp.getLoc().getPitch());
      p.sendMessage("Loc WORLD: " + (w == null ? null : w.getName()));
      p.sendMessage("---------------[" + warp.getName() + "]-----------------");
    }
  }
}