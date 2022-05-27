package me.blvckbytes.blvcksys.packets.modifiers;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import me.blvckbytes.blvcksys.commands.APlayerCommand;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInTabComplete;
import net.minecraft.network.protocol.game.PacketPlayOutTabComplete;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Reads incoming tab completion request packets and then responds with
  suggestions based on the registered commands within the command class map.
  Placeholders are rendered colorful without shadow-text above the chat-bar.
*/
@AutoConstruct
public class TabCompletionPacketModifier implements IPacketModifier {

  // Mapping the last completion requests to their players
  private final Map<UUID, String> lastCompletions;
  private final MCReflect refl;
  private final ILogger logger;

  public TabCompletionPacketModifier(
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
    ) {
    this.lastCompletions = new ConcurrentHashMap<>();
    this.refl = refl;
    this.logger = logger;

    interceptor.register(this, ModificationPriority.HIGH);
  }

  @Override
  public Packet<?> modifyIncoming(UUID sender, PacketSource ps, Packet<?> incoming) {
    // Not an active player packet
    if (sender == null)
      return incoming;

    if (!(incoming instanceof PacketPlayInTabComplete))
      return incoming;

    // Incoming tab completion packet, telling the server what's in the chat bar
    // There's only one int (transaction ID) and a String (value) inside this packet
    try {
      Object o = refl.getFieldByType(incoming, String.class, 0);
      lastCompletions.put(sender, o.toString());
    } catch (Exception e) {
      logger.logError(e);
    }

    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    // Not an active player packet
    if (receiver == null)
      return outgoing;

    // Outgoing tab completion packet, telling the client which suggestions are available and at
    // what offset they should be rendered (from the left hand side of the screen)
    if (outgoing instanceof PacketPlayOutTabComplete) {

      // No last completion present yet
      String lastCompletion = lastCompletions.get(receiver);
      if (lastCompletion == null)
        return outgoing;

      // Try to get the corresponding command
      Optional<APlayerCommand> cmd = APlayerCommand.getByCommand(
        lastCompletion.substring(1, lastCompletion.indexOf(' '))
      );

      // Unknown command
      if (cmd.isEmpty())
        return outgoing;

      try {
        Suggestions sug = refl.getFieldByType(outgoing, Suggestions.class, 0);

        // Get the list of suggestions within that object
        List<?> sugs = (List<?>) refl.getFieldByName(sug, "suggestions");

        boolean isArgsOnly = true;
        for (Object suggestion : sugs) {
          // Get the text of this suggestion
          String text = refl.getFieldByName(suggestion, "text").toString();

          // Get the text's first and last char
          char fc = text.charAt(0);
          char lc = text.charAt(text.length() - 1);

          // Not a mandatory or an optional argument placeholder (indicated by brackets)
          if (!(fc == '<' && lc == '>' || fc == '[' && lc == ']')) {
            isArgsOnly = false;
            break;
          }

          // Color in placeholder
          refl.setFieldByName(suggestion, "text", cmd.get().colorizeUsage(text, false));

          // Set the argument description as a hover-tooltip
          // Decide on the index through the present number of spaces
          String desc = cmd.get().getArgumentDescripton(Math.max(0, lastCompletion.split(" ").length - 1));
          refl.setFieldByName(suggestion, "tooltip", new ChatMessage(desc));
        }

        // Only consists of argument placeholders
        if (isArgsOnly) {
          // Decrement the top level string-range's start so the suggestion isn't rendered into the textbox
          StringRange sr = refl.getFieldByType(sug, StringRange.class, 0);
          int start = (int) refl.getFieldByName(sr, "start");
          refl.setFieldByName(sr, "start", Math.max(0, start - 1));
        }
      } catch (Exception e) {
        logger.logError(e);
      }
    }

    return outgoing;
  }
}
