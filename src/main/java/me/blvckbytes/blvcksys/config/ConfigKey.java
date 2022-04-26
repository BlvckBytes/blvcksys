package me.blvckbytes.blvcksys.config;

public enum ConfigKey {

  //=========================================================================//
  //                               Global Prefix                             //
  //=========================================================================//

  PREFIX("prefix", "&8[&5BVS&8]&r "),

  //=========================================================================//
  //                                 Logging                                 //
  //=========================================================================//

  LOGGING_PREFIX_DEBUG("logging_prefix.debug", "&6"),
  LOGGING_PREFIX_INFO("logging_prefix.info", "&a"),
  LOGGING_PREFIX_ERROR("logging_prefix.error", "&4"),

  //=========================================================================//
  //                                   Chat                                  //
  //=========================================================================//

  CHAT_FORMAT("chat_format", "{{prefix}}{{name}}&7: {{message}}"),

  //=========================================================================//
  //                                  Sidebar                                //
  //=========================================================================//

  SIDEBAR_TITLE("sidebar.title", "&5&lBlvckBytes.DEV"),
  SIDEBAR_LINES(
    "sidebar.lines",
    " ",
    "Spieler:",
    "&d{{num_online}}&7/&d{{num_slots}}",
    " ",
    "Onlinezeit:",
    "&cSoon",
    " ",
    "Münzen:",
    "&cSoon",
    " ",
    "Freunde:",
    "&cSoon",
    " ",
    "Dein Name:",
    "&d{{name}}"
  ),

  //=========================================================================//
  //                                 Tablist                                 //
  //=========================================================================//

  TABLIST_HEADER(
    "tablist.header",
    " ",
    "&5&lBlvckBytes.DEV",
    "&7Willkommen, &d{{player}}&7!",
    " ",
    "&8&m──────────────────────────────",
    " "
  ),
  TABLIST_FOOTER(
    "tablist.footer",
    " ",
    "&8&m──────────────────────────────",
    " ",
    "&d✦ &7Online: &d{{num_online}}&7/&d{{num_slots}} &8| &d⇄ &7Ping: &d{{ping}}ms &8| &d⌚ &7Uhrzeit: &d{{time}}",
    " "
  ),
  TABLIST_PREFIXES(
    "tablist.prefixes",
    "Admin;&cAdmin &8| &c",
    "Spieler;&2Spieler &8| &2"
  ),

  //=========================================================================//
  //                                Playerlist                               //
  //=========================================================================//

  PLAYERLIST_TEXT(
    "playerlist.text",
    "&7» &8❘ &e&lDevelopment Server",
    "&7» &8❘ ➟ &aYour &8× &dAdvertisement &8× &6Placed &8× &9Here"
  ),
  PLAYERLIST_HOVER(
    "playerlist.hover",
    "&bThis server has an",
    "&bawesome hover applied!"
  ),
  PLAYERLIST_ONLINE("playerlist.online", "&c5&7/&c12 &7(&dHello World&7)"),

  //=========================================================================//
  //                              MSG, R Command                             //
  //=========================================================================//

  MSG_SENDER("msg.sender", "&8(&6Du &7-> &6{{receiver}}&8)&7: {{message}}"),
  MSG_RECEIVER("msg.receiver", "&8(&6{{sender}}&8 -> &6Dir&8)&7: {{message}}"),
  MSG_NO_PARTNER("msg.no_partner", "&cDu hast keinen aktiven Nachrichten-Partner!"),
  MSG_SELF("msg.self", "&cDu kannst dir selbst keine Nachrichten schreiben!"),

  //=========================================================================//
  //                               FLY Command                               //
  //=========================================================================//

  FLY_ENABLED("fly.enabled", "&7Du kannst &dnun &7fliegen!"),
  FLY_DISABLED("fly.disabled", "&7Du kannst &dnicht mehr &7fliegen!"),
  FLY_REVOKED("fly.revoked", "&7Du hast soeben deine &cFlugrechte &7verloren!"),

  //=========================================================================//
  //                               GIVE Command                              //
  //=========================================================================//

  GIVE_INVALID_ITEM("give.invalid_item", "&cDas Item {{material}} existiert nicht!"),
  GIVE_SELF("give.self", "&7Du hast dir &d{{amount}}x {{material}} &7gegeben."),
  GIVE_SENDER("give.sender", "&7Dem Spieler &d{{target}} &7wurden &d{{amount}}x {{material}} &7gegeben."),
  GIVE_RECEIVER("give.receiver", "&7Dir wurden &d{{amount}}x {{material}} &7von &d{{executor}} &7gegeben."),
  GIVE_DROPPED("give.dropped", "&cEs wurden {{num_dropped}} Items fallen gelassen!"),

  //=========================================================================//
  //                              INJECT Command                             //
  //=========================================================================//

  INJECT_INJECTED("inject.injected", "&7Die Pakete von &d{{target}} &7werden &dnun &7abgefangen."),
  INJECT_UNINJECTED("inject.uninjected", "&7Die Pakete von &d{{target}} &7werden nun &dnicht mehr &7abgefangen."),
  INJECT_EVENT("inject.event", "&5{{direction}}&8 -> &r{{object}}"),
  INJECT_EVENT_COLOR_OTHER("inject.event_color.other", "&7"),
  INJECT_EVENT_COLOR_VALUES("inject.event_color.values", "&d"),
  INJECT_INVALID_REGEX("inject.invalid_regex", "&cDas Regex \"{{regex}}\" ist ungültig!"),

  //=========================================================================//
  //                            Generic Messages                             //
  //=========================================================================//
  GENERIC_JOINED("generic.joined", "&7Der Spieler &d{{name}} &7ist dem Server &dbeigetreten&7!"),
  GENERIC_QUIT("generic.quit", "&7Der Spieler &d{{name}} &7hat den Server &dverlassen&7!"),

  //=========================================================================//
  //                              Error Messages                             //
  //=========================================================================//

  ERR_INTERNAL("errors.internal", "&4Es trat ein interner Fehler auf!"),
  ERR_INTPARSE("errors.intparse", "&cDie Eingabe &4{{number}} &cist keine Ganzzahl!"),
  ERR_PERMISSION("errors.permission", "&cDir fehlt das Recht &4{{permission}} &cum diesen Befehl ausführen zu können!"),
  ERR_OPTIONPARSE("errors.optionparse", "&cDie Eingabe &4{{option}} &cist keine gültige Wahl!"),
  ERR_NOT_ONLINE("errors.not_online", "&cDer Spieler &4{{player}} &cist nicht online!"),
  ERR_USAGE_PREFIX("errors.usage_prefix", "&7Benutzung: "),
  ERR_USAGE_COLOR_MANDATORY("errors.usage_color.mandatory", "&d"),
  ERR_USAGE_COLOR_OPTIONAL("errors.usage_color.optional", "&b"),
  ERR_USAGE_COLOR_BRACKETS("errors.usage_color.brackets", "&8"),
  ERR_USAGE_COLOR_OTHER("errors.usage_color.other", "&7"),
  ERR_USAGE_COLOR_FOCUS("errors.usage_color.focus", "&4&l"),
  ERR_NOT_A_PLAYER("errors.not_a_player", "&cDieser Befehl ist nur für &4Spieler &czugänglich!"),
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
