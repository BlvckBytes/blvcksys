package me.blvckbytes.blvcksys.config;

public enum ConfigKey {

  // TODO: Change templating to a proper format, like {var}, since placeholders are now unmovable

  PREFIX("prefix", "&8[&5BVS&8]&r "),

  //=========================================================================//
  //                              MSG, R Command                             //
  //=========================================================================//

  MSG_SENDER("msg.sender", "&8[&6Nachrichten&8] &8(&6Du &7-> &6%s&8)&7: %s"),
  MSG_RECEIVER("msg.receiver", "&8[&6Nachrichten&8] &8(&6%s&8 -> &6Dir&8)&7: %s"),
  MSG_NO_PARTNER("msg.no_partner", "&cDu hast keinen aktiven Nachrichten-Partner!"),
  MSG_SELF("msg.self", "&cDu kannst dir selbst keine Nachrichten schreiben!"),

  //=========================================================================//
  //                               GIVE Command                              //
  //=========================================================================//

  GIVE_INVALID_ITEM("give.invalid_item", "&cDas Item %s existiert nicht!"),
  GIVE_SELF("give.self", "&7Du hast dir &d%dx %s &7gegeben."),
  GIVE_SENDER("give.sender", "&7Dem Spieler &d%s &7wurden &d%dx %s &7gegeben."),
  GIVE_RECEIVER("give.receiver", "&7Dir wurden &d%dx %s &7von &d%s &7gegeben."),
  GIVE_DROPPED("give.dropped", "&cEs wurden %d Items fallen gelassen!"),

  //=========================================================================//
  //                              INJECT Command                             //
  //=========================================================================//

  INJECT_INJECTED("inject.injected", "&7Die Pakete von &d%s &7werden &dnun &7abgefangen."),
  INJECT_UNINJECTED("inject.uninjected", "&7Die Pakete von &d%s &7werden nun &dnicht mehr &7abgefangen."),
  INJECT_EVENT("inject.event", "&8[&5%s&8] %s"),
  INJECT_EVENT_COLOR_OTHER("inject.event_color.other", "&7"),
  INJECT_EVENT_COLOR_VALUES("inject.event_color.values", "&d"),
  INJECT_INVALID_DIR("inject.invalid_dir", "&cDie Richtung %s existiert nicht!"),
  INJECT_INVALID_REGEX("inject.invalid_regex", "&cDas Regex \"%s\" ist ungültig!"),

  //=========================================================================//
  //                              Error Messages                             //
  //=========================================================================//

  ERR_INTERNAL("errors.internal", "&4Es trat ein interner Fehler auf!"),
  ERR_INTPARSE("errors.intparse", "&cDie Eingabe %s ist keine Ganzzahl!"),
  ERR_NOT_ONLINE("errors.not_online", "&cDer Spieler %s ist nicht online!"),
  ERR_USAGE("errors.usage", "&7Benutzung: "),
  ERR_USAGE_COLOR_MANDATORY("errors.usage_color.mandatory", "&d"),
  ERR_USAGE_COLOR_OPTIONAL("errors.usage_color.optional", "&b"),
  ERR_USAGE_COLOR_BRACKETS("errors.usage_color.brackets", "&8"),
  ERR_USAGE_COLOR_OTHER("errors.usage_color.other", "&7"),
  ERR_NOT_A_PLAYER("errors.not_a_player", "&cDieser Befehl ist nur für Spieler zugänglich!"),
  ;

  private final String key, prefix;

  ConfigKey(final String key, final String prefix) {
    this.key = key;
    this.prefix = prefix;
  }

  @Override
  public String toString() {
    return this.key;
  }

  public String getDefaultValue() {
    return this.prefix;
  }
}
