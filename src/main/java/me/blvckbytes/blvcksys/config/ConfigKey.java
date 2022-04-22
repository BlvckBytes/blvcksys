package me.blvckbytes.blvcksys.config;

public enum ConfigKey {

  PREFIX("prefix", "&8[&5BVS&8]&r "),
  MSG_SENDER("msg.sender", "&8[&6Nachrichten&8] &8(&6Du &7-> &6%s&8)&7: %s"),
  MSG_RECEIVER("msg.receiver", "&8[&6Nachrichten&8] &8(&6%s&8 -> &6Dir&8)&7: %s"),
  MSG_NO_PARTNER("msg.no_partner", "&cDu hast keinen aktiven Nachrichten-Partner!"),
  MSG_SELF("msg.self", "&cDu kannst dir selbst keine Nachrichten schreiben!"),
  ERR_INTERNAL("errors.internal", "&4Es trat ein interner Fehler auf!"),
  ERR_INTPARSE("errors.intparse", "&cDie Eingabe %s ist keine Ganzzahl!"),
  ERR_NOT_ONLINE("errors.not_online", "&cDer Spieler %s ist nicht online!"),
  ERR_USAGE("errors.usage", "&7Benutzung: %s"),
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
