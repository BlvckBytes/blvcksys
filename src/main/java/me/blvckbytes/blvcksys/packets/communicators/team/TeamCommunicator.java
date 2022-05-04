package me.blvckbytes.blvcksys.packets.communicators.team;

import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
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
  private final ILogger logger;

  public TeamCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
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

    try {
      // Create the scoreboard team packet's inner data model
      Object b = refl.createPacket(PacketPlayOutScoreboardTeam.b.class);

      refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.groupName()), 0);
      refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.prefix()), 1);
      refl.setFieldByType(b, IChatBaseComponent.class, new ChatMessage(group.suffix()), 2);
      refl.setFieldByType(b, String.class, "always", 0); // Name tag visibility: always, hideForOtherTeams, hideForOwnTeam, never
      refl.setFieldByType(b, String.class, "never", 1); // Collision rule (physical collisions): always, pushOtherTeams, pushOwnTeam, never

      chatFormatFromColor(group.nameColor().getChar()).ifPresent(ecf -> {
        refl.setFieldByType(b, EnumChatFormat.class, ecf, 0); // Player name color
      });

      refl.setFieldByType(b, int.class, 0x00, 0); // Bit mask. 0x01: Allow friendly fire, 0x02: can see invisible players on same team.

      // Create the packet itself using all initialized parameters
      Object pack =refl.invokeConstructor(
        PacketPlayOutScoreboardTeam.class,
        group.priority() + group.groupName(), // Unique team name
        action.getMode(),     // Mode (0=create, 1=remove, 2=update, 3=add entites, 4=remove entities)
        Optional.of(b),       // Optional team (not needed for (1 | 3 | 4), I guess?)
        memberNames           // Names of all team members
      );

      return refl.sendPacket(p, pack);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }

  /**
   * Get a EnumChatFormat by it's color character
   * @param color Color character
   * @return Result or empty on errors
   */
  private Optional<EnumChatFormat> chatFormatFromColor(char color) {
    try {
      // Loop all enum values
      for (EnumChatFormat cf : EnumChatFormat.values()) {
        Character colorCode = refl.getFieldByType(cf, char.class, 0);

        // Color char matches
        if (colorCode == color)
          return Optional.of(cf);
      }

      // Not found
      return Optional.empty();
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }
}
