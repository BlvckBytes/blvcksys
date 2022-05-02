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
  CHATBUTTONS_HOVER("chatbuttons.hover", "$0Klicken um auszuführen"),
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
  //                                   Chat                                  //
  //=========================================================================//

  CHAT_FORMAT("chat_format", "{{prefix}}{{name}}$0: {{message}}"),

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
    "$4Soon",
    " ",
    "Freunde:",
    "$4Soon",
    " ",
    "Dein Name:",
    "$2{{name}}"
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
  //                            GRINDSTONE Command                           //
  //=========================================================================//

  GRINDSTONE_GUINAME("grindstone.guiname", "$3{{owner}}'s Schleifstein"),

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
  //                                TPA Command                              //
  //=========================================================================//

  TPA_STILL_PENDING("tpa.still_pending", "$0Es steht bereits eine $4aktive Teleport-Anfrage $0für $4{{target}} $0aus!"),
  TPA_EXPIRED_SENDER("tpa.expired.sender", "$0Deine $4ausgehende $0Teleport-Anfrage an $4{{target}} $0ist abgelaufen!"),
  TPA_EXPIRED_RECEIVER("tpa.expired.receiver", "$0Eine $4eingehende $0Teleport-Anfrage von $4{{sender}} $0ist abgelaufen!"),
  TPA_CANCELLED_SENDER("tpa.cancelled.sender", "$0Deine Anfrage an $4{{target}} $0wurde $4abgebrochen$0!"),
  TPA_CANCELLED_RECEIVER("tpa.cancelled.receiver", "$0Die Anfrage von $4{{sender}} $0wurde $4abgebrochen$0!"),
  TPA_DENIED_SENDER("tpa.denied.sender", "$0Deine Anfrage an $4{{target}} $0wurde $4abgelehnt$0!"),
  TPA_DENIED_RECEIVER("tpa.denied.receiver", "$0Die Anfrage von $4{{sender}} $0wurde $4abgelehnt$0!"),
  TPA_MOVED_SENDER("tpa.moved.sender", "$0Du hast dich $4bewegt$0, deine Anfrage an $4{{target}} $0wurde abgebrochen!"),
  TPA_MOVED_RECEIVER("tpa.moved.receiver", "$4{{sender}} $0hat sich bewegt, die Anfrage wurde $4abgebrochen$0!"),
  TPA_TELEPORTED_SENDER("tpa.teleported.sender", "$0Du wurdest zu $2{{target}} $0teleportiert."),
  TPA_TELEPORTED_RECEIVER("tpa.teleported.receiver", "$2{{sender}} $0wurde zu dir teleportiert."),
  TPA_SENDER_QUIT("tpa.sender_quit", "$0Der Sender $4{{sender}} $0einer ausstehenden Teleport-Anfrage hat den Server $4verlassen$0!"),
  TPA_RECEIVER_QUIT("tpa.receiver_quit", "$0Der Empfänger $4{{target}} $0einer ausstehenden Teleport-Anfrage hat den Server $4verlassen$0!"),
  TPA_SENT("tpa.sent", "$0Deine Anfrage an $2{{target}} $0wurde versendet!"),
  TPA_SELF("tpa.self", "$0Du kannst $4dir selbst $0keine Anfragen senden!"),
  TPA_NONE_RECEIVED("tpa.none.received", "$0Du hast $4keine $0aktive Anfrage von $4{{sender}}$0!"),
  TPA_NONE_SENT("tpa.none.sent", "$0Du hast noch keine Anfrage an $4{{target}} $0versendet!"),
  TPA_ACCEPTED_SENDER(
    "tpa.accepted.sender",
    "$0Deine Anfrage an $4{{target}} $0wurde $2angenommen$0!",
    "$0Bewege dich für $2{{seconds}}s $0nicht, um $2teleportiert $0zu werden."
  ),
  TPA_ACCEPTED_RECEIVER("tpa.accepted.receiver", "$0Die Anfrage von $4{{sender}} $0wurde $2angenommen$0!"),
  TPA_RECEIVED_PREFIX("tpa.received_prefix", "$2{{sender}} $0möchte sich zu dir teleportieren: "),

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
  MSG_SELF("msg.self", "$4Du kannst dir selbst keine Nachrichten schreiben!"),

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
  //                             Weather Command                             //
  //=========================================================================//

  WEATHER_SET("weather_set", "$0Der Spieler $3{{issuer}} $0hat das Wetter auf $3{{weather}} $0gesetzt."),

  //=========================================================================//
  //                               Time Command                              //
  //=========================================================================//

  TIME_SET("time_set", "$0Der Spieler $3{{issuer}} $0hat die Zeit auf $3{{time}} $0gesetzt."),

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
