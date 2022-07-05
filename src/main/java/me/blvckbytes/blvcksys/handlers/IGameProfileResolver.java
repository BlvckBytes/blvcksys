package me.blvckbytes.blvcksys.handlers;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/05/2022

  Public interfaces which the game profile resolver provides to other consumers.
 */
public interface IGameProfileResolver {

  /**
   * Resolve a full GameProfile by the owner's name
   * @param name Target owner's name
   * @param profile Profile callback, null if not resolvable
   */
  void resolve(String name, Consumer<@Nullable GameProfile> profile);

}
