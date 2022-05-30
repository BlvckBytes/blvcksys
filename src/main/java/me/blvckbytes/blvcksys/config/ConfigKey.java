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
  CHATBUTTONS_PROMPT_CANCELLED("chatbuttons.prompt.cancelled", "$0Du hast die Eingabeaufforderung $2abgebrochen$0."),
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
  //                                Death Messages                           //
  //=========================================================================//

  DEATH_MESSAGES_KILLED_VICTIM("death_messages.killed.victim", "$0Du wurdest von $2{{killer}} $0mit &c{{killer_health}} ❤ $0getötet!"),
  DEATH_MESSAGES_KILLED("death_messages.killed.all", "$2{{victim}} $0wurde von $2{{killer}} $0getötet!"),
  DEATH_MESSAGES_UNKNOWN("death_messages.unknown", "$2{{player}} $0starb durch einen $2unbekannten Grund$0!"),
  DEATH_MESSAGES_FALL("death_messages.fall", "$2{{player}} $0starb durch $2Fallschaden$0!"),
  DEATH_MESSAGES_FALLING_BLOCK("death_messages.falling_block", "$2{{player}} $0wurde vom Block $2{{block}} $0erschlagen!"),
  DEATH_MESSAGES_BLOCK_CONTACT("death_messages.block_contact", "$2{{player}} $0starb durch Kontakt mit $2{{block}}$0!"),
  DEATH_MESSAGES_IN_FIRE("death_messages.in_fire", "$2{{player}} $0verbrannte im $2Feuer$0!"),
  DEATH_MESSAGES_FIRE("death_messages.fire", "$2{{player}} $0verbrannte durch $2Feuer$0!"),
  DEATH_MESSAGES_DRAGON("death_messages.block_contact", "$2{{player}} $0starb durch einen $2Drachen$0!"),
  DEATH_MESSAGES_WALL("death_messages.wall", "$2{{player}} $0starb durch eine $2Wand$0!"),
  DEATH_MESSAGES_ENTITY("death_messages.entity_attack", "$2{{player}} $0starb durch $2{{entity}}$0!"),
  DEATH_MESSAGES_FREEZE("death_messages.freeze", "$2{{player}} $0ist $2erfroren$0!"),
  DEATH_MESSAGES_MAGMA("death_messages.magma", "$2{{player}} $0ist auf $2Magma $0verbrannt!"),
  DEATH_MESSAGES_LAVA("death_messages.lava", "$2{{player}} $0ist in $2Lava $0verbrannt!"),
  DEATH_MESSAGES_LIGHTNING("death_messages.lightning", "$2{{player}} $0ist in vom $2Blitz $0getroffen worden!"),
  DEATH_MESSAGES_DROWNED("death_messages.drowned", "$2{{player}} $0ist $2ertrunken$0!"),
  DEATH_MESSAGES_MAGIC("death_messages.magic", "$2{{player}} $0starb durch $2Magie$0!"),
  DEATH_MESSAGES_POISON("death_messages.poison", "$2{{player}} $0starb durch eine $2Vergiftung$0!"),
  DEATH_MESSAGES_TRAMPLED("death_messages.trampled", "$2{{player}} $0wurde zu Tode $2getrampelt$0!"),
  DEATH_MESSAGES_PROJECTILE("death_messages.projectile", "$2{{player}} $0wurde mit einem $2Projektil $0erschossen!"),
  DEATH_MESSAGES_STARVATION("death_messages.starvation", "$2{{player}} $0ist $2verhungert$0!"),
  DEATH_MESSAGES_SUICIDE("death_messages.starvation", "$2{{player}} $0hat $2Suizid $0begangen!"),
  DEATH_MESSAGES_THORNS("death_messages.thorns", "$2{{player}} $0ist durch die $2Dornen $0von $2{{entity}} $0gestorben!"),
  DEATH_MESSAGES_VOID("death_messages.void", "$2{{player}} $0fiel $2aus der Welt$0!"),
  DEATH_MESSAGES_WITHER("death_messages.wither", "$2{{player}} $0Starb durch den $2Wither-Effekt$0!"),
  DEATH_MESSAGES_SUFFOCATION("death_messages.suffocation", "$2{{player}} $0wurde durch den Block $2{{block}} $0erstickt!"),
  DEATH_MESSAGES_BLOCK_EXPLOSION("death_messages.block_explosion", "$2{{player}} $0wurde in die Luft $2gesprengt$0!"),

  //=========================================================================//
  //                           Server Settings COMMAND                       //
  //=========================================================================//

  SERVER_SETTINGS_GET("server_settings.get", "$0Wert der Einstellung $2{{setting}}$0: \"$2{{value}}$0\"."),
  SERVER_SETTINGS_SET("server_settings.set", "$0Neuer Wert der Einstellung $2{{setting}}$0: \"$2{{value}}$0\"."),

  //=========================================================================//
  //                             ItemEditor COMMAND                          //
  //=========================================================================//

  ITEMEDITOR_NO_ITEM("itemeditor.no_item", "$0Du hältst $4kein Item $0in der Hand!"),

  //=========================================================================//
  //                               Survey COMMAND                            //
  //=========================================================================//

  SURVEY_CANCELLED("survey.cancelled", "$2{{executor}} $0hat die aktuelle Umfrage $2vorzeitig abgebrochen$0!"),
  SURVEY_EXISTS("survey.exists", "$0Es $4existiert $0bereits eine aktive Umfrage!"),
  SURVEY_NONE("survey.none", "$0Es ist aktuell $4keine $0Umfrage aktiv!"),
  SURVEY_INVALID_DURATION("survey.invalid_duration", "$0Die Umfrage muss eine Mindestdauer von $4{{min_duration}} $0aufweisen!"),
  SURVEY_NO_ANSWERS("survey.no_answers", "$0Du hast $4keine $0Antwortmöglichkeiten angegeben!"),
  SURVEY_ONE_ANSWER("survey.one_answer", "$0Bitte gib mehr als $4eine $0Antwortmöglichkeit an!"),
  SURVEY_LAUNCHED(
    "survey.launched",
    "$0Es wurde eine neue $2Umfrage $0von $2{{creator}} $0gestartet ($2{{duration}}$0):",
    "$0Frage: \"$2{{question}}$0\"",
    "$0Gib deine Stimme mit $2/answer $0ab!"
  ),
  SURVEY_COMPLETE_HEAD(
    "survey.complete.head",
    "$0Die aktuelle Umfrage wurde $2ausgewertet$0:",
    "$0Frage: \"$2{{question}}$0\""
  ),
  SURVEY_COMPLETE_ANSWERS("survey.complete.answers", "$0\"$2{{answer}}$0\" mit $2{{votes}} Stimmen $0($2{{percent}}%$0)"),
  SURVEY_COMPLETE_TAIL("survey.complete.tail", "$0Stimmen total: $2{{votes_total}}"),
  SURVEY_REMAINING("survey.remaining", "$0Die Umfrage \"$2{{question}}$0\" wird in $2{{duration}} $0beendet! $2/answer"),
  SURVEY_NONE_ACTIVE("survey.none_active", "$0Es ist gerade $4keine $0Umfrage aktiv!"),
  SURVEY_VOTE_PLACED("survey.vote.placed", "$0Deine Stimme wurde auf \"$2{{answer}}$0\" $0platziert!"),
  SURVEY_VOTE_MOVED("survey.vote.moved", "$0Deine Stimme wurde auf \"$2{{answer}}$0\" $0geändert!"),
  SURVEY_VOTE_INVALID("survey.vote.invalid", "$0Diese Antwort ist $4ungültig$0!"),

  //=========================================================================//
  //                               MsgSpy COMMAND                            //
  //=========================================================================//

  MSGSPY_ENABLED("msgspy.enabled", "$0Du $2empfängst $0nun den privaten Nachrichtenverkehr von $2{{target}}$0."),
  MSGSPY_DISABLED("msgspy.disabled", "$0Du $2empfängst $2keinen $0privaten Nachrichtenverkehr von $2{{target}} $0mehr."),
  MSGSPY_SELF("msgspy.self", "$0Du kannst dich nicht $2selbst belauschen$0!"),
  MSGSPY_MESSAGE("msgspy.message", "&6&lSPY $1($2{{sender}}$1 -> $2{{receiver}}$1)$0: {{message}}"),

  //=========================================================================//
  //                              Damage Indicators                          //
  //=========================================================================//

  DAMAGE_INDICATORS_NORMAL(
    "damage_indicators.normal",
    "&c-{{damage}}❤"
  ),

  DAMAGE_INDICATORS_CRITICAL(
    "damage_indicators.critical",
    "&c-{{damage}}❤",
    "&4CRIT"
  ),

  //=========================================================================//
  //                               Kill Indicators                          //
  //=========================================================================//

  KILL_INDICATORS(
    "kill_indicators",
    "&5✝ &d{{victim}}",
    "&6+{{coins}} Coins"
  ),

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
    "$2{{playtime}}",
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
    "$0Onlinezeit: $3{{playtime}}",
    "$0Kills, Deaths, KD: $3{{kills}}$0/$3{{deaths}}$0/$3{{kd}}",
    "$0Coins: $3{{coins}}",
    "$0Warns gesamt: $3{{warns_total}}$0, davon aktiv: $3{{warns_active}}",
    "$0Mute: $3{{mute_duration}}$0, verbleibend: $3{{mute_remaining}}"
  ),

  //=========================================================================//
  //                             ENDERCHEST Command                          //
  //=========================================================================//

  ENDERCHEST_IN_COMBAT("enderchest.in_combat", "$0Du bist $4im Kampf $0und hast keinen Zugriff auf deine Enderchest!"),
  ENDERCHEST_LOCKED("enderchest.locked", "$0Dieser Slot wurde noch $4nicht freigeschalten$0!"),

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
  //                              CRATES Command                             //
  //=========================================================================//

  COMMAND_CRATES_LIST_NONE("crates.none", "$4Keine Crates gefunden"),
  COMMAND_CRATES_LIST_TELEPORTED("crates.teleported", "$0Du wurdest zur Crate $3{{name}} $0teleportiert."),
  COMMAND_CRATES_LIST_NO_LOC("crates.no_loc", "$0Die Crate $4{{name}} $0besitzt noch keine Position."),
  COMMAND_CRATES_LIST_PREFIX("crates.header", "$0Existierende Crates: "),
  COMMAND_CRATES_LIST_FORMAT("crates.list.format", "$3{{name}}$1{{sep}}"),
  COMMAND_CRATES_LIST_HOVER_CREATORS_FORMAT("crates.list.hover.creators_format", "$3{{creator}}$1"),
  COMMAND_CRATES_LIST_HOVER_TEXT(
    "crates.list.hover.text",
    "$0Erstellt am: $3{{created_at}}",
    "$0Ersteller: $3{{creators}}",
    "$0Items: $3{{num_items}}",
    "$0Position: $3{{location}}",
    "$0Distanz: $3{{distance}} Blöcke"
  ),

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
  //                              CRATE Command                              //
  //=========================================================================//

  COMMAND_CRATE_CREATED("crate.created", "$0Die Crate $3{{name}} $0wurde $3erstellt$0."),
  COMMAND_CRATE_NOITEM("crate.noitem", "$0Du hast $4kein Item $0zum setzen in der Hand!"),
  COMMAND_CRATE_NOCHEST("crate.nochest", "$0Du hast aktuell $4keine $0Truhe auf deinem $4Fadenkreuz$0!"),
  COMMAND_CRATE_NOBUILD("crate.nobuild", "$0Du kannst in diesem Gebiet $4nicht bauen$0!"),
  COMMAND_CRATE_NOT_EXISTING("crate.not_existing", "$0Es existiert keine Crate namens $4{{name}}$0!"),
  COMMAND_CRATE_EXISTS("crate.exists", "$0Es existiert bereits eine Crate namens $4{{name}}$0!"),
  COMMAND_CRATE_DELETED("crate.deleted", "$0Die Crate $3{{name}} $0wurde $3gelöscht$0."),
  COMMAND_CRATE_MOVED("crate.moved", "$0Die Crate $3{{name}} $0wurde zu $3{{location}} $0bewegt."),
  COMMAND_CRATE_LAYOUT_SET("crate.layout_set", "$0Das Layout der Crate $3{{name}} $0wurde auf $3{{layout}} $0gesetzt!"),
  COMMAND_CRATE_COLOR_SET("crate.color_set", "$0Die Effektfarbe der Crate $3{{name}} $0wurde auf $3{{color}} $0gesetzt!"),
  COMMAND_CRATE_ITEM_ADDED("crate.item_added", "$0Das $3Item $0wurde mit einer Wahrscheinlichkeit von $3{{probability}}% $0zur Crate $3{{name}} $0hinzugefügt."),
  COMMAND_CRATE_ITEM_INVALID_PROBABILITY("crate.invalid_probability", "$0Die Wahrscheinlichkeit muss größer als $40% $0und kleiner als $4100% $0sein!"),
  COMMAND_CRATE_ITEM_UPDATED_PROBABILITY("crate.updated_probability", "$0Die Wahrscheinlichkeit des Items $2{{item}} $0wurde auf $2{{probability}}% $0gesetzt."),
  COMMAND_CRATE_ITEM_UPDATED("crate.item_updated", "$0Das $3Item {{index}} $0der Crate $3{{name}} $0wurde geändert."),
  COMMAND_CRATE_INVALID_INDEX("crate.invalid_index", "$0Der Index $4{{index}} $0ist $4kein $0verfügbares Item!"),
  COMMAND_CRATE_ITEM_DISAPPEARED("crate.item_disappeared", "$0Das Item $4{{index}} $0wurde in der Zwischenzeit $4gelöscht$0!"),
  COMMAND_CRATE_ITEM_DELETED("crate.item_deleted", "$0Das $3Item {{item}} $0der Crate $3{{name}} $0wurde gelöscht."),

  //=========================================================================//
  //                            CRATEKEYS Command                            //
  //=========================================================================//

  COMMAND_CRATEKEYS_LIST_HEADER_SELF(
    "cratekeys.list.header.self",
    "$0Deine $2Crate-Keys$0:",
    "$1| $3Crate $1| $3Schlüssel $1|"
  ),
  COMMAND_CRATEKEYS_LIST_HEADER_OTHERS(
    "cratekeys.list.header.others",
    "$0Crate-Keys von $2{{target}}$0:",
    "$1| $3Crate $1| $3Schlüssel $1|"
  ),
  COMMAND_CRATEKEYS_LIST_KEY_FORMAT("cratekeys.list.key_format", "$1| $2{{crate}} $1| $2{{keys}} $1|"),
  COMMAND_CRATEKEYS_SET_SELF("cratekeys.set.self", "$0Dein Crate-Konto der Crate $3{{crate}} $0wurde auf $3{{keys}} Schlüssel $0gesetzt ($3{{delta}} &6✒$0)."),
  COMMAND_CRATEKEYS_SET_NOT_FOUND("cratekeys.set.not_found", "$0Die Crate $4{{crate}} $0existiert nicht!"),
  COMMAND_CRATEKEYS_SET_OTHERS_SENDER("cratekeys.set.others.sender", "$0Du hast das Crate-Konto der Crate $3{{crate}} $0von $3{{target}} $0auf $3{{keys}} Schlüssel $0gesetzt ($3{{delta}} &6✒$0)."),
  COMMAND_CRATEKEYS_SET_OTHERS_RECEIVER("cratekeys.set.others.receiver", "$0Dein Crate-Konto der Crate $3{{crate}} $0wurde von $3{{issuer}} $0auf $3{{keys}} Schlüssel $0gesetzt ($3{{delta}} &6✒$0)."),

  //=========================================================================//
  //                             CRATEPAY Command                            //
  //=========================================================================//

  CRATEPAY_INVALID_AMOUNT("cratepay.invalid_amount", "$0Die Schlüsselanzahl muss $4größer als 0 $0sein!"),
  CRATEPAY_TOO_MUCH("cratepay.too_much", "$0Du kannst maximal $4{{available}} Schlüssel $0für die Crate $4{{crate}} $0überweisen!"),
  CRATEPAY_SELF("cratepay.self", "$0Du kannst $4nicht $0an dich $4selbst $0überweisen!"),
  CRATEPAY_HAS_NONE("cratepay.has_none", "$0Du hast $4keine Schlüssel $0für die Crate $4{{crate}}$0!"),
  CRATEPAY_NOT_FOUND("cratepay.not_found", "$0Die Crate $4{{crate}} $0existiert nicht!"),
  CRATEPAY_TRANSFERED_SENDER("cratepay.transfered.sender", "$0Du hast $3{{keys}} Schlüssel $0für die Crate $3{{crate}} $0an $3{{target}} $0überwiesen."),
  CRATEPAY_TRANSFERED_RECEIVER("cratepay.transfered.receiver", "$3{{sender}} $0hat $3{{keys}} Schlüssel $0für die Crate $3{{crate}} $0an dich überwiesen."),

  //=========================================================================//
  //                            CRATESORT Command                            //
  //=========================================================================//

  COMMAND_CRATESORT_NOT_EXISTING("cratesort.not_existing", "$0Es existiert keine Crate namens $4{{name}}$0!"),
  COMMAND_CRATESORT_MISSING_IDS("cratesort.missing_ids", "$0Es fehlen $4{{num_missing}} IDs $0in der Sequenz!"),
  COMMAND_CRATESORT_INVALID_ID("cratesort.invalid_id", "$0Die ID $4{{invalid_id}} $0der Sequenz ist $4ungültig$0!"),
  COMMAND_CRATESORT_SORTED("cratesort.sorted", "$0Die Items der Crate $3{{name}} $0wurden $3sortiert$0."),
  COMMAND_CRATESORT_LIST_HEADER("cratesort.list.header", "$0Items der Crate $3{{name}}$0:"),
  COMMAND_CRATESORT_LIST_ENTRY("cratesort.list.entry", "$0[$3{{index}}$0]: $3{{name}}"),

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
    "$0Einträge: $2{{num_items}}",
    "$0Max. pro Seite: $2{{max_items}}",
    " ",
    "$0Zeigt aktuelle Seiteninformationen"
  ),
  GUI_GENERICS_NAV_BACK_NAME("gui.generics.nav.back.name", "$0» $2Zurück $0«"),
  GUI_GENERICS_NAV_BACK_LORE(
    "gui.generics.nav.back.lore",
    " ",
    "$0Zurück zum vorherigen Menu"
  ),
  GUI_GENERICS_BUTTONS_ENABLE_NAME("gui.generics.buttons.enable.name", "$0» &aAktivieren"),
  GUI_GENERICS_BUTTONS_ENABLE_LORE(
    "gui.generics.buttons.enable.lore",
    " ",
    "$0Ändere den Zustand auf &aaktiviert&0."
  ),
  GUI_GENERICS_BUTTONS_DISABLE_NAME("gui.generics.buttons.disable.name", "$0» &cDeaktivieren"),
  GUI_GENERICS_BUTTONS_DISABLE_LORE(
    "gui.generics.buttons.disable.lore",
    " ",
    "$0Ändere den Zustand auf &cdeaktiviert&0."
  ),
  GUI_GENERICS_PLACEHOLDERS_ENABLED("gui.generics.placeholders.enabled", "&aaktiviert"),
  GUI_GENERICS_PLACEHOLDERS_DISABLED("gui.generics.placeholders.disabled", "&cdeaktiviert"),

  //=========================================================================//
  //                              Enderchest GUI                             //
  //=========================================================================//

  GUI_ENDERCHEST_TITLE("gui.enderchest.title", "$0Enderchest von $2{{viewer}}"),
  GUI_ENDERCHEST_LOCK_NAME("gui.enderchest.lock.name", "$0» &cGesperrter Slot"),
  GUI_ENDERCHEST_LOCK_LORE(
    "gui.enderchest.lock.lore",
    " ",
    "$0Der Slot &c{{slot}} $0der Seite &c{{page}}",
    "$0wurde noch nicht freigeschalten!"
  ),

  //=========================================================================//
  //                               Ignores GUI                               //
  //=========================================================================//

  GUI_IGNORES_TITLE("gui.ignores.title", "$0Ignores von $2{{viewer}}"),
  GUI_IGNORES_NONE_NAME("gui.ignores.none.name", "$0» &cKeine Ignores"),
  GUI_IGNORES_NONE_LORE(
    "gui.ignores.none.lore",
    " ",
    "$0Du hast aktuell &ckeine $0Spieler ignoriert."
  ),
  GUI_IGNORES_PLAYER_NAME("gui.ignores.player.name", "$0» $2{{name}} $0«"),
  GUI_IGNORES_PLAYER_LORE(
    "gui.ignores.player.lore",
    " ",
    "$0MSG Blockiert: {{msg_state}}",
    "$0Chat Blockiert: {{chat_state}}"
  ),

  //=========================================================================//
  //                                IGNORE GUI                               //
  //=========================================================================//

  GUI_IGNORE_TITLE("gui.ignore.title", "$0Ignore $2{{target}}"),
  GUI_IGNORE_MSG_NAME("gui.ignore.msg.name", "$0» $2Private Nachrichten $0«"),
  GUI_IGNORE_MSG_LORE(
    "gui.ignore.msg.lore",
    " ",
    "$0Gibt an, ob du private Nachrichten",
    "$0von $2{{target}} $0empfängst.",
    " ",
    "$0Aktueller Zustand: {{state}}"
  ),
  GUI_IGNORE_CHAT_NAME("gui.ignore.chat.name", "$0» $2Chat $0«"),
  GUI_IGNORE_CHAT_LORE(
    "gui.ignore.chat.lore",
    " ",
    "$0Gibt an, ob du Nachrichten von",
    "$2{{target}}$ 0im Chat empfängst.",
    " ",
    "$0Aktueller Zustand: {{state}}"
  ),

  //=========================================================================//
  //                            SingleChoice GUI                             //
  //=========================================================================//

  GUI_SINGLECHOICE_TITLE("gui.singlechoice.title", "$0Auswahl $2{{type}}"),

  //=========================================================================//
  //                             ItemEditor GUI                              //
  //=========================================================================//

  GUI_ITEMEDITOR_TITLE("gui.itemeditor.title", "$0Itemeditor $2{{item_type}}"),
  GUI_ITEMEDITOR_META_UNAVAILABLE("gui.itemeditor.meta_unavailable", "$0Auf die ItemMeta dieses Items konnte $4nicht $0zugegriffen werden!"),
  GUI_ITEMEDITOR_AMOUNT_CHANGED("gui.itemeditor.amount.changed", "$0Du hast die Anzahl auf $2{{amount}} $0gesetzt."),
  GUI_ITEMEDITOR_AMOUNT_INCREASE_NAME("gui.itemeditor.amount.increase.name", "$0» $2Anzahl erhöhen $0«"),
  GUI_ITEMEDITOR_AMOUNT_INCREASE_LORE(
    "gui.itemeditor.amount.increase.lore",
    " ",
    "$0Linksklick: $2+1",
    "$0Shift + Linksklick: $2+64",
    "$0Rechtsklick: $2+8",
    "$0Shift + Rechtsklick: $2=64"
  ),
  GUI_ITEMEDITOR_AMOUNT_DECREASE_NAME("gui.itemeditor.amount.decrease.name", "$0» $2Anzahl verringern $0«"),
  GUI_ITEMEDITOR_AMOUNT_DECREASE_LORE(
    "gui.itemeditor.amount.decrease.lore",
    " ",
    "$0Linksklick: $2-1",
    "$0Shift + Linksklick: $2-64",
    "$0Rechtsklick: $2-8",
    "$0Shift + Rechtsklick: $2=1"
  ),
  GUI_ITEMEDITOR_MATERIAL_NAME("gui.itemeditor.material.name", "$0» $2Material $0«"),
  GUI_ITEMEDITOR_MATERIAL_LORE(
    "gui.itemeditor.material.lore",
    " ",
    "$0Ändere das $2Material $0dieses Items."
  ),
  GUI_ITEMEDITOR_CHOICE_MATERIAL_TITLE("gui.itemeditor.choice.material.title", "$2Material"),
  GUI_ITEMEDITOR_CHOICE_MATERIAL_NAME("gui.itemeditor.choice.material.name", "$2{{hr_type}}"),
  GUI_ITEMEDITOR_CHOICE_MATERIAL_LORE(
    "gui.itemeditor.choice.material.lore",
    " ",
    "$0Klicke um dieses Material zu wählen"
  ),
  GUI_ITEMEDITOR_MATERIAL_CHANGED("gui.itemeditor.material.changed", "$0Du hast das Material $2{{material}} $0gewählt."),
  GUI_ITEMEDITOR_FLAGS_NAME("gui.itemeditor.flags.name", "$0» $2Flags $0«"),
  GUI_ITEMEDITOR_FLAGS_LORE(
    "gui.itemeditor.flags.lore",
    " ",
    "$0Aktiviere/Deaktiviere $2Flags$0."
  ),
  GUI_ITEMEDITOR_CHOICE_FLAG_TITLE("gui.itemeditor.choice.flag.title", "$2Flags"),
  GUI_ITEMEDITOR_CHOICE_FLAG_NAME("gui.itemeditor.choice.flag.name", "$2{{flag}}"),
  GUI_ITEMEDITOR_CHOICE_FLAG_ACTIVE("gui.itemeditor.choice.flag.inactive", "&aAktiviert"),
  GUI_ITEMEDITOR_CHOICE_FLAG_INACTIVE("gui.itemeditor.choice.flag.active", "&cDeaktiviert"),
  GUI_ITEMEDITOR_CHOICE_FLAG_LORE(
    "gui.itemeditor.choice.flag.lore",
    " ",
    "$0Aktueller Zustand: {{state}}",
    "$0Klicke, um den Zustand zu wechseln."
  ),
  GUI_ITEMEDITOR_FLAG_CHANGED("gui.itemeditor.flag.changed", "$0Du hast den Flag $2{{flag}} $0auf $2{{state}} $0geändert."),
  GUI_ITEMEDITOR_ENCHANTMENTS_NAME("gui.itemeditor.enchantments.name", "$0» $2Verzauberungen $0«"),
  GUI_ITEMEDITOR_ENCHANTMENTS_LORE(
    "gui.itemeditor.enchantments.lore",
    " ",
    "$0Füge Verzauberungen $2hinzu $0oder",
    "$2entferne $0bestehende Verzauberungen."
  ),
  GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_TITLE("gui.itemeditor.choice.enchantment.title", "$2Verzauberung"),
  GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_NAME("gui.itemeditor.choice.enchantment.name", "$2{{enchantment}}"),
  GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_ACTIVE("gui.itemeditor.choice.enchantment.inactive", "&cBereits angewandt"),
  GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_INACTIVE("gui.itemeditor.choice.enchantment.active", "&aNoch nicht angewandt"),
  GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_LORE_ACTIVE(
    "gui.itemeditor.choice.enchantments.lore.active",
    " ",
    "$0Aktueller Zustand: {{state}}",
    "$0Angewandter Level: $2{{level}}",
    "$0Klicke um die Verzauberung zu &centfernen$0."
  ),
  GUI_ITEMEDITOR_CHOICE_ENCHANTMENT_LORE_INACTIVE(
    "gui.itemeditor.choice.enchantments.lore.inactive",
    " ",
    "$0Aktueller Zustand: {{state}}",
    "$0Klicke um die Verzauberung &ahinzuzufügen$0."
  ),
  GUI_ITEMEDITOR_ENCHANTMENT_ADDED("gui.itemeditor.enchantment.changed", "$0Du hast die Verzauberung $2{{enchantment}} $0auf Level $2{{level}} $0hinzugefügt."),
  GUI_ITEMEDITOR_ENCHANTMENT_REMOVED("gui.itemeditor.enchantment.removed", "$0Du hast die Verzauberung $2{{enchantment}} $0entfernt."),
  GUI_ITEMEDITOR_ENCHANTMENT_LEVEL_PROMPT("gui.itemeditor.enchantment.level_prompt", "$0Bitte gib das gewünschte $2Verzauberungslevel $0in den $2Chat $0ein."),
  GUI_ITEMEDITOR_DISPLAYNAME_NAME("gui.itemeditor.displayname.name", "$0» $2Anzeigename $0«"),
  GUI_ITEMEDITOR_DISPLAYNAME_LORE(
    "gui.itemeditor.displayname.lore",
    " ",
    "$0Ändere den $2Namen $0des Items."
  ),
  GUI_ITEMEDITOR_DISPLAYNAME_PROMPT("gui.itemeditor.displayname.prompt", "$0Bitte gib den gewünschten $2Anzeigenamen $0in den $2Chat $0ein (tippe \"null\" um diesen zurückzusetzen)."),
  GUI_ITEMEDITOR_DISPLAYNAME_SET("gui.itemeditor.displayname.set", "$0Du hast den Anzeigenamen auf &r{{name}} $0gesetzt."),
  GUI_ITEMEDITOR_DISPLAYNAME_RESET("gui.itemeditor.displayname.reset", "$0Du hast den Anzeigenamen $2zurückgesetzt$0."),
  GUI_ITEMEDITOR_LORE_NAME("gui.itemeditor.lore.name", "$0» $2Lore $0«"),
  GUI_ITEMEDITOR_LORE_LORE(
    "gui.itemeditor.lore.lore",
    " ",
    "$0Ändere die $2Zeilen $0der $2Lore$0.",
    " ",
    "$0Linksklick: $2Zeile einfügen",
    "$0Shift + Linksklick: $2Zeile anfügen",
    "$0Rechtsklick: $2Zeile wählen und entfernen",
    "$0Shift + Rechtsklick: $2Lore leeren"
  ),
  GUI_ITEMEDITOR_LORE_RESET("gui.itemeditor.lore.reset", "$0Du hast die Lore $2zurückgesetzt$0."),
  GUI_ITEMEDITOR_LORE_LINE_REMOVED("gui.itemeditor.lore.line_removed", "$0Die Zeile $2{{line_number}} $0($2{{line_content}}$0) wurde entfernt."),
  GUI_ITEMEDITOR_LORE_NO_LORE("gui.itemeditor.lore.no_lore", "$0Dieses Item hat noch $4keine $0Lore!"),
  GUI_ITEMEDITOR_CHOICE_LORE_TITLE("gui.itemeditor.choice.lore.title", "$2Lore"),
  GUI_ITEMEDITOR_CHOICE_LORE_NAME("gui.itemeditor.choice.lore.name", "$2Zeile {{line_number}}"),
  GUI_ITEMEDITOR_CHOICE_LORE_LORE(
    "gui.itemeditor.choice.lore.lore",
    " ",
    "$0Inhalt: ",
    "{{line_content}}"
  ),
  GUI_ITEMEDITOR_LORE_PROMPT("gui.itemeditor.lore.prompt", "$0Bitte gib die gewünschte $2Lore-Zeile $0in den $2Chat $0ein."),
  GUI_ITEMEDITOR_LORE_SELECT_POS("gui.itemeditor.lore.select_pos", "$0Bitte wähle die gewünschte $2Position $0der Zeile."),
  GUI_ITEMEDITOR_LORE_LINE_ADDED("gui.itemeditor.lore.line_added", "$0Die $2Lore-Zeile $0wurde hinzugefügt."),
  GUI_ITEMEDITOR_DURABILITY_NAME("gui.itemeditor.durability.name", "$0» $2Haltbarkeit $0«"),
  GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE("gui.itemeditor.durability.unbreakable", "$2Unzerstörbar"),
  GUI_ITEMEDITOR_DURABILITY_BREAKABLE("gui.itemeditor.durability.breakable", "$2{{current_durability}}$0/$2{{max_durability}}"),
  GUI_ITEMEDITOR_DURABILITY_NON_BREAKABLE("gui.itemeditor.durability.non_breakable", "$2Nicht beschädigbar"),
  GUI_ITEMEDITOR_DURABILITY_LORE(
    "gui.itemeditor.durability.lore",
    " ",
    "$0Haltbarkeit: $2{{durability}}",
    " ",
    "$0Linksklick: $2Haltbarkeit erhöhen",
    "$0Shift + Linksklick: $2Unzerstörbar setzen",
    "$0Rechtsklick: $2Haltbarkeit verringern",
    "$0Shift + Rechtsklick: $2Unzerstörbarkeit entfernen"
  ),
  GUI_ITEMEDITOR_DURABILITY_NOT_BREAKABLE("gui.itemeditor.durability.not_breakable", "$0Dieses Item ist $4nicht beschädigbar$0!"),
  GUI_ITEMEDITOR_DURABILITY_CHANGED("gui.itemeditor.durability.changed", "$0Die Haltbarkeit dieses Items wurde auf $2{{current_durability}}$0/$2{{max_durability}} $0gesetzt."),
  GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_ACTIVE("gui.itemeditor.durability.unbreakable_active", "$0Das Item ist nun $2Unzerstörbar$0."),
  GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_INACTIVE("gui.itemeditor.durability.unbreakable_inactive", "$0Das Item ist nun $2nicht mehr $0unzerstörbar."),
  GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_NOT_ACTIVE("gui.itemeditor.durability.unbreakable_not_active", "$0Das Item ist $4nicht unzerstörbar$0!"),
  GUI_ITEMEDITOR_DURABILITY_UNBREAKABLE_NOT_INACTIVE("gui.itemeditor.durability.unbreakable_not_inactive", "$0Das Item ist $4bereits unzerstörbar$0!"),
  GUI_ITEMEDITOR_ATTRIBUTES_NAME("gui.itemeditor.attributes.name", "$0» $2Attribute $0«"),
  GUI_ITEMEDITOR_ATTRIBUTES_LORE(
    "gui.itemeditor.attributes.lore",
    " ",
    "$0Füge Attribute $2hinzu $0oder",
    "$2entferne $0bestehende Attribute.",
    " ",
    "$0Linksklick: $2Attribut hinzufügen",
    "$0Rechtsklick: $2Attribut wählen und entfernen",
    "$0Shift + Rechtsklick: $2Attribute leeren"
  ),
  GUI_ITEMEDITOR_ATTRIBUTES_HAS_NONE("gui.itemeditor.attributes.has_none", "$0Dieses Item besitzt $4keine Attribute$0!"),
  GUI_ITEMEDITOR_ATTRIBUTES_CLEARED("gui.itemeditor.attributes.cleared", "$0Alle Attribute dieses Items wurden $2entfernt$0."),
  GUI_ITEMEDITOR_ATTRIBUTES_REMOVED("gui.itemeditor.attributes.removed", "$0Das Attribut $2{{attribute}} $0wurde von diesem Item $2entfernt$0."),
  GUI_ITEMEDITOR_ATTRIBUTES_ADDED("gui.itemeditor.attributes.added", "$0Das Attribut $2{{attribute}} $0wurde diesem Item $2hinzugefügt$0."),
  GUI_ITEMEDITOR_CHOICE_ATTR_TITLE("gui.itemeditor.choice.attr.title", "$2Attribute"),
  GUI_ITEMEDITOR_CHOICE_ATTR_NAME("gui.itemeditor.choice.attr.name", "$2{{attribute}}"),
  GUI_ITEMEDITOR_CHOICE_ATTR_EXISTING_LORE(
    "gui.itemeditor.choice.attr.existing_lore",
    " ",
    "$0Name: $2{{name}}",
    "$0Wert: $2{{amount}}",
    "$0Operator: $2{{operation}}",
    "$0Slot: $2{{slot}}"
  ),
  GUI_ITEMEDITOR_CHOICE_ATTR_NEW_LORE(
    "gui.itemeditor.choice.attr.new_lore",
    " ",
    "$0Erstelle eine $2neue $0Instanz",
    "$0dieses $2Attributs$0."
  ),
  GUI_ITEMEDITOR_ATTRIBUTES_AMOUNT_PROMPT("gui.itemeditor.attributes.amount_prompt", "$0Bitte gib den gewünschten $2Betrag $0in den $2Chat $0ein."),
  GUI_ITEMEDITOR_CHOICE_EQUIPMENT_TITLE("gui.itemeditor.choice.equipment.title", "$2Equipment"),
  GUI_ITEMEDITOR_CHOICE_EQUIPMENT_NAME("gui.itemeditor.choice.equipment.name", "$2{{slot}}"),
  GUI_ITEMEDITOR_CHOICE_EQUIPMENT_LORE(
    "gui.itemeditor.choice.equipment.lore",
    " ",
    "$0Klicke um diesen Equipment-Slot zu wählen."
  ),
  GUI_ITEMEDITOR_CHOICE_OPERATION_TITLE("gui.itemeditor.choice.operation.title", "$2Operator"),
  GUI_ITEMEDITOR_CHOICE_OPERATION_NAME("gui.itemeditor.choice.operation.name", "$2{{operation}}"),
  GUI_ITEMEDITOR_CHOICE_OPERATION_LORE(
    "gui.itemeditor.choice.operation.lore",
    " ",
    "$0Klicke um diese Operation zu wählen."
  ),
  GUI_ITEMEDITOR_SKULLOWNER_NO_SKULL("gui.itemeditor.skullowner.no_skull", "$0Dieses Item ist $4kein Kopf$0!"),
  GUI_ITEMEDITOR_SKULLOWNER_CHANGED("gui.itemeditor.skullowner.changed", "$0Der $2Kopfbesitzer $0dieses Items wurde auf $2{{owner}} $0geändert."),
  GUI_ITEMEDITOR_SKULLOWNER_NAME("gui.itemeditor.skullowner.name", "$0» $2Kopfbesitzer $0«"),
  GUI_ITEMEDITOR_SKULLOWNER_LORE(
    "gui.itemeditor.skullowner.lore",
    " ",
    "$0Ändere den $2Besitzer $0dieses Kopfes."
  ),
  GUI_ITEMEDITOR_SKULLOWNER_PROMPT("gui.itemeditor.skullowner.prompt", "$0Bitte gib den gewünschten $2Kopfbesitzer $0in den $2Chat $0ein."),
  GUI_ITEMEDITOR_SKULLOWNER_NOT_LOADABLE("gui.itemeditor.skullowner.not_loadable", "$0Der Skin von $4{{owner}} $0konnte nicht geladen werden!"),
  GUI_ITEMEDITOR_LEATHERCOLOR_NO_LEATHER("gui.itemeditor.leathercolor.no_leather", "$0Dieses Item ist $4keine Lederrüstung$0!"),
  GUI_ITEMEDITOR_LEATHERCOLOR_CHANGED("gui.itemeditor.leathercolor.changed", "$0Die $2Lederfarbe $0dieses Items wurde auf $2{{color}} $0geändert."),
  GUI_ITEMEDITOR_LEATHERCOLOR_NAME("gui.itemeditor.leathercolor.name", "$0» $2Lederfarbe $0«"),
  GUI_ITEMEDITOR_LEATHERCOLOR_LORE(
    "gui.itemeditor.leathercolor.lore",
    " ",
    "$0Ändere die $2Farbe $0dieses $2Lederteils$0.",
    " ",
    "$0Linksklick: $2Farbwert aus Liste",
    "$0Rechtsklick: $2RGB Farbwert"
  ),
  GUI_ITEMEDITOR_LEATHERCOLOR_PROMPT("gui.itemeditor.leathercolor.prompt", "$0Bitte gib den gewünschten $2Farbwert $0in den $2Chat $0ein (r g b)."),
  GUI_ITEMEDITOR_LEATHERCOLOR_INVALID_FORMAT("gui.itemeditor.leathercolor.invalid_format", "$0Die Farbeingabe von $4{{input}} $0ist ungültig!"),
  GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_TITLE("gui.itemeditor.choice.leathercolor.title", "$2Lederfarbe"),
  GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_NAME("gui.itemeditor.choice.leathercolor.name", "$2{{color}}"),
  GUI_ITEMEDITOR_CHOICE_LEATHERCOLOR_LORE(
    "gui.itemeditor.choice.leathercolor.lore",
    " ",
    "$0Klicke um diese Farbe zu wählen."
  ),

  //=========================================================================//
  //                             Preferences GUI                             //
  //=========================================================================//

  GUI_PREFERENCES_TITLE("gui.preferences.title", "$0Einstellungen von $2{{viewer}}"),
  GUI_PREFERENCES_MSG_NAME("gui.preferences.msg.name", "$0» $2Private Nachrichten $0«"),
  GUI_PREFERENCES_MSG_LORE(
    "gui.preferences.msg.lore",
    " ",
    "$0Gibt an, ob du private Nachrichten, welche",
    "$0mittels /msg und /r versandt werden, empfängst.",
    " ",
    "$0Aktueller Zustand: {{state}}"
  ),
  GUI_PREFERENCES_CHAT_NAME("gui.preferences.chat.name", "$0» $2Chat $0«"),
  GUI_PREFERENCES_CHAT_LORE(
    "gui.preferences.chat.lore",
    " ",
    "$0Gibt an, ob du Nachrichten von anderen",
    "$0Spielern im Chat empfängst. Systemnachrichten",
    "$0bleiben erhalten.",
    " ",
    "$0Aktueller Zustand: {{state}}"
  ),
  GUI_PREFERENCES_SCOREBOARD_NAME("gui.preferences.scoreboard.name", "$0» $2Scoreboard $0«"),
  GUI_PREFERENCES_SCOREBOARD_LORE(
    "gui.preferences.scoreboard.lore",
    " ",
    "$0Gibt an, ob das Scoreboard am rechten",
    "$0Bildschirmrand angezeigt wird.",
    " ",
    "$0Aktueller Zustand: {{state}}"
  ),

  //=========================================================================//
  //                           Crate Item Detail GUI                         //
  //=========================================================================//

  GUI_CRATE_DETAIL_NAME("gui.crate_detail.name", "$0Crate $2{{name}}"),
  GUI_CRATE_DETAIL_PROBABILITY_NAME("gui.crate_detail.probability.name", "$0» $2Wahrscheinlichkeit $0«"),
  GUI_CRATE_DETAIL_PROBABILITY_LORE(
    "gui.crate_detail.probability.lore",
    " ",
    "$0Ändere die $2Wahrscheinlichkeit $0dieses",
    "$0Item zu ziehen."
  ),
  GUI_CRATE_DETAIL_PROBABILITY_PROMPT("gui.crate_detail.probability.prompt", "$0Bitte gib die neue $2Wahrscheinlichkeit $0in den $2Chat $0ein."),
  GUI_CRATE_DETAIL_DELETE_CANCELLED("gui.crate_detail.delete.cancelled", "$0Die Entfernung des Items $2{{item}} $0wurde $2abgebrochen$0."),
  GUI_CRATE_DETAIL_DELETE_NAME("gui.crate_detail.delete.name", "$0» &cEntfernen $0«"),
  GUI_CRATE_DETAIL_DELETE_LORE(
    "gui.crate_detail.delete.lore",
    " ",
    "&cEntferne $0dieses Item."
  ),
  GUI_CRATE_DETAIL_EDIT_NAME("gui.crate_detail.edit.name", "$0» $2Bearbeiten $0«"),
  GUI_CRATE_DETAIL_EDIT_LORE(
    "gui.crate_detail.edit.lore",
    " ",
    "$0Öffnet den $2Itemeditor$0."
  ),

  //=========================================================================//
  //                                Crate Draw GUI                           //
  //=========================================================================//

  GUI_CRATE_DRAW_NAME("gui.crate_draw.name", "$0Crate $2{{name}}"),
  GUI_CRATE_DRAW_INDICATOR_NAME("gui.crate_draw.indicator.name", "$0» $2Dein Gewinn"),
  GUI_CRATE_DRAW_INDICATOR_LORE(
    "gui.crate_draw.indicator.lore",
    " ",
    "$0Markiert den Slot der Gewinnausgabe."
  ),
  GUI_CRATE_DRAW_NO_ITEMS("gui.crate_draw.no_items", "$0Die Crate $4{{name}} $0besitzt noch $4keine Items$0!"),
  GUI_CRATE_DRAW_NO_KEYS("gui.crate_draw.no_keys", "$0Du besitzt $4keinen Schlüssel $0für die Crate $4{{name}}$0!"),
  GUI_CRATE_DRAW_KEY_USED("gui.crate_draw.key_used", "$0Du hast einen $2Schlüssel $0für die Crate $2{{name}} $0eingelöst."),
  GUI_CRATE_DRAW_KEY_CANCELLED("gui.crate_draw.key_cancelled", "$0Die Öffnung der Crate $2{{name}} $0wurde $2abgebrochen$0."),
  GUI_CRATE_DRAW_PRIZE("gui.crate_draw.prize", "$0Du hast das Item $2{{item}} $0gewonnen!"),

  //=========================================================================//
  //                             Crate Content GUI                           //
  //=========================================================================//

  GUI_CRATE_CONTENT_NAME("gui.crate_content.name", "$0Crate $2{{name}}"),
  GUI_CRATE_CONTENT_NONE_NAME("gui.crate_content.none.name", "$0» &cKeine Items"),
  GUI_CRATE_CONTENT_NONE_LORE(
    "gui.crate_content.none.lore",
    " ",
    "$0Diese Crate hat noch $4keine $0Items!"
  ),
  GUI_CRATE_CONTENT_CONTENT_NAME("gui.crate_content.content.name", "$2{{hr_type}}"),
  GUI_CRATE_CONTENT_CONTENT_LORE(
    "gui.crate_content.content.lore",
    " ",
    "$0Dieses Item ist mit einer Wahrscheinlichkeit",
    "$0von $2{{probability}}% $0in der Crate $2{{name}} $0vorzufinden."
  ),

  //=========================================================================//
  //                                Kits GUI                                 //
  //=========================================================================//

  GUI_KITS_TITLE("gui.kits.title", "$0Kitmenu von $2{{viewer}}"),
  GUI_KITS_KIT_NAME("gui.kits.kit.name", "$0» $2{{name}}"),
  GUI_KITS_KIT_LORE(
    "gui.kits.kit.lore",
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
  //                            Confirmation GUI                             //
  //=========================================================================//

  GUI_CONFIRMATION_TITLE("gui.confirmation.title", "$0Bestätigung"),
  GUI_CONFIRMATION_CONFIRM_NAME("gui.confirmation.confirm.name", "$0» &aBestätigen"),
  GUI_CONFIRMATION_CONFIRM_LORE(
    "gui.confirmation.confirm.lore",
    " ",
    "$0Bestätigt diese Aktion."
  ),
  GUI_CONFIRMATION_CANCEL_NAME("gui.confirmation.cancel.name", "$0» &cAbbrechen"),
  GUI_CONFIRMATION_CANCEL_LORE(
    "gui.confirmation.cancel.lore",
    " ",
    "$0Bricht diese Aktion ab."
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
  ERR_FLOATPARSE("errors.floatparse", "$4Die Eingabe $5{{number}} $4ist keine Kommazahl!"),
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
