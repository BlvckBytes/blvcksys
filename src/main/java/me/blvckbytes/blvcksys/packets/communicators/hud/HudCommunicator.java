package me.blvckbytes.blvcksys.packets.communicators.hud;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import org.bukkit.entity.Player;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/19/2022

  Sends packets to display messages on the HUD of a player's client.
*/
@AutoConstruct
public class HudCommunicator implements IHudCommunicator {

  private final MCReflect refl;
  private final ILogger logger;

  public HudCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void sendTitle(
    Player p,
    String line1, String line2,
    int fadeIn, int stay, int fadeOut
  ) {
    try {
      refl.sendPacket(p, new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
      refl.sendPacket(p, new ClientboundSetTitleTextPacket(new ChatComponentText(line1)));
      refl.sendPacket(p, new ClientboundSetSubtitleTextPacket(new ChatComponentText(line2)));
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void sendActionBar(Player p, String text) {
    try {
      refl.sendPacket(p, new PacketPlayOutChat(
        new ChatComponentText(text),
        ChatMessageType.a((byte) 2),
        UUID.randomUUID()
      ));
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
