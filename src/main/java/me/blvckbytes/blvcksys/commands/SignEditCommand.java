package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.adapters.IRegionAdapter;
import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.packets.communicators.signeditor.ISignEditorCommunicator;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Enables a player to edit a sign without destroying and re-placing it.
*/
@AutoConstruct
public class SignEditCommand extends APlayerCommand {

  private final IRegionAdapter regions;
  private final ISignEditorCommunicator signEditor;
  private final IChatListener chat;

  public SignEditCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IRegionAdapter regions,
    @AutoInject ISignEditorCommunicator signEditor,
    @AutoInject IChatListener chat
  ) {
    super(
      plugin, logger, cfg, refl,
      "signedit,se",
      "Edit the sign you're pointing at",
      PlayerPermission.COMMAND_SIGNEDIT
    );

    this.regions = regions;
    this.signEditor = signEditor;
    this.chat = chat;
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Block b = p.getTargetBlockExact(10, FluidCollisionMode.NEVER);

    // Not a sign block
    if (b == null || !(b.getState() instanceof Sign s)) {
      p.sendMessage(
        cfg.get(ConfigKey.SIGNEDIT_NOSIGN)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Cannot build here
    if (!regions.canBuild(p, b.getLocation())) {
      p.sendMessage(
        cfg.get(ConfigKey.SIGNEDIT_NOBUILD)
          .withPrefix()
          .asScalar()
      );
      return;
    }

    // Get the current lines of the sign and
    // replace all colors to the alternate notation
    String[] currLines = Arrays.stream(s.getLines())
      .map(line -> line.replace("§", "&"))
      .toArray(String[]::new);

    // Open a new sign editor and patch the sign's lines on submit
    signEditor.openSignEditor(p, currLines, lines -> {
      // Support a dynamic number of lines
      for (int i = 0; i < Math.min(currLines.length, lines.length); i++)
        s.setLine(
          i,
          // Translate sign colors based on the player's permissions
          chat.translateColors(p, lines[i], PlayerPermission.SIGN_COLOR_PREFIX)
        );

      // Update the block and make changes visible
      s.update();
    });
  }
}
