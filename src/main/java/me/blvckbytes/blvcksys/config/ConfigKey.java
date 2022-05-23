package me.blvckbytes.blvcksys.config;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  All known keys which the configuration file needs to implement. Enum
  constants each have their own key as well as a value, which can consist of
  multiple strings that will be newline-separated automatically.
*/
public enum ConfigKey {

  // TODO: Fully integrate the palette and get rid of color-only keys
  // TODO: Move all command description texts into the config

  //=========================================================================//
  //                              Color Palette                              //
  //=========================================================================//

  // This palette is accessible within all templates as $0 ... $9
  PALETTE(
    "palette",
    /*
       $0 Neutral
       $1 Neutral Dark
       $2 Main
       $3 Secondary
       $4 Error
       $5 Error Dark
    */
    "78d5c4"
  ),

  //=========================================================================//
  //                               Chat Buttons                              //
  //=========================================================================//

  CHATBUTTONS_EXPIRED("chatbuttons.expired", "$0Dieser Button ist bereits $4abgelaufen$0!"),
  CHATBUTTONS_YES("chatbuttons.yes", "$1[&aJa$1]"),
  CHATBUTTONS_NO("chatbuttons.no", "$1[&cNein$1]"),
  CHATBUTTONS_EDIT("chatbuttons.edit", "$1[&6Bearbeiten$1]"),
  CHATBUTTONS_CANCEL("chatbuttons.cancel", "$1[&cAbbrechen$1]"),

  //=========================================================================//
  //                                   AFK                                   //
  //=========================================================================//

  AFK_WENT("afk.went", "$0Der Spieler $2{{name}} $0ist $2nun abwesend$0!"),
  AFK_RESUMED("afk.resumed", "$0Der Spieler $2{{name}} $0ist $2wieder anwesend$0!"),
  AFK_SUFFIX("afk.suffix", " $1&lAFK"),
  AFK_ALREADY("afk.already", "$0Du bist $4bereits AFK$0!"),

  //=========================================================================//
  //                               Global Prefix                             //
  //=========================================================================//

  PREFIX("prefix", "$1[$3BVS$1]$0 "),

  //=========================================================================//
  //                                 Logging                                 //
  //=========================================================================//

  LOGGING_PREFIX_DEBUG("logging_prefix.debug", "&6"),
  LOGGING_PREFIX_INFO("logging_prefix.info", "&a"),
  LOGGING_PREFIX_ERROR("logging_prefix.error", "$5"),

  //=========================================================================//
  //                                NPC Command                              //
  //=========================================================================//

  NPC_EXISTS("npc.exists", "$0Es existiert bereits ein NPC namens $4{{name}}$0!"),
  NPC_NOT_FOUND("npc.not_found", "$0Es existiert kein NPC namens $4{{name}}$0!"),
  NPC_DELETED("npc.deleted", "$0Der NPC $3{{name}} $0wurde gelöscht."),
  NPC_CREATED("npc.created", "$0Der NPC $3{{name}} $0wurde erstellt."),
  NPC_MOVED("npc.moved", "$0Der NPC $3{{name}} $0wurde zu dir bewegt."),
  NPC_SKIN_CHANGED("npc.skin_changed", "$0Der Skin des NPCs $3{{name}} $0wurde auf $3{{skin}} $0gesetzt."),
  NPC_SKIN_NOT_LOADABLE("npc.skin_not_loadable", "$0Der Skin von $4{{name}} $0konnte nicht geladen werden!"),

  //=========================================================================//
  //                               NPCS Command                              //
  //=========================================================================//

  NPC_LIST_NONE("npc.none", "$4Keine NPCs gefunden"),
  NPC_LIST_TELEPORTED("npc.teleported", "$0Du wurdest zum NPC $3{{name}} $0teleportiert."),
  NPC_LIST_PREFIX("npc.header", "$0NPCs im Radius von $3{{radius}} $0Blöcken: "),
  NPC_LIST_FORMAT("npc.list.format", "$3{{name}}$1{{sep}}"),
  NPC_LIST_HOVER_TEXT(
    "npc.list.hover.text",
    "$0Erstellt am: $3{{created_at}}",
    "$0Geändert am: $3{{updated_at}}",
    "$0Erstellt von: $3{{creator}}",
    "$0Skin: $3{{skin}}",
    "$0Position: $3{{location}}",
    "$0Distanz: $3{{distance}} Blöcke"
  ),

  //=========================================================================//
  //                                   MOTD                                  //
  //=========================================================================//

  MOTD_SCREEN_RELOGIN(
    "motd_screen.relogin",
    "&8&m----------------------------------------------",
    "$0Willkommen zurück, $2{{name}}$0!",
    "$0Letzter Login: $2{{last_login}}",
    "$0Aktuelle Spielerzahl: $2{{num_online}}$0/$2{{num_slots}}",
    "&8&m----------------------------------------------"
  ),
  MOTD_SCREEN_FIRST_JOIN(
    "motd_screen.first_join",
    "&8&m----------------------------------------------",
    "$0Willkommen auf unserem Server, $2{{name}}$0!",
    "$0Alle verfügbaren Befehle: $2/help",
    "$0Aktuelle Spielerzahl: $2{{num_online}}$0/$2{{num_slots}}",
    "&8&m----------------------------------------------"
  ),

  //=========================================================================//
  //                                   Chat                                  //
  //=========================================================================//

  CHAT_MESSAGE_FORMAT("chat.message_format", "{{prefix}}{{name}}$0: {{message}}"),
  CHAT_MESSAGE_DEF_COLOR("chat.message_def_color", "$0"),
  CHAT_TAG_FORMAT("chat.tag_format", "&6@{{name}}"),

  //=========================================================================//
  //                                  Sidebar                                //
  //=========================================================================//

  SIDEBAR_TITLE("sidebar.title", "$3&lBlvckBytes.DEV"),
  SIDEBAR_LINES(
    "sidebar.lines",
    " ",
    "Spieler:",
    "$2{{num_online}}$0/$2{{num_slots}}",
    " ",
    "Onlinezeit:",
    "$4Soon",
    " ",
    "Münzen:",
    "$2{{money}}",
    " ",
    "Kills/Deaths:",
    "$2{{kills}} $1| $2{{deaths}} $1| $2{{kd}}KD",
    " "
  ),

  //=========================================================================//
  //                                 Below Name                              //
  //=========================================================================//

  // Since minecraft burdens us with limitations here, this text
  // will always start with a white number of levels the player has.
  // Then, go from there and specify the rest
  BELOWNAME_TEXT(
    "belowname.text",
    "&a◎ $1| &r{{hearts}} &c❤"
  ),
  // Separates text and flags
  BELOWNAME_FLAGS_SEP("belowname.flags_sep", " $1| "),
  // Joins individual flags
  BELOWNAME_FLAGS_JOIN("belowname.flags_join", " $1| "),

  //=========================================================================//
  //                                 Tablist                                 //
  //=========================================================================//

  TABLIST_HEADER(
    "tablist.header",
    " ",
    "$3&lBlvckBytes.DEV",
    "$0Willkommen, $2{{player}}$0!",
    " ",
    "$1&m──────────────────────────────",
    " "
  ),
  TABLIST_FOOTER(
    "tablist.footer",
    " ",
    "$1&m──────────────────────────────",
    " ",
    "$2✦ $0Online: $2{{num_online}}$0/$2{{num_slots}} $1| $2⇄ $0Ping: $2{{ping}}ms $1| $2⌚ $0Uhrzeit: $2{{time}}",
    " "
  ),
  TABLIST_PREFIXES(
    "tablist.prefixes",
    "Admin;&cAdmin $1| &c",
    "Spieler;&2Spieler $1| &2"
  ),

  //=========================================================================//
  //                            Version Disconnect                           //
  //=========================================================================//

  VERSION_DISCONNECT_SCREEN(
    "version_disconnect.screen",
    "&5&lBlvckBytes.Dev",
    " ",
    "$0Deine $3Clientversion $0ist leider nicht mit unserer",
    "$0Serverversion von $3{{version}} $0kompatibel!"
  ),

  //=========================================================================//
  //                                Playerlist                               //
  //=========================================================================//

  PLAYERLIST_TEXT(
    "playerlist.text",
    "$0» $1❘ &5&lBlvckBytes.Dev $1[&a{{version}}$1]",
    "$0» $1❘ ➟ &aSkyPvP $1× &dSkyBlock $1× &6Farmwelt $1× &9Wie Früher!"
  ),
  PLAYERLIST_HOVER(
    "playerlist.hover",
    "$3Herzlich Willkommen!",
    " ",
    "$0Trete uns bei und werde",
    "$0Teil des Projektes,",
    "$0SkyPvP wieder zum",
    "$0Leben zu erwecken!"
  ),
  PLAYERLIST_VERSION_MISMATCH("playerlist.version_mismatch", "$2Version {{version}}"),

  //=========================================================================//
  //                               WARN Commands                             //
  //=========================================================================//

  WARN_REVOKED_BROADCAST(
    "warn.revoked_broadcast",
    "$0Der $3{{warn_number}}te Warn $0vom Spieler $3{{target}} $0wurde $3aufgehoben$0!",
    "$0Von: $3{{revoker}}",
    "$0Grund: $3{{revocation_reason}}"
  ),
  WARN_CASTED_BROADCAST(
    "warn.casted_broadcast",
    "$0Der Spieler $3{{target}} $0erhielt die $3{{warn_number}}te Verwarnung$0!",
    "$0Von: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Grund: $3{{reason}}"
  ),
  WARN_CLEARED_BROADCAST(
    "warn.cleared_broadcast",
    "$3{{num_cleared}} Warnungen $0von $3{{target}} $0wurden zurückgesetzt!",
    "$0Von: $3{{creator}}"
  ),
  WARN_DURATION_PERMANENT("warn.duration_permanent", "&cPermanent"),
  WARN_REMAINING_PERMANENT("warn.remaining_permanent", "&c/"),
  WARN_NO_REASON("warn.no_reason", "&cKein Grund angegeben"),
  WARN_NO_ADDRESS("warn.no_address", "&c/"),
  WARN_NO_REVOKED("warn.no_revoked", "&c/"),
  WARN_MAX_REACHED("warn.max_reached", "$0Der Spieler $4{{target}} $0hat bereits die maximale Anzahl an Warns von $4{{max_warns}} $0erreicht!"),
  WARN_UNKNOWN("warn.unknown", "$0Es existiert kein Warn mit der ID $4{{id}}$0!"),
  WARN_ALREADY_REVOKED("warn.already_revoked", "$0Der Warn mit der ID $4{{id}} $0wurde bereits aufgehoben!"),
  WARN_NOT_ACTIVE("warn.not_active", "$0Der Warn mit der ID $4{{id}} $0ist nicht mehr $4aktiv."),
  WARN_STILL_ACTIVE("warn.still_active", "$0Der Warn mit der ID $4{{id}} $0ist noch $4aktiv $0und kann daher nicht gelöscht werden."),
  WARN_DELETED("warn.deleted", "$0Der Warn mit der ID $3{{id}} $0wurde $3gelöscht$0!"),
  WARN_LIST_EMPTY("warn.list.empty", "$0Der Spieler $4{{target}} $0hat keine $4{{type}} $0Warns!"),
  WARN_LIST_HEADLINE(
    "warn.list.headline",
    "$3{{type}} $0Warns von $3{{target}}$0:",
    "$1| $3# $1| $3Ersteller $1| $3Datum $1| $3Dauer $1| $3Aktiv $1|"
  ),
  WARN_LIST_YES("warn.list.yes", "&aJa"),
  WARN_LIST_NO("warn.list.no", "&cNein"),
  WARN_LIST_ENTRY("warn.list.entry", "$1| $0{{warn_number}} $1| $0{{creator}} $1| $0{{created_at}} $1| $0{{duration}} $1| $0{{is_active}} $1|"),
  WARN_LIST_HOVER("warn.list.hover", "$0Klick: $3{{command}}"),
  WARN_NOT_REVOKED("warn.not_revoked", "$0Der Warn mit der ID $4{{id}} $0wurde $4noch nicht $0aufgehoben!"),
  WARN_EDIT_SAVED("warn.edit_saved", "$0Die Änderungen für den Warn mit der ID $3{{id}} $0wurden $3gespeichert$0."),
  WARN_INFO_SCREEN(
    "warn.info.screen",
    "$0Warn mit der ID $3{{id}}$0:",
    "$0#: $3{{warn_number}}",
    "$0Ziel: $3{{target}}",
    "$0Erstellt am: $3{{created_at}}",
    "$0Ersteller: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Verbleibend: $3{{remaining}}",
    "$0Grund: $3{{reason}}",
    "$0Aufgehoben von: $3{{revoker}}",
    "$0Aufgehoben am: $3{{revoked_at}}",
    "$0Aufhebegrund: $3{{revocation_reason}}"
  ),
  WARN_AUTO_PUNISHMENT_REASON(
    "warn.auto_punishment.reason",
    "$0Du hast die Maximalzahl von $3{{max_warns}} aktiven Warnungen $0erreicht!"
  ),
  WARN_CLEAR_CONFIRMATION("warn.clear.confirmation", "$3Alle ({{num_warns}}) $0Warnungen von $3{{target}} $0löschen?: "),
  WARN_CLEAR_CANCELLED("warn.clear.cancelled", "$0Die Leerung wurde $4abgebrochen$0!"),
  WARN_CLEAR_NO_WARNS("warn.clear.no_warns", "$0Der Spieler $4{{target}} $0besitzt $4keine Warns$0!"),

  //=========================================================================//
  //                               STATS Command                             //
  //=========================================================================//

  STATS_NONE("stats.none", "&c/"),
  STATS_SCREEN(
    "stats.screen",
    "$0Name: $3{{target}}",
    "$0Kills, Deaths, KD: $3{{kills}}$0/$3{{deaths}}$0/$3{{kd}}",
    "$0Coins: $3{{coins}}",
    "$0Warns gesamt: $3{{warns_total}}$0, davon aktiv: $3{{warns_active}}",
    "$0Mute: $3{{mute_duration}}$0, verbleibend: $3{{mute_remaining}}"
  ),

  //=========================================================================//
  //                                PAY Command                              //
  //=========================================================================//

  PAY_INVALID_AMOUNT("pay.invalid_amount", "$0Der Betrag muss $4größer als 0 $0sein!"),
  PAY_TOO_MUCH("pay.too_much", "$0Du kannst maximal $3{{available}} Coins $0überweisen!"),
  PAY_SELF("pay.self", "$0Du kannst $4nicht $0an dich $4selbst $0überweisen!"),
  PAY_HAS_NONE("pay.has_none", "$0Du hast $4keine Coins$0!"),
  PAY_TRANSFERED_SENDER("pay.transfered.sender", "$0Du hast $3{{amount}} Coins $0an $3{{target}} $0überwiesen."),
  PAY_TRANSFERED_RECEIVER("pay.transfered.receiver", "$3{{sender}} $0hat $3{{amount}} Coins $0an dich überwiesen."),

  //=========================================================================//
  //                               MUTE Commands                             //
  //=========================================================================//

  MUTE_REVOKED_BROADCAST(
    "mute.revoked_broadcast",
    "$0Der Mute vom Spieler $3{{target}} $0wurde $3aufgehoben$0!",
    "$0Von: $3{{revoker}}",
    "$0Grund: $3{{revocation_reason}}"
  ),
  MUTE_CASTED_BROADCAST(
    "mute.casted_broadcast",
    "$0Der Spieler $3{{target}} $0wurde $3gemuted$0!",
    "$0Von: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Grund: $3{{reason}}"
  ),
  MUTE_NO_REASON("mute.no_reason", "&cKein Grund angegeben"),
  MUTE_NO_REVOKED("mute.no_revoked", "&c/"),
  MUTE_SCREEN(
    "mute.screen",
    " ",
    "$0Du bist &cgemuted$0.",
    "$0Von: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Verbleibend: $3{{remaining}}",
    "$0Grund: $3{{reason}}",
    " "
  ),
  MUTE_ALREADY_MUTED("mute.already_muted", "$0Der Spieler $4{{target}} $0besitzt bereits einen $4aktiven $0Mute!"),
  MUTE_UNKNOWN("mute.unknown", "$0Es existiert kein Mute mit der ID $4{{id}}$0!"),
  MUTE_ALREADY_REVOKED("mute.already_revoked", "$0Der Mute mit der ID $4{{id}} $0wurde bereits aufgehoben!"),
  MUTE_INFO_SCREEN(
    "mute.info.screen",
    "$0Mute mit der ID $3{{id}}$0:",
    "$0Ziel: $3{{target}}",
    "$0Erstellt am: $3{{created_at}}",
    "$0Ersteller: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Verbleibend: $3{{remaining}}",
    "$0Grund: $3{{reason}}",
    "$0Aufgehoben von: $3{{revoker}}",
    "$0Aufgehoben am: $3{{revoked_at}}",
    "$0Aufhebegrund: $3{{revocation_reason}}"
  ),
  MUTE_NOT_REVOKED("mute.not_revoked", "$0Der Mute mit der ID $4{{id}} $0wurde $4noch nicht $0aufgehoben!"),
  MUTE_EDIT_SAVED("mute.edit_saved", "$0Die Änderungen für den Mute mit der ID $3{{id}} $0wurden $3gespeichert$0."),
  MUTE_STILL_ACTIVE("mute.still_active", "$0Der Mute mit der ID $4{{id}} $0ist noch $4aktiv $0und kann daher nicht gelöscht werden."),
  MUTE_NOT_ACTIVE("mute.not_active", "$0Der Mute mit der ID $4{{id}} $0ist nicht mehr $4aktiv."),
  MUTE_DELETED("mute.deleted", "$0Der Mute mit der ID $3{{id}} $0wurde $3gelöscht$0!"),
  MUTE_LIST_EMPTY("mute.list.empty", "$0Der Spieler $4{{target}} $0hat keine $4{{type}} $0Mutes!"),
  MUTE_LIST_HEADLINE(
    "mute.list.headline",
    "$3{{type}} $0Mutes von $3{{target}}$0:",
    "$1| $3Ersteller $1| $3Datum $1| $3Dauer $1| $3Aktiv $1|"
  ),
  MUTE_LIST_YES("mute.list.yes", "&aJa"),
  MUTE_LIST_NO("mute.list.no", "&cNein"),
  MUTE_LIST_ENTRY("mute.list.entry", "$1| $0{{creator}} $1| $0{{created_at}} $1| $0{{duration}} $1| $0{{is_active}} $1|"),
  MUTE_LIST_HOVER("mute.list.hover", "$0Klick: $3{{command}}"),

  //=========================================================================//
  //                                BAN Commands                             //
  //=========================================================================//

  BAN_REVOKED_BROADCAST(
    "ban.revoked_broadcast",
    "$0Der Bann vom Spieler $3{{target}} $0wurde $3aufgehoben$0!",
    "$0Von: $3{{revoker}}",
    "$0Grund: $3{{revocation_reason}}"
  ),
  BAN_CASTED_BROADCAST(
    "ban.casted_broadcast",
    "$0Der Spieler $3{{target}} $0wurde $3gebannt$0!",
    "$0Von: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Grund: $3{{reason}}"
  ),
  BAN_DURATION_PERMANENT("ban.duration_permanent", "&cPermanent"),
  BAN_REMAINING_PERMANENT("ban.remaining_permanent", "&c/"),
  BAN_NO_REASON("ban.no_reason", "&cKein Grund angegeben"),
  BAN_NO_ADDRESS("ban.no_address", "&c/"),
  BAN_NO_REVOKED("ban.no_revoked", "&c/"),
  BAN_SCREEN(
    "ban.screen",
    "$0Du wurdest &cgebannt$0.",
    " ",
    "$0Von: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Verbleibend: $3{{remaining}}",
    "$0Grund: $3{{reason}}"
  ),
  BAN_ALREADY_BANNED("ban.already_banned", "$0Der Spieler $4{{target}} $0besitzt bereits einen $4aktiven $0Bann!"),
  BAN_LIST_HEADLINE(
    "ban.list.headline",
    "$3{{type}} $0Bans von $3{{target}}$0:",
    "$1| $3Ersteller $1| $3Datum $1| $3Dauer $1| $3IP $1| $3Aktiv $1|"
  ),
  BAN_LIST_EMPTY("ban.list.empty", "$0Der Spieler $4{{target}} $0hat keine $4{{type}} $0Bans!"),
  BAN_LIST_ENTRY("ban.list.entry", "$1| $0{{creator}} $1| $0{{created_at}} $1| $0{{duration}} $1| $0{{has_ip}} $1| $0{{is_active}} $1|"),
  BAN_LIST_HOVER("ban.list.hover", "$0Klick: $3{{command}}"),
  BAN_LIST_YES("ban.list.yes", "&aJa"),
  BAN_LIST_NO("ban.list.no", "&cNein"),
  BAN_UNKNOWN("ban.unknown", "$0Es existiert kein Bann mit der ID $4{{id}}$0!"),
  BAN_STILL_ACTIVE("ban.still_active", "$0Der Bann mit der ID $4{{id}} $0ist noch $4aktiv $0und kann daher nicht gelöscht werden."),
  BAN_NOT_ACTIVE("ban.not_active", "$0Der Bann mit der ID $4{{id}} $0ist nicht mehr $4aktiv."),
  BAN_DELETED("ban.deleted", "$0Der Bann mit der ID $3{{id}} $0wurde $3gelöscht$0!"),
  BAN_ALREADY_REVOKED("ban.already_revoked", "$0Der Bann mit der ID $4{{id}} $0wurde bereits aufgehoben!"),
  BAN_EDIT_SAVED("ban.edit_saved", "$0Die Änderungen für den Bann mit der ID $3{{id}} $0wurden $3gespeichert$0."),
  BAN_NOT_REVOKED("ban.not_revoked", "$0Der Bann mit der ID $4{{id}} $0wurde $4noch nicht $0aufgehoben!"),
  BAN_IS_PERMANENT("ban.is_permanent", "$0Der Bann mit der ID $4{{id}} $0ist $4permanent!"),
  BAN_INFO_SCREEN(
    "ban.info.screen",
    "$0Bann mit der ID $3{{id}}$0:",
    "$0Ziel: $3{{target}}",
    "$0Erstellt am: $3{{created_at}}",
    "$0Ersteller: $3{{creator}}",
    "$0Dauer: $3{{duration}}",
    "$0Verbleibend: $3{{remaining}}",
    "$0IP: $3{{ip}}",
    "$0Grund: $3{{reason}}",
    "$0Aufgehoben von: $3{{revoker}}",
    "$0Aufgehoben am: $3{{revoked_at}}",
    "$0Aufhebegrund: $3{{revocation_reason}}"
  ),

  //=========================================================================//
  //                              GAMEMODE Command                           //
  //=========================================================================//

  GAMEMODE_SELF_SET("gamemode.self.set", "$0Dein $3GameMode $0wurde von $3{{prev_mode}} $0auf $3{{curr_mode}} $0gesetzt!"),
  GAMEMODE_SELF_HAS("gamemode.self.has", "$0Dein $3GameMode $0ist bereits $3{{mode}}$0!"),
  GAMEMODE_OTHERS_SENDER_SET("gamemode.others.receiver", "$0Der $3GameMode $0von $3{{target}} $0wurde von $3{{prev_mode}} $0auf $3{{curr_mode}} $0gesetzt!"),
  GAMEMODE_OTHERS_SENDER_HAS("gamemode.others.sender.has", "$0Der $3GameMode $0von $3{{target}} $0ist bereits $3{{mode}}$0!"),
  GAMEMODE_OTHERS_RECEIVER("gamemode.others.sender.set", "$0Dein $3GameMode $0wurde durch $3{{issuer}} $0von $3{{prev_mode}} $0auf $3{{curr_mode}} $0gesetzt!"),

  //=========================================================================//
  //                                  UP Command                             //
  //=========================================================================//

  UP_TELEPORTED("up.teleported", "$0Du wurdest zum $3nächst höheren $0Block $3über $0dir teleportiert."),
  UP_AIR("up.air", "$0Es befindet sich $3kein $0weiterer Block $3über $0dir!"),

  //=========================================================================//
  //                                 DOWN Command                            //
  //=========================================================================//

  DOWN_TELEPORTED("down.teleported", "$0Du wurdest zum $3nächst tieferen $0Block $3unter $0dir teleportiert."),
  DOWN_VOID("down.void", "$0Es befindet sich $3kein $0weiterer Block $3unter $0dir!"),

  //=========================================================================//
  //                               BOTTOM Command                            //
  //=========================================================================//

  BOTTOM_TELEPORTED("bottom.teleported", "$0Du wurdest zum $3tiefsten $0Block unter dir $3teleportiert$0."),
  BOTTOM_VOID("bottom.void", "$0Es befindet sich $3kein weiterer Block $0unter dir!"),

  //=========================================================================//
  //                                 TOP Command                             //
  //=========================================================================//

  TOP_TELEPORTED("top.teleported", "$0Du wurdest zum $3höchsten $0Block über dir $3teleportiert$0."),
  TOP_AIR("top.air", "$0Es befindet sich $3kein weiterer Block $0über dir!"),

  //=========================================================================//
  //                                INVSEE Command                           //
  //=========================================================================//

  INVSEE_SELF("invsee.self", "$0Du kannst dein $3eigenes Inventar $0nicht beobachten!"),

  //=========================================================================//
  //                              BROADCAST Command                          //
  //=========================================================================//

  BROADCAST_FORMAT("broadcast.format", "$1[$3Broadcast$1]$0 {{message}}"),
  BROADCAST_HOVER(
    "broadcast.hover",
    "$0Gesendet von: $3{{issuer}}"
  ),

  //=========================================================================//
  //                                BACK Command                             //
  //=========================================================================//

  BACK_TELEPORTED("back.teleported", "$0Du wurdest zu deiner $3vorherigen $0Teleport-Position gebracht."),
  BACK_NONE("back.none", "$0Es existiert keine $3vorherige $0Teleport-Position mehr!"),

  //=========================================================================//
  //                               FORWARD Command                           //
  //=========================================================================//

  FORWARD_TELEPORTED("forward.teleported", "$0Du wurdest zu deiner $3nächsten $0Teleport-Position gebracht."),
  FORWARD_NONE("forward.none", "$0Es existiert keine $3nächste $0Teleport-Position mehr!"),

  //=========================================================================//
  //                                TPALL Command                            //
  //=========================================================================//

  TPALL_CONFIRMATION_PREFIX("tpall.confirmation_prefix", "$0Bist du sicher, dass du $3alle Spieler $0zu dir teleportieren möchtest?: "),
  TPALL_BROADCAST("tpall.broadcast", "$0Der Spieler $3{{issuer}} $0hat $3alle Spieler $0zu sich teleportiert!"),
  TPALL_CANCELLED("tpall.cancelled", "$0Die Teleportation wurde $3abgebrochen$0!"),

  //=========================================================================//
  //                                MONEY Command                            //
  //=========================================================================//

  MONEY_GET_SELF("money.get.self", "$0Du hast $3{{money}} Coins$0."),
  MONEY_GET_OTHERS("money.get.others", "$3{{target}} $0hat $3{{money}} Coins$0."),
  MONEY_SET_SELF("money.set.self", "$0Dein Konto wurde auf $3{{money}} Coins $0gesetzt ($3{{delta}} &6₵$0)."),
  MONEY_SET_OTHERS_SENDER("money.set.others.sender", "$0Du hast das Konto von $3{{target}} $0auf $3{{money}} Coins $0gesetzt ($3{{delta}} &6₵$0)."),
  MONEY_SET_OTHERS_RECEIVER("money.set.others.receiver", "$0Dein Konto wurde von $3{{issuer}} $0auf $3{{money}} Coins $0gesetzt ($3{{delta}} &6₵$0)."),

  //=========================================================================//
  //                               LEVELS Command                            //
  //=========================================================================//

  LEVELS_GET_SELF("levels.get.self", "$0Du hast $3{{level}} Level$0."),
  LEVELS_GET_OTHERS("levels.get.others", "$3{{target}} $0hat $3{{level}} Level$0."),
  LEVELS_SET_SELF("levels.set.self", "$0Deine Erfahrung wurde auf $3{{level}} Level $0gesetzt ($3{{delta}} &a◎$0)."),
  LEVELS_SET_OTHERS_SENDER("levels.set.others.sender", "$0Du hast die Erfahrung von $3{{target}} $0auf $3{{level}} Level $0gesetzt ($3{{delta}} &a◎$0)."),
  LEVELS_SET_OTHERS_RECEIVER("levels.set.others.receiver", "$0Deine Erfahrung wurde von $3{{issuer}} $0auf $3{{level}} Level $0gesetzt ($3{{delta}} &a◎$0)."),

  //=========================================================================//
  //                                HEAL Command                             //
  //=========================================================================//

  HEAL_SELF("heal.self", "$0Du hast dich $3geheilt $0($3{{delta}} &c❤$0)."),
  HEAL_OTHERS_SENDER("heal.others.sender", "$0Du hast $3{{target}} $0geheilt ($3{{delta}} &c❤$0)."),
  HEAL_OTHERS_RECEIVER("heal.others.receiver", "$0Du wurdest von $3{{issuer}} $0geheilt ($3{{delta}} &c❤$0)."),

  //=========================================================================//
  //                                FEED Command                             //
  //=========================================================================//

  FEED_SELF("feed.self", "$0Du hast deinen $3Hunger $0gestillt ($3{{delta}} &6\uD83C\uDF56$0)."),
  FEED_OTHERS_SENDER("feed.others.sender", "$0Du hast den $3Hunger $0von $3{{target}} $0gestillt ($3{{delta}} &6\uD83C\uDF56$0)."),
  FEED_OTHERS_RECEIVER("feed.others.receiver", "$0Dein $3Hunger $0wurde von $3{{issuer}} $0gestillt ($3{{delta}} &6\uD83C\uDF56$0)."),

  //=========================================================================//
  //                            STONECUTTER Command                          //
  //=========================================================================//

  STONECUTTER_GUINAME("stonecutter.guiname", "$3{{owner}}'s Steinsäge"),

  //=========================================================================//
  //                             SMITHING Command                            //
  //=========================================================================//

  SMITHING_GUINAME("smithing.guiname", "$3{{owner}}'s Schmiedetisch"),

  //=========================================================================//
  //                               LOOM Command                              //
  //=========================================================================//

  LOOM_GUINAME("loom.guiname", "$3{{owner}}'s Webstuhl"),

  //=========================================================================//
  //                            GRINDSTONE Command                           //
  //=========================================================================//

  GRINDSTONE_GUINAME("grindstone.guiname", "$3{{owner}}'s Schleifstein"),

  //=========================================================================//
  //                            ENCHANTING Command                           //
  //=========================================================================//

  ENCHANTING_GUINAME("enchanting.guiname", "$3{{owner}}'s Zaubertisch"),

  //=========================================================================//
  //                             WORKBENCH Command                           //
  //=========================================================================//

  WORKBENCH_GUINAME("workbench.guiname", "$3{{owner}}'s Werkbank"),

  //=========================================================================//
  //                               ANVIL Command                             //
  //=========================================================================//

  ANVIL_GUINAME("anvil.guiname", "$3{{owner}}'s Amboss"),

  //=========================================================================//
  //                               TRASH Command                             //
  //=========================================================================//

  TRASH_INV_TITLE("trash.inv_title", "$3{{owner}}'s Mülleimer"),
  TRASH_CONFIRMATION(
    "trash.confirmation",
    "$0Der Mülleimer wird in $2{{timeout}}s $0automatisch $2geleert$0!",
    "$0Möchtest du die Items $2wiederherstellen$0?: "
  ),
  TRASH_CLEARED_MANUAL("trash.cleared.manual", "$0Du hast deinen Mülleimer $2geleert$0!"),
  TRASH_CLEARED_AUTOMATIC("trash.cleared.automatic", "$0Dein Mülleimer $0wurde $2automatisch geleert$0!"),
  TRASH_CLEARED_CANCELLED("trash.cleared.cancelled", "$0Dein Mülleimer wurde $2wiederhergestellt$0!"),
  TRASH_DUMPED("trash.dumped", "$0Dein $2Mülleimer wurde in dein $2Inventar $0gelegt!"),
  TRASH_DUMP_DROPPED("trash.dump_dropped", "$0Es wurden $4{{dropped}} $0Items $4fallen gelassen$0!"),

  //=========================================================================//
  //                                KILL Command                             //
  //=========================================================================//

  KILL_SENDER("kill.sender", "$0Du hast $2{{target}} $0per $2Befehl $0getötet!"),
  KILL_RECEIVER("kill.receiver", "$0Du wurdest von $2{{issuer}} $0per $2Befehl $0getötet!"),

  //=========================================================================//
  //                                 TP Command                              //
  //=========================================================================//

  TP_SELF("tp.self", "$0Du hast dich zu $2{{target}} $0teleportiert!"),
  TP_OTHER_SENDER("tp.other.sender", "$0Du hast $2{{player}} $0zu $2{{target}} $0teleportiert!"),
  TP_OTHER_RECEIVER("tp.other.receiver", "$0Du wurdest von $2{{issuer}} $0zu $2{{target}} $0teleportiert!"),

  //=========================================================================//
  //                                 CombatLog                               //
  //=========================================================================//

  COMBATLOG_BROADCAST("combatlog.broadcast", "$0Der Spieler $2{{name}} $0hat sich $2im Kampf $0ausgeloggt und wurde $2getötet$0."),
  COMBATLOG_INFO("combatlog.info", "$0Du bist noch $2{{remaining_seconds}}s $0im Kampf, $2nicht ausloggen$0!"),
  COMBATLOG_DONE("combatlog.done", "$0Du bist $2nicht mehr $0im Kampf$0."),

  //=========================================================================//
  //                               Teleportations                            //
  //=========================================================================//

  TELEPORTATIONS_MOVED("teleportations.moved", "$4Du hast dich bewegt!"),
  TELEPORTATIONS_INITIATED("teleportations.initiated", "$0Teleportation eingeleitet, $2nicht bewegen$0..."),

  //=========================================================================//
  //                                TPA Command                              //
  //=========================================================================//

  TPA_STILL_PENDING("tpa.still_pending", "$0Es steht bereits eine $4aktive Teleport-Anfrage $0für $4{{target}} $0aus!"),
  TPA_EXPIRED_SENDER("tpa.expired.sender", "$0Deine $4ausgehende $0Teleport-Anfrage an $4{{target}} $0ist abgelaufen!"),
  TPA_EXPIRED_RECEIVER("tpa.expired.receiver", "$0Eine $4eingehende $0Teleport-Anfrage von $4{{sender}} $0ist abgelaufen!"),
  TPA_CANCELLED_SENDER("tpa.cancelled.sender", "$0Deine Anfrage an $4{{target}} $0wurde $4abgebrochen$0!"),
  TPA_CANCELLED_RECEIVER("tpa.cancelled.receiver", "$0Die Anfrage von $4{{sender}} $0wurde $4abgebrochen$0!"),
  TPA_DENIED_SENDER("tpa.denied.sender", "$0Deine Anfrage an $4{{target}} $0wurde $4abgelehnt$0!"),
  TPA_DENIED_RECEIVER("tpa.denied.receiver", "$0Die Anfrage von $4{{sender}} $0wurde $4abgelehnt$0!"),
  TPA_MOVED_SENDER("tpa.moved.sender", "$0Deine Anfrage an $4{{target}} $0wurde abgebrochen!"),
  TPA_MOVED_RECEIVER("tpa.moved.receiver", "$4{{sender}} $0hat sich bewegt, die Anfrage wurde $4abgebrochen$0!"),
  TPA_TELEPORTED_SENDER("tpa.teleported.sender", "$0Du wurdest zu $2{{target}} $0teleportiert."),
  TPA_TELEPORTED_RECEIVER("tpa.teleported.receiver", "$2{{sender}} $0wurde zu dir teleportiert."),
  TPA_SENDER_QUIT("tpa.sender_quit", "$0Der Sender $4{{sender}} $0einer ausstehenden Teleport-Anfrage hat den Server $4verlassen$0!"),
  TPA_RECEIVER_QUIT("tpa.receiver_quit", "$0Der Empfänger $4{{target}} $0einer ausstehenden Teleport-Anfrage hat den Server $4verlassen$0!"),
  TPA_SENT("tpa.sent", "$0Deine Anfrage an $2{{target}} $0wurde versendet!"),
  TPA_SELF("tpa.self", "$0Du kannst $4dir selbst $0keine Anfragen senden!"),
  TPA_NONE_RECEIVED("tpa.none.received", "$0Du hast $4keine $0aktive Anfrage von $4{{sender}}$0!"),
  TPA_NONE_SENT("tpa.none.sent", "$0Du hast noch keine Anfrage an $4{{target}} $0versendet!"),
  TPA_ACCEPTED_SENDER("tpa.accepted.sender", "$0Deine Anfrage an $2{{target}} $0wurde $2angenommen$0!"),
  TPA_ACCEPTED_RECEIVER("tpa.accepted.receiver", "$0Die Anfrage von $2{{sender}} $0wurde $2angenommen$0!"),
  TPA_RECEIVED_PREFIX("tpa.received_prefix", "$2{{sender}} $0möchte sich zu dir teleportieren: "),

  //=========================================================================//
  //                            IMAGEFRAME Command                           //
  //=========================================================================//

  IMAGEFRAME_NO_BLOCK("imageframe.no_block", "$0Es befindet sich $4kein Block $0auf deinem Fadenkreuz!"),
  IMAGEFRAME_NO_FRAME("imageframe.no_frame", "$0Es befindet sich $4kein Rahmen $0auf deinem Fadenkreuz!"),
  IMAGEFRAME_ALREADY_REGISTERED("imageframe.already_registered", "$0Dieser Rahmen ist bereits als $4{{name}} $0registriert!"),
  IMAGEFRAME_GROUP_NOT_FOUND("imageframe.group_not_found", "$0Es existiert keine Gruppe namens $4{{name}}$0!"),
  IMAGEFRAME_GROUP_EXISTS("imageframe.group_exists", "$0Es existiert bereits eine Gruppe namens $4{{name}}$0!"),
  IMAGEFRAME_GROUP_CREATED("imageframe.group_created", "$0Die Gruppe $3{{name}} $0wurde angelegt."),
  IMAGEFRAME_GROUP_DELETED("imageframe.group_deleted", "$0Die Gruppe $3{{name}} $0wurde gelöscht."),
  IMAGEFRAME_GROUP_RELOADED("imageframe.group_reloaded", "$0Die Gruppe $3{{name}} $0wurde neu geladen."),

  //=========================================================================//
  //                              Repair Command                             //
  //=========================================================================//

  REPAIR_HAND_SUCCESS("repair.repaired.hand.success", "$0Das Item in deiner Hand wurde $2repariert$0."),
  REPAIR_HAND_NONE("repair.repaired.hand.none", "$0Das Item in deiner Hand konnte $4nicht $0repariert werden!"),
  REPAIR_HAND_EMPTY("repair.repaired.hand.empty", "$0Du hältst $4kein $0Item in der Hand!"),
  REPAIR_INV_SUCCESS("repair.repaired.inv.success", "$0Dein $3ganzes Inventar $0wurde repariert."),
  REPAIR_INV_NONE("repair.repaired.inv.none", "$4Kein Item $0in deinem Inventar konnte $4repariert $0werden!"),

  //=========================================================================//
  //                               Kick Command                              //
  //=========================================================================//

  KICK_DEFAULT_REASON("kick.default_reason", "$0Es wurde $3kein Grund $0angegeben."),
  KICK_TRIEDSELF("kick.tried_self", "$0Du kannst dich $4nicht selbst $0kicken!"),
  KICK_UNKICKABLE("kick.unkickable", "$0Der Spieler $4{{target}} $0kann $4nicht $0gekickt werden!"),
  KICK_KICKED_TARGET(
    "kick.kicked.target",
    "$0Der Spieler $3{{target}} $0wurde $0vom Server geworfen.",
    "$0Ausführer: $3{{issuer}}",
    "$0Grund: $3{{reason}}"
  ),
  KICK_KICKED_ALL(
    "kick.kicked.all",
    "$3Alle Spieler $0wurden vom Server geworfen.",
    "$0Ausführer: $3{{issuer}}",
    "$0Grund: $3{{reason}}"
  ),
  KICK_SCREEN(
    "kick.screen",
    "$3&lBlvckBytes.Dev",
    "&6Du wurdest vom Server geworfen!",
    " ",
    "$2Grund: $0{{reason}}",
    "$2Von: $0{{issuer}}"
  ),

  //=========================================================================//
  //                             GiveHand Command                            //
  //=========================================================================//

  GIVEHAND_NOITEM("givehand.noitem", "$0Du hast $4kein Item $0zum vergeben in der Hand!"),
  GIVEHAND_SPECIFIC_SENDER("givehand.specific.sender", "$0Du hast das Item $3{{item_name}} $0an $3{{receiver}} $0gegeben!"),
  GIVEHAND_SPECIFIC_RECEIVER("givehand.specific.receiver", "$0Du hast das Item $3{{item_name}} $0von $3{{issuer}} $0bekommen!"),
  GIVEHAND_ALL_SENDER("givehand.all.sender", "$0Du hast das Item $3{{item_name}} $0an $3alle $0verteilt!"),
  GIVEHAND_ALL_RECEIVER("givehand.all.receiver", "$3{{issuer}} $0hat das Item $3{{item_name}} $0an $3alle $0verteilt!"),

  //=========================================================================//
  //                             LongChat Command                            //
  //=========================================================================//

  LONGCHAT_INIT("longchat.init", "$0Schreibe den Text in das $3Buch $0und klicke auf $3Fertig$0!"),
  LONGCHAT_CANCELLED("longchat.cancelled", "$0Du hast die Texteingabe $4abgebrochen$0!"),
  LONGCHAT_LENGTH_EXCEEDED("longchat.length_exceeded", "$0Die Eingabe ist länger als $4{{max_len}}$0: "),
  LONGCHAT_PREVIEW(
    "longchat.preview",
    "$3Vorschau $0der Nachricht:",
    "$0{{message}}"
  ),
  LONGCHAT_CONFIRM("longchat.confirm", "$0Eingabe $3absenden$0? "),

  //=========================================================================//
  //                             SignEdit Command                            //
  //=========================================================================//

  SIGNEDIT_NOSIGN("signedit.nosign", "$0Du hast aktuell $4kein $0Schild auf deinem $4Fadenkreuz$0!"),
  SIGNEDIT_NOBUILD("signedit.nobuild", "$0Du kannst in diesem Gebiet $4nicht bauen$0!"),
  SIGNEDIT_IS_PSIGN("signedit.is_psign", "$0Dieses Schild wird bereits als $4PSign verwaltet$0!"),
  SIGNEDIT_CANCELLED("signedit.cancelled", "$4Die Bearbeitung wurde abgebrochen!"),

  //=========================================================================//
  //                              Color Command                              //
  //=========================================================================//

  COLOR_LISTING("color_listing", "$0Verfügbare Farben: {{colors}}"),

  //=========================================================================//
  //                             ClearChat Command                           //
  //=========================================================================//

  CLEARCHAT_SELF("clearchat.self", "$0Du hast deinen Chat $3geleert$0!"),
  CLEARCHAT_GLOBAL("clearchat.global", "$0Der Spieler $3{{issuer}} $0hat den Chat $3geleert$0!"),

  //=========================================================================//
  //                              MSG, R Command                             //
  //=========================================================================//

  MSG_SENDER("msg.sender", "$1($2Du $0-> $2{{receiver}}$1)$0: {{message}}"),
  MSG_RECEIVER("msg.receiver", "$1($2{{sender}}$1 -> $2Dir$1)$0: {{message}}"),
  MSG_NO_PARTNER("msg.no_partner", "$4Du hast keinen aktiven Nachrichten-Partner!"),
  MSG_DISABLED_SELF("msg.disabled.self", "$0Du hast deine privaten Nachrichten $3deaktiviert$0!"),
  MSG_DISABLED_OTHERS("msg.disabled.others", "$3{{receiver}} $0hat privaten Nachrichten $3deaktiviert$0!"),
  MSG_SELF("msg.self", "$4Du kannst dir selbst keine Nachrichten schreiben!"),

  //=========================================================================//
  //                              PSIGN Command                              //
  //=========================================================================//

  PSIGN_NOSIGN("psign.nosign", "$0Du hast aktuell $4kein $0Schild auf deinem $4Fadenkreuz$0!"),
  PSIGN_LOC_NOSIGN("psign.loc_nosign", "$0Bei der Position $4{{location}} $0befindet sich $4kein Schild$0!"),
  PSIGN_NOBUILD("psign.nobuild", "$0Du kannst in diesem Gebiet $4nicht bauen$0!"),
  PSIGN_LOC_NOBUILD("psign.loc_nobuild", "$0Du kannst bei $4{{location}} nicht bauen$0!"),
  PSIGN_NOT_EXISTING("psign.not_existing", "$0Dieses Schild ist $4kein $0verwaltetes PSign!"),
  PSIGN_DELETED("psign.deleted", "$0Das Schild wird nun $3nicht mehr $0verwaltet."),
  PSIGN_EXISTS("psign.exists", "$0Das Schild ist $4bereits $0ein verwaltetes PSign!"),
  PSIGN_INVALID_LINEID("psign.invalid_lineid", "$0Die LineID muss eine Zahl zwischen $41-4 $0sein!"),
  PSIGN_CREATED("psign.created", "$0Das Schild $3ist nun $0ein verwaltetes PSign."),
  PSIGN_UPDATED("psign.updated", "$0Die Zeile $3{{line_id}} $0wurde geändert."),
  PSIGN_LISTLINES(
    "psign.listlines",
    "$0Schild bei $3{{location}}",
    "$0Erstellt von: $3{{creator}}",
    "$0Erstellt am: $3{{created_at}}",
    "$0Geändert von: $3{{last_editor}}",
    "$0Geändert am: $3{{updated_at}}",
    "$0Zeilen:",
    "$01: {{line1}}",
    "$02: {{line2}}",
    "$03: {{line3}}",
    "$04: {{line4}}"
  ),
  PSIGN_CANNOT_BREAK("psign.cannot_break", "$0Aktive PSigns können $4nicht abgebaut $0werden!"),
  PSIGN_MOVED("psign.moved", "$0Das Schild wurde zu deinem Fadenkreuz $3bewegt$0."),
  PSIGN_MOVE_OCCUPIED("psign.move.occupied", "$0Das Zielschild $4wird bereits $0als PSign verwaltet!"),
  PSIGN_MOVE_NOT_EXISTING("psign.move.not_existing", "$0Das Schild $4{{location}} $0ist kein PSign!"),

  //=========================================================================//
  //                              IGNORE Command                             //
  //=========================================================================//

  IGNORE_CHAT_DISABLED("ignore.chat.disabled", "$0Du empfängst nun $3Chat Nachrichten $0von $3{{target}}$0."),
  IGNORE_CHAT_ENABLED("ignore.chat.enabled", "$0Du empfängst nun $3keine Chat Nachrichten $0von $3{{target}} $0mehr."),
  IGNORE_MSG_DISABLED("ignore.msg.disabled", "$0Du empfängst nun $3private Nachrichten $0von $3{{target}}$0."),
  IGNORE_MSG_ENABLED("ignore.msg.enabled", "$0Du empfängst nun $3keine privaten Nachrichten $0von $3{{target}} $0mehr."),
  IGNORE_SELF("ignore.self", "$0Du kannst dich nicht $4selbst $0ignorieren!"),
  IGNORE_MSG_IGNORED("ignore.msg.ignored", "$4{{target}} $0ignoriert dich über $4MSG$0."),
  IGNORE_MSG_IGNORING("ignore.msg.ignoring", "$0Du ignorierst den Spieler $4{{target}} $0über $4MSG$0."),

  //=========================================================================//
  //                               FLY Command                               //
  //=========================================================================//

  FLY_ENABLED_OTHER("fly.enabled_other", "$2{{name}} $0kann $2nun $0fliegen!"),
  FLY_DISABLED_OTHER("fly.disabled_other", "$2{{name}} $0kann $2nicht mehr $0fliegen!"),
  FLY_ENABLED("fly.enabled", "$0Du kannst $2num $0fliegen!"),
  FLY_DISABLED("fly.disabled", "$0Du kannst $2nicht mehr $0fliegen!"),
  FLY_REVOKED("fly.revoked", "$0Du hast soeben deine $4Flugrechte $0verloren!"),

  //=========================================================================//
  //                               GIVE Command                              //
  //=========================================================================//

  GIVE_INVALID_ITEM("give.invalid_item", "$4Das Item {{material}} existiert nicht!"),
  GIVE_SELF("give.self", "$0Du hast dir $2{{amount}}x {{material}} $0gegeben."),
  GIVE_SENDER("give.sender", "$0Dem Spieler $2{{target}} $0wurden $2{{amount}}x {{material}} $0gegeben."),
  GIVE_RECEIVER("give.receiver", "$0Dir wurden $2{{amount}}x {{material}} $0von $2{{issuer}} $0gegeben."),
  GIVE_ALL_SENDER("give.all.sender", "$0Du hast $2{{amount}}x {{material}} $0an $2alle $0verteilt."),
  GIVE_ALL_RECEIVER("give.all.receiver", "$2{{issuer}} $0hat $2{{amount}}x {{material}} $0an $2alle $0verteilt."),
  GIVE_DROPPED("give.dropped", "$4Es wurden {{num_dropped}} Items fallen gelassen!"),

  //=========================================================================//
  //                              VANISH Command                             //
  //=========================================================================//

  VANISH_HIDDEN("vanish.hidden", "$0Du bist nun $3unsichtbar$0."),
  VANISH_SHOWN("vanish.shown", "$0Du bist $3nicht mehr $0unsichtbar."),
  VANISH_SUFFIX("vanish.suffix", " $3&lV"),

  //=========================================================================//
  //                                HAT Command                              //
  //=========================================================================//

  HAT_NO_ITEM("hat.no_item", "$0Du hältst $3kein $0Item in der $3Hand$0!"),
  HAT_UNDRESSED("hat.undressed", "$0Du hast deinen Hut $3ausgezogen$0."),
  HAT_DRESSED("hat.dressed", "$0Du trägst nun das Item $3{{item}} $0als Hut."),
  HAT_UNWEARABLE("hat.unwearable", "$0Das Item $3{{item}} $0ist $3nicht tragbar$0!"),

  //=========================================================================//
  //                              INJECT Command                             //
  //=========================================================================//

  INJECT_INJECTED("inject.injected", "$0Die Pakete von $2{{target}} $0werden $2nun $0abgefangen."),
  INJECT_UNINJECTED("inject.uninjected", "$0Die Pakete von $2{{target}} $0werden nun $2nicht mehr $0abgefangen."),
  INJECT_EVENT("inject.event", "$3{{direction}}$1 -> &r{{object}}"),
  INJECT_EVENT_COLOR_OTHER("inject.event_color.other", "$0"),
  INJECT_EVENT_COLOR_VALUES("inject.event_color.values", "$2"),
  INJECT_INVALID_REGEX("inject.invalid_regex", "$4Das Regex \"{{regex}}\" ist ungültig!"),

  //=========================================================================//
  //                          ClearInventory Command                         //
  //=========================================================================//

  CLEARINVENTORY_CONFIRM_SELF("clearinventory.confirm.self", "$0Dein Inventar $3leeren$0?: "),
  CLEARINVENTORY_CONFIRM_OTHERS("clearinventory.confirm.others", "$3{{target}}'s $0Inventar $3leeren$0?: "),
  CLEARINVENTORY_CLEARED_SELF("clearinventory.cleared.self", "$0Dein Inventar wurde $3geleert$0."),
  CLEARINVENTORY_CLEARED_SENDER("clearinventory.cleared.sender", "$0Das Inventar von $3{{target}} $0wurde $3geleert$0."),
  CLEARINVENTORY_CLEARED_TARGET("clearinventory.cleared.target", "$3{{issuer}} $0hat dein Inventar $3geleert$0."),
  CLEARINVENTORY_CANCELLED("clearinventory.cancelled", "$0Du hast die Leerung $4abgebrochen$0!"),

  //=========================================================================//
  //                             MsgToggle Command                           //
  //=========================================================================//

  MSGTOGGLE_DISABLED("msgtoggle.disabled", "$0Du empfängst nun $3keine $0privaten Nachrichten mehr."),
  MSGTOGGLE_ENABLED("msgtoggle.enabled", "$0Du empfängst nun $3wieder $0privaten Nachrichten."),

  //=========================================================================//
  //                            Scoreboard Command                           //
  //=========================================================================//

  SCOREBOARD_HIDDEN("scoreboard.hidden", "$0Dein Scoreboard wurde $3ausgeblendet$0."),
  SCOREBOARD_SHOWN("scoreboard.shown", "$0Dein Scoreboard wurde $3eingeblendet$0."),

  //=========================================================================//
  //                              PTime Command                              //
  //=========================================================================//

  PTIME_SET("ptime.set.self", "$0Du hast $3deine $0Clientzeit auf $3{{time}} $0gesetzt."),
  PTIME_RESET("ptime.reset.self", "$0Du hast $3deine $0Clientzeit $3zurückgesetzt$0."),
  PTIME_SET_OTHERS_SENDER("ptime.set.others.sender", "$0Du hast die Clientzeit von $3{{target}} $0auf $3{{time}} $0gesetzt."),
  PTIME_SET_OTHERS_RECEIVER("ptime.set.others.receiver", "$0Deine Clientzeit wurde von $3{{issuer}} $0auf $3{{time}} $0gesetzt."),
  PTIME_RESET_OTHERS_SENDER("ptime.reset.others.sender", "$0Du hast die Clientzeit von $3{{target}} $3zurückgesetzt$0."),
  PTIME_RESET_OTHERS_RECEIVER("ptime.reset.others.receiver", "$0Deine Clientzeit wurde von $3{{issuer}} $3zurückgesetzt$0."),

  //=========================================================================//
  //                            PWeather Command                             //
  //=========================================================================//

  PWEATHER_SET("pweather.set.self", "$0Du hast $3dein $0Wetter auf $3{{weather}} $0gesetzt."),
  PWEATHER_RESET("pweather.reset.self", "$0Du hast $3dein $0Wetter $3zurückgesetzt$0."),
  PWEATHER_SET_OTHERS_SENDER("pweather.set.others.sender", "$0Du hast das Wetter von $3{{target}} $0auf $3{{weather}} $0gesetzt."),
  PWEATHER_SET_OTHERS_RECEIVER("pweather.set.others.receiver", "$0Dein Wetter wurde von $3{{issuer}} $0auf $3{{weather}} $0gesetzt."),
  PWEATHER_RESET_OTHERS_SENDER("pweather.reset.others.sender", "$0Du hast das Wetter von $3{{target}} $3zurückgesetzt$0."),
  PWEATHER_RESET_OTHERS_RECEIVER("pweather.reset.others.receiver", "$0Dein Wetter wurde von $3{{issuer}} $3zurückgesetzt$0."),

  //=========================================================================//
  //                             Weather Command                             //
  //=========================================================================//

  WEATHER_SET("weather_set", "$0Der Spieler $3{{issuer}} $0hat das Wetter auf $3{{weather}} $0gesetzt."),

  //=========================================================================//
  //                               Time Command                              //
  //=========================================================================//

  TIME_SET("time_set", "$0Der Spieler $3{{issuer}} $0hat die Zeit auf $3{{time}} $0gesetzt."),

  //=========================================================================//
  //                             TOGGLECHAT Command                          //
  //=========================================================================//

  TOGGLECHAT_ENABLED("togglechat.enabled", "$0Du empfängst nun $3wieder $0Chat-Nachrichten von Spielern."),
  TOGGLECHAT_DISABLED("togglechat.disabled", "$0Du empfängst nun $3keine $0Chat-Nachrichten von Spielern mehr."),

  //=========================================================================//
  //                                 KIT Command                             //
  //=========================================================================//

  KIT_NOT_EXISTING("kit.not_existing", "$0Es existiert kein Kit namens $4{{name}}$0!"),
  KIT_NO_ITEM("kit.no_item", "$0Du hältst $4kein Item $0in der Hand!"),
  KIT_ITEM_SET("kit.item_set", "$0Das Item des Kits $3{{name}} $0wurde auf $3{{item}} $0geändert."),
  KIT_NO_ITEMS("kit.no_items", "$0Du hast $4keine Items $0im Inventar!"),
  KIT_CREATED("kit.created", "$0Das Kit $3{{name}} $0wurde mit $3{{num_items}} Items $0erstellt."),
  KIT_COOLDOWN("kit.cooldown", "$0Du kannst das Kit $4{{name}} $0erst in $4{{duration}} $0erneut anfordern."),
  KIT_DELETED("kit.deleted", "$0Das Kit $3{{name}} $0wurde $3gelöscht$0."),
  KIT_CONSUMED_SELF("kit.consumed.self", "$0Du hast das Kit $3{{name}} $0erhalten."),
  KIT_CONSUMED_DROPPED("kit.consumed.dropped", "$0Es wurden $4{{num_dropped}} Items $0fallen gelassen!"),
  KIT_CONSUMED_OTHERS_SENDER("kit.consumed.others.sender", "$0Du hast das Kit $3{{name}} $0an $3{{target}} $0gesendet."),
  KIT_CONSUMED_OTHERS_RECEIVER("kit.consumed.others.receiver", "$0Du hast das Kit $3{{name}} $0von $3{{issuer}} $0erhalten."),
  KIT_OVERWRITE_PREFIX(
    "kit.overwrite.prefix",
    "$0Das Kit $3{{name}} $0existiert bereits.",
    "Soll dieses $3überschrieben $0werden?: "
  ),
  KIT_OVERWRITE_CANCELLED("kit.overwrite.cancelled", "$0Die Erstellung wurde $4abgebrochen$0."),
  KIT_OVERWRITE_SAVED("kit.overwrite.saved", "$0Du hast das Kit $3{{name}} $0mit $3{{num_items}} Items $0überschrieben."),
  KIT_LIST_PREFIX("kit.list.prefix", "$0Verfügbare Kits ($3{{count}}$0): "),
  KIT_LIST_NO_ITEMS("kit.list.no_items", "$4Keine Kits verfügbar"),
  KIT_LIST_ITEM_FORMAT("kit.list.item_format", "$3{{name}}$1"),
  KIT_LIST_HOVER(
    "kit.list.hover",
    "$0Erstellt am: $3{{created_at}}",
    "$0Zuletzt geändert: $3{{updated_at}}",
    "$0Erstellt von: $3{{creator}}",
    "$0Items: $3{{num_items}}",
    "$0Verfügbar in: $3{{cooldown_dur}}"
  ),

  //=========================================================================//
  //                            IMAGEFRAMES Command                          //
  //=========================================================================//

  COMMAND_IMAGEFRAMES_LIST_NONE("imageframes.none", "$4Keine Image-Frames gefunden"),
  COMMAND_IMAGEFRAMES_LIST_PREFIX("imageframes.header", "$0Image-Frames im Radius von $3{{radius}} $0Blöcken: "),
  COMMAND_IMAGEFRAMES_LIST_FORMAT("imageframes.list.format", "$3{{name}}$1{{sep}}"),
  COMMAND_IMAGEFRAMES_LIST_TELEPORTED("imageframes.teleported", "$0Du wurdest zum Image-Frame $3{{name}} $0teleportiert."),
  COMMAND_IMAGEFRAMES_LIST_HOVER_CREATORS_FORMAT("imageframes.list.hover.creators_format", "$3{{creator}}$1"),
  COMMAND_IMAGEFRAMES_LIST_HOVER_TEXT(
    "imageframes.list.hover.text",
    "$0Erstellt am: $3{{created_at}}",
    "$0Zuletzt geändert: $3{{updated_at}}",
    "$0Erstellt von: $3{{creator}}",
    "$0Rahmen: $3{{num_members}}",
    "$0Abmessungen: $3{{dimensions}}",
    "$0Typ: $3{{type}}",
    "$0Resource: $3{{resource}}",
    "$0Position: $3{{location}}",
    "$0Distanz: $3{{distance}} Blöcke"
  ),

  //=========================================================================//
  //                              HOLOS Command                              //
  //=========================================================================//

  COMMAND_HOLOS_LIST_NONE("holos.none", "$4Keine Hologramme gefunden"),
  COMMAND_HOLOS_LIST_TELEPORTED("holos.teleported", "$0Du wurdest zum Hologramm $3{{name}} $0teleportiert."),
  COMMAND_HOLOS_LIST_PREFIX("holos.header", "$0Hologramme im Radius von $3{{radius}} $0Blöcken: "),
  COMMAND_HOLOS_LIST_FORMAT("holos.list.format", "$3{{name}}$1{{sep}}"),
  COMMAND_HOLOS_LIST_HOVER_CREATORS_FORMAT("holos.list.hover.creators_format", "$3{{creator}}$1"),
  COMMAND_HOLOS_LIST_HOVER_TEXT(
    "holos.list.hover.text",
    "$0Erstellt am: $3{{created_at}}",
    "$0Ersteller: $3{{creators}}",
    "$0Zeilen: $3{{num_lines}}",
    "$0Position: $3{{location}}",
    "$0Distanz: $3{{distance}} Blöcke"
  ),

  //=========================================================================//
  //                             HOLOSORT Command                            //
  //=========================================================================//

  COMMAND_HOLOSORT_NOT_EXISTING("holosort.not_existing", "$0Es existiert kein Hologramm namens $4{{name}}$0!"),
  COMMAND_HOLOSORT_MISSING_IDS("holosort.missing_ids", "$0Es fehlen $4{{num_missing}} IDs $0in der Sequenz!"),
  COMMAND_HOLOSORT_INVALID_ID("holosort.invalid_id", "$0Die ID $4{{invalid_id}} $0der Sequenz ist $4ungültig$0!"),
  COMMAND_HOLOSORT_SORTED("holosort.sorted", "$0Die Zeilen des Hologramms $3{{name}} $0wurden $3sortiert$0."),

  //=========================================================================//
  //                               HOLO Command                              //
  //=========================================================================//

  COMMAND_HOLO_NOT_EXISTING("holo.not_existing", "$0Es existiert kein Hologramm namens $4{{name}}$0!"),
  COMMAND_HOLO_DELETED("holo.deleted", "$0Das Hologramm $3{{name}} $0wurde $3gelöscht$0."),
  COMMAND_HOLO_MOVED("holo.moved", "$0Das Hologramm $3{{name}} $0wurde zu dir $3bewegt$0."),
  COMMAND_HOLO_LINE_ADDED("holo.line_added", "$0Die $3Zeile $0wurde zum Hologramm $3{{name}} $0hinzugefügt."),
  COMMAND_HOLO_LINE_UPDATED("holo.line_updated", "$0Die $3Zeile {{index}} $0vom Hologramm $3{{name}} $0wurde geändert."),
  COMMAND_HOLO_INVALID_INDEX("holo.invalid_index", "$0Der Index $4{{index}} $0ist $4keine $0verfügbare Zeile!"),
  COMMAND_HOLO_LINE_DISAPPEARED("holo.line_disappeared", "$0Die Zeile $4{{index}} $0wurde in der Zwischenzeit $4gelöscht$0!"),
  COMMAND_HOLO_LINE_DELETED("holo.line_deleted", "$0Die $3Zeile {{index}} $0vom Hologramm $3{{name}} $0wurde gelöscht."),
  COMMAND_HOLO_LINES_HEADER("holo.lines_header", "$0Zeilen vom Hologramm $3{{name}}$0:"),
  COMMAND_HOLO_LINES_LINE_FORMAT("holo.line_format", "$0[$3{{index}}$0]: {{text}}"),

  //=========================================================================//
  //                              HOMES Command                              //
  //=========================================================================//

  HOMES_LIST_PREFIX_SELF("homes.list.prefix.self", "$0Deine Homes ($3{{count}}$0): "),
  HOMES_LIST_PREFIX_OTHERS("homes.list.prefix.others", "$3{{target}}'s $0Homes ($3{{count}}$0): "),
  HOMES_LIST_NO_ITEMS("homes.list.no_items", "$4Keine Homes verfügbar"),
  HOMES_LIST_ITEM_FORMAT("homes.list.item_format", "$3{{name}}$1"),
  HOMES_LIST_HOVER(
    "homes.list.hover",
    "$0Erstellt am: $3{{created_at}}",
    "$0Zuletzt geändert: $3{{updated_at}}",
    "$0Welt: $3{{world}}",
    "$0Position: $3{{location}}"
  ),
  HOMES_NOT_FOUND("homes.not_found", "$0Es existiert kein Home mit dem Namen $4{{name}}$0!"),
  HOMES_TELEPORTED_SELF("homes.teleported.self", "$0Du hast dich zum Home $3{{name}} $0teleportiert!"),
  HOMES_TELEPORTED_OTHERS("homes.teleported.others", "$0Du hast dich zum Home $3{{name}} $0von $3{{owner}} $0teleportiert!"),
  HOMES_EXISTING("homes.existing", "$0Es existiert bereits ein Home mit dem Namen $4{{name}}$0!"),
  HOMES_DELETED("homes.deleted", "$0Das Home $3{{name}} $0wurde gelöscht."),
  HOMES_CREATED("homes.created", "$0Das Home $3{{name}} $0wurde erstellt."),
  HOMES_MOVED("homes.moved", "$0Das Home $3{{name}} $0wurde zu deiner Position $3bewegt$0."),
  HOMES_MAX_REACHED("homes.max_reached", "$0Du hast deine $4maximale Anzahl $0an Homes von $4{{num_max_homes}} $0erreicht!"),

  //=========================================================================//
  //                               WARP Command                              //
  //=========================================================================//

  WARP_NOT_EXISTING("warp.not_existing", "$0Es existiert kein Warp namens $4{{name}}$0!"),
  WARP_CREATED("warp.created", "$0Der Warp $3{{name}} $0wurde bei $3{{location}} $0erstellt."),
  WARP_DELETED("warp.deleted", "$0Der Warp $3{{name}} $0wurde $3gelöscht$0."),
  WARP_TELEPORTED("warp.teleported", "$0Du wurdest zum Warp $3{{name}} $0teleportiert."),
  WARP_OVERWRITE_PREFIX(
    "warp.overwrite.prefix",
    "$0Der Warp $3{{name}} $0existiert bereits.",
    "Soll dieser $3überschrieben $0werden?: "
  ),
  WARP_OVERWRITE_CANCELLED("warp.overwrite.cancelled", "$0Die Erstellung wurde $4abgebrochen$0."),
  WARP_OVERWRITE_SAVED("warp.overwrite.saved", "$0Du hast den Warp $3{{name}} $0überschrieben."),
  WARP_LIST_PREFIX("warp.list.prefix", "$0Verfügbare Warps ($3{{count}}$0): "),
  WARP_LIST_NO_ITEMS("warp.list.no_items", "$4Keine Warps verfügbar"),
  WARP_LIST_ITEM_FORMAT("warp.list.item_format", "$3{{name}}$1"),
  WARP_LIST_HOVER(
    "warp.list.hover",
    "$0Erstellt am: $3{{created_at}}",
    "$0Zuletzt geändert: $3{{updated_at}}",
    "$0Erstellt von: $3{{creator}}",
    "$0Welt: $3{{world}}",
    "$0Position: $3{{location}}"
  ),

  //=========================================================================//
  //                              SPAWN Command                              //
  //=========================================================================//

  SPAWN_NOT_SET("spawn.not_set", "$0Der Spawn-Punkt wurde $4noch nicht $0gesetzt!"),
  SPAWN_TELEPORTED("spawn.teleported", "$0Du wurdest zum $3Spawn $0teleportiert."),

  //=========================================================================//
  //                              GUI Generics                               //
  //=========================================================================//

  GUI_GENERICS_PAGING_NEXT_NAME("gui.generics.paging.next.name", "$0» $2Nächste Seite $0«"),
  GUI_GENERICS_PAGING_NEXT_LORE(
    "gui.generics.paging.next.lore",
    " ",
    "$0Navigiere zur nächsten Seite"
  ),
  GUI_GENERICS_PAGING_PREV_NAME("gui.generics.paging.prev.name", "$0» $2Vorherige Seite $0«"),
  GUI_GENERICS_PAGING_PREV_LORE(
    "gui.generics.paging.prev.lore",
    " ",
    "$0Navigiere zur vorherigen Seite"
  ),
  GUI_GENERICS_PAGING_INDICATOR_NAME("gui.generics.paging.indicator.name", "$0» $2Seite {{curr_page}}/{{num_pages}} $0«"),
  GUI_GENERICS_PAGING_INDICATOR_LORE(
    "gui.generics.paging.indicator.lore",
    " ",
    "$0Zeigt die aktuelle Seitenposition"
  ),
  GUI_GENERICS_NAV_BACK_NAME("gui.generics.nav.back.name", "$0» $2Zurück $0«"),
  GUI_GENERICS_NAV_BACK_LORE(
    "gui.generics.nav.back.lore",
    " ",
    "$0Zurück zum vorherigen Menu"
  ),

  //=========================================================================//
  //                                Kits GUI                                 //
  //=========================================================================//

  GUI_KITS_TITLE("gui.kits.title", "$0Kitmenu von $2{{viewer}}"),
  GUI_KITS_KIT_NAME("gui.kits.kit_name", "$0» $2{{name}}"),
  GUI_KITS_KIT_LORE(
    "gui.kits.kit_lore",
    " ",
    "$0Anzahl Items: $2{{num_items}}",
    "$0Cooldown verbleibend: $2{{cooldown}}",
    " ",
    "$0&oRechtsklick zur Vorschau"
  ),
  GUI_KIT_CONTENT_TITLE("gui.kit_content.title", "$0Kitvorschau $2{{name}}"),
  GUI_KIT_CONTENT_CONTENT_NAME("gui.kit_content.content.name", "$2{{hr_type}}"),
  GUI_KIT_CONTENT_CONTENT_LORE(
    "gui.kit_content.content.lore",
    " ",
    "$0Dieses Item erhältst du, wenn du",
    "$0das Kit $2{{name}} $0anforderst."
  ),

  //=========================================================================//
  //                            Generic Messages                             //
  //=========================================================================//
  GENERIC_JOINED("generic.joined", "$0Der Spieler $2{{name}} $0ist dem Server $2beigetreten$0!"),
  GENERIC_QUIT("generic.quit", "$0Der Spieler $2{{name}} $0hat den Server $2verlassen$0!"),

  //=========================================================================//
  //                              Error Messages                             //
  //=========================================================================//

  ERR_INTERNAL("errors.internal", "$5Es trat ein interner Fehler auf!"),
  ERR_INTPARSE("errors.intparse", "$4Die Eingabe $5{{number}} $4ist keine Ganzzahl!"),
  ERR_UUIDPARSE("errors.uuidparse", "$4Die Eingabe $5{{uuid}} $4ist keine UUID!"),
  ERR_FLOATPARSE("errors.floatparse", "$4Die Eingabe $5{{duration}} $4ist keine Kommazahl!"),
  ERR_DURATIONPARSE("errors.durationparse", "$4Die Eingabe $5{{number}} $4ist keine gültige Dauer (<Anzahl><y/m/w/d/h/m/s>!"),
  ERR_PERMISSION("errors.permission", "$4Dir fehlt das Recht $5{{permission}} $4um diesen Befehl ausführen zu können!"),
  ERR_OPTIONPARSE("errors.optionparse", "$4Die Eingabe $5{{option}} $4ist keine gültige Wahl!"),
  ERR_NOT_ONLINE("errors.not_online", "$4Der Spieler $5{{player}} $4ist nicht online!"),
  ERR_COOLDOWN("errors.cooldown", "$0Bitte warte noch $4{{duration}} $0bevor du diese Aktion erneut benutzt!"),
  ERR_PLAYER_UNKNOWN("errors.player_unknown", "$4Der Spieler $5{{player}} $4ist nicht Teil des Servers!"),
  ERR_USAGE_PREFIX("errors.usage_prefix", "$0Benutzung: "),
  ERR_USAGE_COLOR_MANDATORY("errors.usage_color.mandatory", "$2"),
  ERR_USAGE_COLOR_OPTIONAL("errors.usage_color.optional", "&b"),
  ERR_USAGE_COLOR_BRACKETS("errors.usage_color.brackets", "$1"),
  ERR_USAGE_COLOR_OTHER("errors.usage_color.other", "$0"),
  ERR_USAGE_COLOR_FOCUS("errors.usage_color.focus", "$5&l"),
  ERR_NOT_A_PLAYER("errors.not_a_player", "$4Dieser Befehl ist nur für $5Spieler $4zugänglich!"),

  //=========================================================================//
  //                              Database Config                            //
  //=========================================================================//

  DB_HOST("db.host", "localhost"),
  DB_PORT("db.port", "3306"),
  DB_DATABASE("db.database", "blvcksys"),
  DB_USERNAME("db.username", "root"),
  DB_PASSWORD("db.password", "mysql2001")
  ;

  private final String key;
  private final String value;

  ConfigKey(final String key, final String ...prefix) {
    this.key = key;
    this.value = String.join("\n", prefix);
  }

  @Override
  public String toString() {
    return this.key;
  }

  public String getDefaultValue() {
    return this.value;
  }
}
