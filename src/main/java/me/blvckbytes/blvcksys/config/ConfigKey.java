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
    "belowname_text",
    "&a◎ $1| &r{{hearts}} &c❤"
  ),

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
  //                                Playerlist                               //
  //=========================================================================//

  PLAYERLIST_TEXT(
    "playerlist.text",
    "$0» $1❘ &e&lDevelopment Server",
    "$0» $1❘ ➟ &aYour $1× &dAdvertisement $1× &6Placed $1× &9Here"
  ),
  PLAYERLIST_HOVER(
    "playerlist.hover",
    "$2This server has an",
    "$2awesome hover applied!"
  ),
  PLAYERLIST_ONLINE("playerlist.online", ""),

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
