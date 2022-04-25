package me.blvckbytes.blvcksys.packets.modifiers;

import com.mojang.authlib.GameProfile;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import net.minecraft.network.protocol.status.ServerPing;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;

@AutoConstruct
public class ServerListPacketModifier implements IPacketModifier {

  private final MCReflect refl;
  private final IConfig cfg;
  private final JavaPlugin plugin;
  private final ILogger logger;
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
    interceptor.register(this);
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(Player sender, NetworkManager nm, Packet<?> incoming) {
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(Player receiver, NetworkManager nm, Packet<?> outgoing) {
    // Playerbound packet, not interested
    if (receiver != null)
      return outgoing;

    if (outgoing instanceof PacketStatusOutServerInfo si) {
      refl.getFieldByType(si, ServerPing.class)
        .ifPresent(sp -> {
          // Set the text
          refl.setFieldByType(
            sp, IChatBaseComponent.class,
            new ChatMessage(cfg.get(ConfigKey.PLAYERLIST_TEXT).asScalar())
          );

          // Modify the icon base64 string value
          if (this.encodedIcon != null)
            refl.setFieldByType(sp, String.class, encodedIcon);

          // Modify the player-sample by overriding online players with fake players
          String online = cfg.get(ConfigKey.PLAYERLIST_ONLINE).asScalar();
          if (!online.isEmpty()) {
            refl.getFieldByType(sp, ServerPing.ServerData.class)
              .ifPresent(sd -> {
                refl.setFieldByType(sd, String.class, online);
                refl.setFieldByType(sd, int.class, 0);
              });
          }

          // Modify the player-sample by overriding online players with fake players
          refl.getFieldByType(sp, ServerPing.ServerPingPlayerSample.class)
            .ifPresent(ps -> {
              // Map individual hover lines to gameprofiles with random UUIDs
              GameProfile[] profiles =
                cfg.get(ConfigKey.PLAYERLIST_HOVER)
                  .asStream()
                  .map(line -> new GameProfile(UUID.randomUUID(), line))
                  .toArray(GameProfile[]::new);

              refl.setArrayFieldByType(ps, "GameProfile", profiles);
            });
        });
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
