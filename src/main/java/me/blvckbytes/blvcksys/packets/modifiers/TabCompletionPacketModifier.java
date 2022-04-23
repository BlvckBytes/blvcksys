package me.blvckbytes.blvcksys.packets.modifiers;

import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.cmd.APlayerCommand;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInTabComplete;
import net.minecraft.network.protocol.game.PacketPlayOutTabComplete;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@AutoConstruct
public class TabCompletionPacketModifier implements IPacketModifier {

  // Mapping the last completion requests to their players
  private final Map<Player, String> lastCompletions;
  private final MCReflect refl;

  public TabCompletionPacketModifier(
    @AutoInject IPacketInterceptor interceptor,
    @AutoInject MCReflect refl
  ) {
    this.lastCompletions = new ConcurrentHashMap<>();
    this.refl = refl;
    interceptor.register(this);
  }

  @Override
  public Packet<?> modifyIncoming(Player sender, Packet<?> incoming) {
    // Incoming tab completion packet, telling the server what's in the chat bar
    // There's only one int (transaction ID) and a String (value) inside this packet
    if (incoming instanceof PacketPlayInTabComplete)
      refl.getFieldByType(incoming, "String").ifPresent(o -> lastCompletions.put(sender, o.toString()));

    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(Player receiver, Packet<?> outgoing) {
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

      // Get the suggestions object from this packet
      refl.getFieldByType(outgoing, "Suggestions").ifPresent(sug -> {

        // Get the list of suggestions within that object
        boolean isArgsOnly = refl.getFieldByName(sug, "suggestions")

          // Map this list to a boolean that signals whether or not all suggestions are placeholders
          .map(sugs -> {
            // Loop all suggestions
            for (Object suggestion : (List<?>) sugs) {
              // Get the text of this suggestion
              String text = refl.getFieldByName(suggestion, "text")
                .map(Object::toString)
                .orElse("");

              // Get the text's first and last char
              char fc = text.charAt(0);
              char lc = text.charAt(text.length() - 1);

              // Not a mandatory or an optional argument placeholder (indicated by brackets)
              if (!(fc == '<' && lc == '>' || fc == '[' && lc == ']'))
                return false;

              // Color in placeholder
              refl.setFieldByName(suggestion, "text", cmd.get().colorizeUsage(text));

              // Set the argument description as a hover-tooltip
              // Decide on the index through the present number of spaces
              String desc = cmd.get().getArgumentDescripton(Math.max(0, lastCompletion.split(" ").length - 1));
              refl.setFieldByName(suggestion, "tooltip", new ChatMessage(desc));
            }

            // Only consisting of placeholder(s)
            return true;
          }).orElse(false);

        // Only consists of argument placeholders
        if (isArgsOnly) {
          // Decrement the top level string-range's start so the suggestion isn't rendered into the textbox
          refl.getFieldByType(sug, "StringRange").ifPresent(sr -> {
            refl.getFieldByName(sr, "start").ifPresent(start -> {
              refl.setFieldByName(sr, "start", Math.max(0, ((int) start) - 1));
            });
          });
        }
      });
    }

    return outgoing;
  }
}
