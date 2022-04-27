package me.blvckbytes.blvcksys.packets.communicators;

import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Creates all packets in regards to creating/updating/removing scoreboard objectives.
*/
@AutoConstruct
public class ScoreBoardCommunicator implements IScoreBoardCommunicator {

  private final MCReflect refl;

  public ScoreBoardCommunicator(
    @AutoInject MCReflect refl
  ) {
    this.refl = refl;
  }

  @Override
  public void sendObjective(
    Player p,
    String identifier,
    ObjectiveMode mode,
    @Nullable String display,
    @Nullable ObjectiveUnit unit
  ) {
    refl.createGarbageInstance(PacketPlayOutScoreboardObjective.class)
      .ifPresent(sop -> {

        // Unique identifier for this objective
        refl.setFieldByType(sop, String.class, identifier);

        // Objective's display-name
        if (display != null) {
          refl.setFieldByType(
            sop, IChatBaseComponent.class,
            new ChatComponentText(display)
          );
        }

        // Get the target's enum class (what a mouth-full)
        Class<IScoreboardCriteria.EnumScoreboardHealthDisplay> ehdC = IScoreboardCriteria.EnumScoreboardHealthDisplay.class;

        // Get the enum by it's string field
        if (unit != null) {
          refl.getEnumByField(ehdC, String.class, unit.getUnit(), 0)
            .ifPresent(eC -> {
              // Scoreboard health display type (integer/hearts)
              refl.setFieldByType(sop, ehdC, eC);
            });
        }

        // Packet mode (0=create, 1=remove, 2=update text)
        refl.setFieldByType(sop, int.class, mode.getMode());

        refl.sendPacket(p, sop);
      });
  }

  @Override
  public void displayObjective(Player p, String identifier, ObjectivePosition pos) {
    refl.createGarbageInstance(PacketPlayOutScoreboardDisplayObjective.class)
      .ifPresent(dop -> {

        // Unique identifier for the objective
        refl.setFieldByType(dop, String.class, identifier);

        // Position within the HUD
        refl.setFieldByType(dop, int.class, pos.getPosition());

        refl.sendPacket(p, dop);
      });
  }

  @Override
  public void updateScore(
    Player p,
    String identifier,
    String name,
    boolean delete,
    @Nullable Integer score
  ) {
    refl.createGarbageInstance(PacketPlayOutScoreboardScore.class)
      .ifPresent(ssp -> {

        // Name of the objective's member (score holder)
        refl.setFieldByType(ssp, String.class, name, 0);

        // Unique identifier for the objective
        refl.setFieldByType(ssp, String.class, identifier, 1);

        // Score value
        if (score != null)
          refl.setFieldByType(ssp, int.class, score);

        // Packet action
        Class<ScoreboardServer.Action> ssaC = ScoreboardServer.Action.class;
        refl.getEnumNth(ssaC, delete ? 1 : 0)
            .ifPresent(action -> {
              refl.setFieldByType(ssp, ssaC, action);
            });

        refl.sendPacket(p, ssp);
      });
  }
}
