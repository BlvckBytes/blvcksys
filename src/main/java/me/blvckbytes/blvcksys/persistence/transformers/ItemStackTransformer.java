package me.blvckbytes.blvcksys.persistence.transformers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.models.ItemStackModel;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Handles transforming bukkit itemstacks.
*/
@AutoConstruct
public class ItemStackTransformer implements IDataTransformer<ItemStackModel, ItemStack> {

  private final ILogger logger;

  public ItemStackTransformer(
    @AutoInject ILogger logger
  ) {
    this.logger = logger;
  }

  @Override
  public ItemStack revive(ItemStackModel data) {
    if (data == null)
      return null;

    try {
      byte[] bytes = Base64Coder.decodeLines(data.getBase64Item());
      ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
      BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

      ItemStack ret = (ItemStack) dataInput.readObject();

      dataInput.close();
      inputStream.close();
      return ret;
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  @Override
  public ItemStackModel replace(ItemStack data) {
    if (data == null)
      return null;

    String base64;

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
      dataOutput.writeObject(data);
      dataOutput.close();
      base64 = Base64Coder.encodeLines(outputStream.toByteArray());
      outputStream.close();
    } catch (Exception e) {
      logger.logError(e);
      base64 = "";
    }

    return new ItemStackModel(base64);
  }

  @Override
  public Class<ItemStackModel> getKnownClass() {
    return ItemStackModel.class;
  }

  @Override
  public Class<ItemStack> getForeignClass() {
    return ItemStack.class;
  }
}
