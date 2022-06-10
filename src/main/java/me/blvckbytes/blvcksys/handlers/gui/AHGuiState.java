package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/09/2022

  Wraps the state of the AH GUI screen for each player individually.
*/
@AllArgsConstructor
public class AHGuiState {

  AuctionCategory cat;
  AuctionSort sort;
  @Nullable String search;

  public static AHGuiState makeDefault() {
    return new AHGuiState(AuctionCategory.ALL, AuctionSort.NEWEST, null);
  }
}
