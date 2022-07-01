package me.blvckbytes.blvcksys.config;

import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/24/2022

  The base class of a configuration section value which is
  represented by a POJO model and can be parsed automatically.
*/
public abstract class AConfigSection {

  // Has to implement an empty constructor in order to allow
  // for default creation and field-patching afterwards
  public AConfigSection() {}

  /**
   * Called to decide the type of Object fields at runtime,
   * based on previously parsed values of that instance, as
   * it's patched one field at a time. Decideable fields are
   * always read last, so that they have access to other,
   * known type fields in order to decide properly.
   * @param field Target field in question
   * @return Decided type, Object.class means skip
   */
  public Class<?> runtimeDecide(String field) {
    return Object.class;
  }

  /**
   * Called when a field wasn't found within the config and a default could be set
   * @param type Target field's type
   * @param field Target field name
   * @return Value to use as a default
   */
  public @Nullable Object defaultFor(Class<?> type, String field) {
    return null;
  }
}
