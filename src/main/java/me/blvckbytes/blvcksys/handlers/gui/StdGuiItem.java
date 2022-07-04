package me.blvckbytes.blvcksys.handlers.gui;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Enumerates all available standard GUI items.
*/
public enum StdGuiItem {
  // Background item for backgrounds or borders
  BACKGROUND,

  // Free text search button
  SEARCH,

  // Back to previous GUI
  BACK,

  // Anvil GUI search item in first slot
  SEARCH_PLACEHOLDER,

  // New choice when in multiple choice GUI
  NEW_CHOICE,

  // Submit choices when in multiple choice GUI
  SUBMIT_CHOICES_ACTIVE,

  // Submit choices when in multiple choice GUI, button disabled
  SUBMIT_CHOICES_DISABLED,

  // Previous page of pagination
  PREV_PAGE,

  // Previous page of pagination, button disabled
  PREV_PAGE_DISABLED,

  // Next page of pagination
  NEXT_PAGE,

  // Next page of pagination, button disabled
  NEXT_PAGE_DISABLED,

  // Current page indicator of pagination
  PAGE_INDICATOR
}
