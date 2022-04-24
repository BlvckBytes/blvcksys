package me.blvckbytes.blvcksys.packets.modifiers;

import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.ObjectStringifier;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import org.bukkit.entity.Player;

@AutoConstruct
public class ServerListPacketModifier implements IPacketModifier {

  private final MCReflect refl;
  private final ILogger logger;
  private final ObjectStringifier ostr;

  public ServerListPacketModifier(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger,
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject ObjectStringifier ostr
  ) {
    this.logger = logger;
    this.refl = refl;
    this.ostr = ostr;
    interceptor.register(this);
  }

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
      refl.getFieldByType(si, "ServerPing")
        .ifPresent(sp ->
            refl.setFieldByType(sp, "IChatBaseComponent", new ChatMessage("§cHello!\n§aLine 2"))
          );
    }

    return outgoing;
  }
}
