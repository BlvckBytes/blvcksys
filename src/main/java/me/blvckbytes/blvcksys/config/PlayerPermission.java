package me.blvckbytes.blvcksys.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  All known permissions which features can use to check for authorization
*/
@AllArgsConstructor
public enum PlayerPermission {
  //=========================================================================//
  //                                Arrow Trails                             //
  //=========================================================================//

  ARROWTRAILS("bvs.arrowtrails"),

  //=========================================================================//
  //                               Teleportations                            //
  //=========================================================================//

  TELEPORTATIONS_BYPASS("bvs.teleportations.bypass"),

  //=========================================================================//
  //                                  ToggleChat                             //
  //=========================================================================//

  TOGGLECHAT_BYPASS("bvs.togglechat.bypass"),

  //=========================================================================//
  //                             ITEMEDITOR Command                          //
  //=========================================================================//

  COMMAND_ITEMEDITOR("bvs.itemeditor"),

  //=========================================================================//
  //                              CRATEKEYS Command                          //
  //=========================================================================//

  COMMAND_CRATEKEYS_MANAGE("bvs.cratekeys.manage"),

  //=========================================================================//
  //                                MSGSPY Command                           //
  //=========================================================================//

  COMMAND_MSGSPY("bvs.msgspy"),

  //=========================================================================//
  //                            SERVERSETTINGS Command                       //
  //=========================================================================//

  COMMAND_SERVERSETTINGS("bvs.serversettings"),

  //=========================================================================//
  //                                 AFK Command                             //
  //=========================================================================//

  AFK_COOLDOWN_BYPASS("bvs.afk.cooldown.bypass"),

  //=========================================================================//
  //                              ENDERCHEST Command                         //
  //=========================================================================//

  COMMAND_ENDERCHEST_OTHERS("bvs.enderchest.others"),
  COMMAND_ENDERCHEST_MAX_SLOTS("bvs.enderchest.max_slots"),

  //=========================================================================//
  //                                MONEY Command                            //
  //=========================================================================//

  COMMAND_MONEY("bvs.money"),
  COMMAND_MONEY_OTHERS("bvs.money.others"),

  //=========================================================================//
  //                               IFRAME Command                            //
  //=========================================================================//

  COMMAND_IFRAME("bvs.iframe"),

  //=========================================================================//
  //                               PSIGN Command                             //
  //=========================================================================//

  COMMAND_PSIGN("bvs.psign"),

  //=========================================================================//
  //                                HOLO Command                             //
  //=========================================================================//

  COMMAND_HOLO("bvs.holo"),

  //=========================================================================//
  //                                 KIT Command                             //
  //=========================================================================//

  COMMAND_SETKIT("bvs.setkit"),
  COMMAND_KITITEM("bvs.kititem"),
  COMMAND_DELKIT("bvs.delkit"),
  COMMAND_KIT_OTHERS("bvs.kit.others"),
  COMMAND_KIT_COOLDOWN_BYPASS("bvs.kit.cooldown.bypass"),

  //=========================================================================//
  //                                WARP Command                             //
  //=========================================================================//

  COMMAND_SETWARP("bvs.setwarp"),
  COMMAND_DELWARP("bvs.delwarp"),

  //=========================================================================//
  //                              GAMEMODE Command                           //
  //=========================================================================//

  COMMAND_GAMEMODE("bvs.gamemode"),
  COMMAND_GAMEMODE_OTHERS("bvs.gamemode.others"),

  //=========================================================================//
  //                               FORWARD Command                           //
  //=========================================================================//

  COMMAND_FORWARD("bvs.forward"),

  //=========================================================================//
  //                                MUTE Command                             //
  //=========================================================================//

  COMMAND_MUTE("bvs.mute"),

  //=========================================================================//
  //                               TEMPBAN Command                           //
  //=========================================================================//

  COMMAND_TEMPBAN("bvs.tempban"),

  //=========================================================================//
  //                              TEMPIPBAN Command                          //
  //=========================================================================//

  COMMAND_TEMPIPBAN("bvs.tempipban"),

  //=========================================================================//
  //                                IPBAN Command                            //
  //=========================================================================//

  COMMAND_IPBAN("bvs.ipban"),

  //=========================================================================//
  //                                MUTES Command                            //
  //=========================================================================//

  COMMAND_MUTES("bvs.mutes"),

  //=========================================================================//
  //                                 BANS Command                            //
  //=========================================================================//

  COMMAND_BANS("bvs.bans"),

  //=========================================================================//
  //                              CLEARWARNS Command                         //
  //=========================================================================//

  COMMAND_CLEARWARNS("bvs.clearwarns"),

  //=========================================================================//
  //                               DELMUTE Command                           //
  //=========================================================================//

  COMMAND_DELWARN("bvs.delwarn"),

  //=========================================================================//
  //                               DELMUTE Command                           //
  //=========================================================================//

  COMMAND_DELMUTE("bvs.delmute"),

  //=========================================================================//
  //                                DELBAN Command                           //
  //=========================================================================//

  COMMAND_DELBAN("bvs.delban"),

  //=========================================================================//
  //                             MUTEREVOKE Command                          //
  //=========================================================================//

  COMMAND_MUTEREVOKE("bvs.muterevoke"),

  //=========================================================================//
  //                              BANREVOKE Command                          //
  //=========================================================================//

  COMMAND_BANREVOKE("bvs.banrevoke"),

  //=========================================================================//
  //                              WARNREVOKE Command                         //
  //=========================================================================//

  COMMAND_WARNREVOKE("bvs.warnrevoke"),

  //=========================================================================//
  //                               WARNS Command                             //
  //=========================================================================//

  COMMAND_WARNS("bvs.warns"),

  //=========================================================================//
  //                                WARN Command                             //
  //=========================================================================//

  COMMAND_WARN("bvs.warn"),

  //=========================================================================//
  //                              TEMPWARN Command                           //
  //=========================================================================//

  COMMAND_TEMPWARN("bvs.tempwarn"),

  //=========================================================================//
  //                                 BAN Command                             //
  //=========================================================================//

  COMMAND_BAN("bvs.ban"),

  //=========================================================================//
  //                                BACK Command                             //
  //=========================================================================//

  COMMAND_BACK("bvs.back"),

  //=========================================================================//
  //                                  UP Command                             //
  //=========================================================================//

  COMMAND_UP("bvs.up"),

  //=========================================================================//
  //                                 DOWN Command                            //
  //=========================================================================//

  COMMAND_DOWN("bvs.down"),

  //=========================================================================//
  //                                 HAT Command                             //
  //=========================================================================//

  COMMAND_HAT("bvs.hat"),

  //=========================================================================//
  //                                 TOP Command                             //
  //=========================================================================//

  COMMAND_TOP("bvs.top"),

  //=========================================================================//
  //                                BOTTOM Command                           //
  //=========================================================================//

  COMMAND_BOTTOM("bvs.bottom"),

  //=========================================================================//
  //                                TPALL Command                            //
  //=========================================================================//

  COMMAND_TPALL("bvs.tpall"),

  //=========================================================================//
  //                                 HEAL Command                            //
  //=========================================================================//

  COMMAND_HEAL("bvs.heal"),
  COMMAND_HEAL_OTHERS("bvs.heal.others"),
  COMMAND_HEAL_COOLDOWN("bvs.heal.cooldown"),
  COMMAND_HEAL_COOLDOWN_BYPASS("bvs.heal.cooldown.bypass"),

  //=========================================================================//
  //                                 FEED Command                            //
  //=========================================================================//

  COMMAND_FEED("bvs.feed"),
  COMMAND_FEED_OTHERS("bvs.feed.others"),
  COMMAND_FEED_COOLDOWN("bvs.feed.cooldown"),
  COMMAND_FEED_COOLDOWN_BYPASS("bvs.feed.cooldown.bypass"),

  //=========================================================================//
  //                                LEVEL Command                            //
  //=========================================================================//

  COMMAND_LEVEL("bvs.level"),
  COMMAND_LEVEL_OTHERS("bvs.level.others"),

  //=========================================================================//
  //                              SMITHING Command                           //
  //=========================================================================//

  COMMAND_SMITHING("bvs.smithing"),

  //=========================================================================//
  //                            STONECUTTER Command                          //
  //=========================================================================//

  COMMAND_STONECUTTER("bvs.stonecutter"),

  //=========================================================================//
  //                               LOOM Command                             //
  //=========================================================================//

  COMMAND_LOOM("bvs.loom"),

  //=========================================================================//
  //                            GRINDSTONE Command                           //
  //=========================================================================//

  COMMAND_GRINDSTONE("bvs.grindstone"),

  //=========================================================================//
  //                           ENCHANTING Command                            //
  //=========================================================================//

  COMMAND_ENCHANTING("bvs.enchanting"),

  //=========================================================================//
  //                            WORKBENCH Command                            //
  //=========================================================================//

  COMMAND_WORKBENCH("bvs.workbench"),

  //=========================================================================//
  //                              ANVIL Command                              //
  //=========================================================================//

  COMMAND_ANVIL("bvs.anvil"),

  //=========================================================================//
  //                              TRASH Command                              //
  //=========================================================================//

  COMMAND_TRASH("bvs.trash"),

  //=========================================================================//
  //                               KILL Command                              //
  //=========================================================================//

  COMMAND_KILL("bvs.kill"),

  //=========================================================================//
  //                                TP Command                               //
  //=========================================================================//

  COMMAND_TP("bvs.tp"),
  COMMAND_TP_OTHERS("bvs.tp.others"),

  //=========================================================================//
  //                             LongChat Command                            //
  //=========================================================================//

  COMMAND_LONGCHAT("bvs.longchat"),
  COMMAND_LONGCHAT_LIMITLESS("bvs.longchat.limitless"),

  //=========================================================================//
  //                         ClearInventory Command                          //
  //=========================================================================//

  COMMAND_CLEARINVENTORY_SELF("bvs.clearinventory"),
  COMMAND_CLEARINVENTORY_OTHERS("bvs.clearinventory.others"),

  //=========================================================================//
  //                             Survey Command                              //
  //=========================================================================//

  COMMAND_SURVEY("bvs.survey"),

  //=========================================================================//
  //                            ClearChat Command                            //
  //=========================================================================//

  COMMAND_CLEARCHAT_SELF("bvs.clearchat"),
  COMMAND_CLEARCHAT_GLOBAL("bvs.clearchat.global"),

  //=========================================================================//
  //                             SignEdit Command                            //
  //=========================================================================//

  COMMAND_SIGNEDIT("bvs.signedit"),

  //=========================================================================//
  //                            VANISH Command                               //
  //=========================================================================//

  COMMAND_VANISH("bvs.vanish"),
  COMMAND_VANISH_BYPASS("bvs.vanish.bypass"),

  //=========================================================================//
  //                           PWEATHER Command                              //
  //=========================================================================//

  COMMAND_PTIME("bvs.ptime"),
  COMMAND_PTIME_OTHERS("bvs.ptime.others"),

  //=========================================================================//
  //                           PWEATHER Command                              //
  //=========================================================================//

  COMMAND_PWEATHER("bvs.pweather"),
  COMMAND_PWEATHER_OTHERS("bvs.pweather.others"),

  //=========================================================================//
  //                            WEATHER Command                              //
  //=========================================================================//

  COMMAND_WEATHER("bvs.weather"),

  //=========================================================================//
  //                              KICK Command                               //
  //=========================================================================//

  COMMAND_KICK("bvs.kick"),
  COMMAND_KICK_ALL("bvs.kick.all"),
  COMMAND_KICK_UNKICKABLE("bvs.kick.unkickable"),
  COMMAND_KICK_UNKICKABLE_OVERRIDE("bvs.kick.unkickable.override"),
  COMMAND_KICK_KICKALL_BYPASS("bvs.kick.kickall_bypass"),

  //=========================================================================//
  //                              HOMES Command                              //
  //=========================================================================//

  COMMAND_HOMES_OTHERS("bvs.homes.others"),

  //=========================================================================//
  //                               HOME Command                              //
  //=========================================================================//

  COMMAND_HOME_OTHERS("bvs.home.others"),

  //=========================================================================//
  //                             SETHOME Command                             //
  //=========================================================================//

  COMMAND_SETHOME_MAX("bvs.sethome.max"),
  COMMAND_SETHOME_MAX_BYPASS("bvs.sethome.max.bypass"),

  //=========================================================================//
  //                             REPAIR Command                              //
  //=========================================================================//

  COMMAND_REPAIR("bvs.repair"),
  COMMAND_REPAIR_ALL("bvs.repair.all"),
  COMMAND_REPAIR_COOLDOWN("bvs.repair.cooldown"),
  COMMAND_REPAIR_COOLDOWN_BYPASS("bvs.repair.cooldown.bypass"),

  //=========================================================================//
  //                            BROADCAST Command                            //
  //=========================================================================//

  COMMAND_BROADCAST("bvs.broadcast"),

  //=========================================================================//
  //                             INVSEE Command                              //
  //=========================================================================//

  COMMAND_INVSEE("bvs.invsee"),
  COMMAND_INVSEE_ALTER("bvs.invsee.alter"),

  //=========================================================================//
  //                               NPC Command                               //
  //=========================================================================//

  COMMAND_NPC("bvs.npc"),

  //=========================================================================//
  //                             CRATE Command                               //
  //=========================================================================//

  COMMAND_CRATE("bvs.crate"),

  //=========================================================================//
  //                              TIME Command                               //
  //=========================================================================//

  COMMAND_TIME("bvs.time"),

  //=========================================================================//
  //                            GIVEHAND Command                             //
  //=========================================================================//

  COMMAND_GIVEHAND("bvs.givehand"),
  COMMAND_GIVEHAND_ALL("bvs.givehand.all"),

  //=========================================================================//
  //                               FLY Command                               //
  //=========================================================================//

  COMMAND_FLY("bvs.fly"),
  COMMAND_FLY_OTHERS("bvs.fly.others"),

  //=========================================================================//
  //                               GIVE Command                              //
  //=========================================================================//

  COMMAND_GIVE("bvs.give"),
  COMMAND_GIVE_OTHERS("bvs.give.others"),
  COMMAND_GIVE_ALL("bvs.give.all"),

  //=========================================================================//
  //                              INJECT Command                             //
  //=========================================================================//

  COMMAND_INJECT("bvs.inject"),

  //=========================================================================//
  //                                Chat Colors                              //
  //=========================================================================//

  CHAT_COLOR_PREFIX("bvs.chatcolor."),

  //=========================================================================//
  //                                Sign Colors                              //
  //=========================================================================//

  SIGN_COLOR_PREFIX("bvs.signcolor.")
  ;

  @Getter
  private final String value;

  /**
   * Checks whether or not this player has the permission
   * @param p Player to check
   */
  public boolean has(Player p) {
    return p.hasPermission(value);
  }

  /**
   * Checks whether or not this player has the permission and supports
   * adding a suffix to it (concrete value of a template permission)
   * @param p Player to check
   * @param suffix Suffix to add to the permission's string
   */
  public boolean has(Player p, String suffix) {
    return p.hasPermission(joinPermissions(value, suffix));
  }

  /**
   * Safely join two permissions to always end up with a proper notation
   * @param a Permission A
   * @param b Permission B
   * @return Permission A joined with permission B
   */
  private String joinPermissions(String a, String b) {
    // Colliding dots, remove trailing dot from a
    if (a.endsWith(".") && b.startsWith("."))
      return a.substring(0, a.length() - 1) + b;

    // Missing dot, add trailing dot to a
    if (!a.endsWith(".") && !b.startsWith("."))
      return a + "." + b;

    // Just join the strings and the dot will match
    return a + b;
  }

  /**
   * Get the highest available number (if any) that's suffixed onto
   * the current permission, like: permission.number_suffix
   * @return Highest ocurring number there was
   */
  public Optional<Integer> getSuffixNumber(Player p, boolean highest) {
    String marker = getValue() + ".";

    Integer result = p.getEffectivePermissions()
      .stream()
      .map(PermissionAttachmentInfo::getPermission)
      .filter(perm -> perm.startsWith(marker))
      .map(perm -> {
        try {
          return Integer.parseInt(perm.substring(marker.length()));
        } catch (NumberFormatException e) {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .sorted(highest ? Comparator.naturalOrder() : Comparator.reverseOrder())
      .reduce(null, (acc, curr) -> curr);

    return result == null ? Optional.empty() : Optional.of(result);
  }
}
