package me.blvckbytes.blvcksys.packets.modifiers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Modifies outgoing login disconnect packets to override disconnects that
  tell the user that their version isn't compatible with this server.
*/
@AutoConstruct
public class LoginDisconnectPacketModifier implements IPacketModifier, Listener {

  private final MCReflect refl;
  private final IConfig cfg;

  public LoginDisconnectPacketModifier(
    @AutoInject MCReflect refl,
    @AutoInject IConfig cfg,
    @AutoInject IPacketInterceptor interceptor
  ) {
    this.refl = refl;
    this.cfg = cfg;
    interceptor.register(this);
  }

  @Override
  public Packet<?> modifyIncoming(UUID sender, NetworkManager nm, Packet<?> incoming) {
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    // Player bound packet
    if (receiver != null)
      return outgoing;

    // Was a login disconnect
    if (outgoing instanceof PacketLoginOutDisconnect od) {
      refl.getFieldByType(od, IChatBaseComponent.class, 0).ifPresent(bc -> {
        String message = ChatColor.stripColor(bc.getString());

        // Didn't contain the version disconnect marker in it's message, skip
        if (!(
          message.toLowerCase().contains("outdated server!") ||
          message.toLowerCase().contains("outdated client!")
        ))
          return;

        // Build the new screen
        ChatMessage screen = new ChatMessage(
          cfg.get(ConfigKey.VERSION_DISCONNECT_SCREEN)
            .withVariable("version", refl.getPlayableVersion())
            .asScalar()
        );

        // Override the text value
        refl.setFieldByType(od, IChatBaseComponent.class, screen, 0);
      });
    }
    return outgoing;
  }
}
