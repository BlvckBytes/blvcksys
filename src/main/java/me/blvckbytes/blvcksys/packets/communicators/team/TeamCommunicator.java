package me.blvckbytes.blvcksys.packets.communicators.team;

import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Creates all packets in regards to creating/updating/removing scoreboard teams.
*/
@AutoConstruct
public class TeamCommunicator implements ITeamCommunicator {

  private final MCReflect refl;

  public TeamCommunicator(
    @AutoInject MCReflect refl
  ) {
    this.refl = refl;
  }

  @Override
  public boolean sendScoreboardTeam(
    Player p,
    TeamGroup group,
    TeamAction action,
    @Nullable Collection<? extends Player> members
  ) {
    // Provide a fallback to allow for null-values
    // Null-values are used when just the team itself is modified
    if (members == null)
      members = new ArrayList<>();

    // Create a list of member names
    final List<String> memberNames = members.stream().map(Player::getName).toList();

    // Create the scoreboard team packet's inner data model
    return refl.createPacket(PacketPlayOutScoreboardTeam.b.class)
      .flatMap(b -> {

        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.groupName()), 0);
        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.prefix()), 1);
        refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.suffix()), 2);
        refl.setFieldByType(b, String.class, "always", 0); // Name tag visibility: always, hideForOtherTeams, hideForOwnTeam, never
        refl.setFieldByType(b, String.class, "pushOwnTeam", 1); // Collision rule: always, pushOtherTeams, pushOwnTeam, never

        chatFormatFromColor(group.nameColor().getChar()).ifPresent(ecf -> {
          refl.setFieldByType(b, EnumChatFormat.class, ecf, 0); // Player name color
        });

        refl.setFieldByType(b, int.class, 0x00, 0); // Bit mask. 0x01: Allow friendly fire, 0x02: can see invisible players on same team.

        // Create the packet itself using all initialized parameters
        return refl.invokeConstructor(
          PacketPlayOutScoreboardTeam.class,
          group.priority() + group.groupName(), // Unique team name
          action.getMode(),     // Mode (0=create, 1=remove, 2=update, 3=add entites, 4=remove entities)
          Optional.of(b),       // Optional team (not needed for (1 | 3 | 4), I guess?)
          memberNames           // Names of all team members
        ).map(o -> (Packet<?>) o);
      })
      .map(pack -> refl.sendPacket(p, pack))
      .orElse(false);
  }

  /**
   * Get a EnumChatFormat by it's color character
   * @param color Color character
   * @return Result or empty on errors
   */
  private Optional<EnumChatFormat> chatFormatFromColor(char color) {
    // Loop all enum values
    for (EnumChatFormat cf : EnumChatFormat.values()) {
      Optional<Object> colorCode = refl.getFieldByType(cf, char.class);

      // Could not get the color code of this enum entry
      if (colorCode.isEmpty())
        continue;

      // Color char matches
      if (((char) colorCode.get()) == color)
        return Optional.of(cf);
    }

    // Not found
    return Optional.empty();
  }
}
