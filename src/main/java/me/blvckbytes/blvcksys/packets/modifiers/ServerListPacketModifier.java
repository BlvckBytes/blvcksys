package me.blvckbytes.blvcksys.packets.modifiers;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.packets.ModificationPriority;
import me.blvckbytes.blvcksys.packets.PacketSource;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import net.minecraft.network.protocol.status.ServerPing;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/24/2022

  Modifies outgoing serverlist packets to set the icon, the text, override
  the currently online player-hover with custom text and optionally override
  the player sample (online/slots) with a custom text as well.
*/
@AutoConstruct
public class ServerListPacketModifier implements IPacketModifier {

  private final MCReflect refl;
  private final IConfig cfg;
  private final JavaPlugin plugin;
  private final ILogger logger;

  // Serverlist-Icon base64, preloaded for use with all requests
  private String encodedIcon;

  public ServerListPacketModifier(
    @AutoInject MCReflect refl,
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IPacketInterceptor interceptor
  ) {
    this.cfg = cfg;
    this.refl = refl;
    this.logger = logger;
    this.plugin = plugin;

    loadIconFile();
    interceptor.register(this, ModificationPriority.HIGH);
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(UUID sender, PacketSource ps, Packet<?> incoming) {
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    // Playerbound packet, not interested
    if (receiver != null)
      return outgoing;

    if (!(outgoing instanceof PacketStatusOutServerInfo si))
      return outgoing;

    try {
      ServerPing sp = refl.getFieldByType(si, ServerPing.class, 0);

      // Set the text
      refl.setFieldByType(
        sp, IChatBaseComponent.class,
        new ChatMessage(
          cfg.get(ConfigKey.PLAYERLIST_TEXT)
            .withVariable("version", refl.getPlayableVersion())
            .asScalar()
        ), 0
      );

      // Modify the icon base64 string value
      if (this.encodedIcon != null)
        refl.setFieldByType(sp, String.class, encodedIcon, 0);

      // Modify the version mismatch string that the client renders when the client version
      // cannot join the server
      Object sd = refl.getFieldByType(sp, ServerPing.ServerData.class, 0);
      refl.setFieldByType(
        sd, String.class,
        cfg.get(ConfigKey.PLAYERLIST_VERSION_MISMATCH)
          .withVariable("version", refl.getPlayableVersion())
          .asScalar(),
        0
      );

      // Modify the player-sample by overriding online players with fake players
      Object ps = refl.getFieldByType(sp, ServerPing.ServerPingPlayerSample.class, 0);

      // Map individual hover lines to gameprofiles with random UUIDs
      GameProfile[] profiles =
        cfg.get(ConfigKey.PLAYERLIST_HOVER)
          .asStream()
          .map(line -> new GameProfile(UUID.randomUUID(), line))
          .toArray(GameProfile[]::new);

      refl.setGenericFieldByType(ps, GameProfile[].class, GameProfile.class, profiles, 0);
    } catch (Exception e) {
      logger.logError(e);
    }

    return outgoing;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Load the icon file and convert it to Base64, if it exists
   */
  private void loadIconFile() {
    try {
      File iconFile = new File(plugin.getDataFolder() + "/icon.png");
      if (iconFile.exists()) {
        byte[] bytes = Files.readAllBytes(iconFile.toPath());
        encodedIcon = "data:image/png;base64," + new String(Base64.getEncoder().encode(bytes));
      }
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
