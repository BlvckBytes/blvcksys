package me.blvckbytes.blvcksys.persistence.transformers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.models.InventoryModel;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Handles transforming bukkit inventories.
*/
@AutoConstruct
public class InventoryTransformer implements IDataTransformer<InventoryModel, Inventory> {

  private final ILogger logger;

  public InventoryTransformer(
    @AutoInject ILogger logger
  ) {
    this.logger = logger;
  }

  @Override
  public Inventory revive(InventoryModel data) {
    Inventory res;

    try {
      byte[] bytes = Base64Coder.decodeLines(data.getBase64Items());
      ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
      BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

      int streamLen = dataInput.readInt();
      Inventory inv = Bukkit.getServer().createInventory(null, nextNearestSize(streamLen), "");

      for (int i = 0; i < Math.min(streamLen, inv.getSize()); i++)
        inv.setItem(i, (ItemStack) dataInput.readObject());

      dataInput.close();
      inputStream.close();
      res = inv;
    } catch (Exception e) {
      logger.logError(e);
      res = null;
    }

    return res;
  }

  @Override
  public InventoryModel replace(Inventory data) {
    String base64;

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

      dataOutput.writeInt(data.getSize());
      for (int i = 0; i < data.getSize(); i++)
        dataOutput.writeObject(data.getItem(i));

      dataOutput.close();
      base64 = Base64Coder.encodeLines(outputStream.toByteArray());
      outputStream.close();
    } catch (Exception e) {
      logger.logError(e);
      base64 = "";
    }

    return new InventoryModel(base64);
  }

  @Override
  public Class<InventoryModel> getKnownClass() {
    return InventoryModel.class;
  }

  @Override
  public Class<Inventory> getForeignClass() {
    return Inventory.class;
  }

  /**
   * Get the next nearest (but bigger, limited to 54) inventory-size that's a
   * multiple of 9, in case the value isn't
   * @param size Size to make a multiple of 9
   * @return Next nearest multiple of 9
   */
  private int nextNearestSize(int size) {
    // Already within constraints
    if (size % 9 == 0 && size <= 54)
      return size;

    // Too large, cap at max
    if (size > 54)
      size = 54;

    // Round up to the next multiple of 9
    return (int) Math.round(Math.ceil((size + 9F) / 9F) * 9F);
  }
}
