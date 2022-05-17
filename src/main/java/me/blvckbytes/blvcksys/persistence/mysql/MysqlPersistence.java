package me.blvckbytes.blvcksys.persistence.mysql;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.di.IAutoConstructer;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.MigrationDefault;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.ModelNotFoundException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import me.blvckbytes.blvcksys.persistence.query.*;
import me.blvckbytes.blvcksys.persistence.transformers.IDataTransformer;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  An implementation of the persistence API towards the MariaDB database.
*/
@AutoConstruct(typeDependencies = { IDataTransformer.class })
public class MysqlPersistence implements IPersistence, IAutoConstructed {

  private final Map<Class<? extends APersistentModel>, MysqlTable> tables;
  private final List<IDataTransformer<?, ?>> transformers;
  private Connection conn;
  private String database;

  private final ILogger logger;
  private final MCReflect refl;
  private final JavaPlugin plugin;
  private final IAutoConstructer ac;
  private final IConfig cfg;

  public MysqlPersistence(
    @AutoInject ILogger logger,
    @AutoInject MCReflect refl,
    @AutoInject JavaPlugin plugin,
    @AutoInject IAutoConstructer ac,
    @AutoInject IConfig cfg
  ) throws Exception {
    this.logger = logger;
    this.refl = refl;
    this.plugin = plugin;
    this.ac = ac;
    this.cfg = cfg;

    this.transformers = new ArrayList<>();
    this.tables = new HashMap<>();

    connect();
    loadTransformers();
    parseAllTables();
    createAllTables();
  }

  //=========================================================================//
  //                                  API                                    //
  //=========================================================================//

  @Override
  public void store(APersistentModel model) throws PersistenceException {
    try {
      writeModel(model);
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public<T extends APersistentModel> List<T> list(Class<T> type) throws PersistenceException {
    try {
      MysqlTable table = getTableFromModel(type, false);
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM `" + table.name() + "`");

      logStatement(ps);

      ResultSet rs = ps.executeQuery();
      List<T> res = mapRows(type, rs);

      rs.close();
      ps.close();

      return res;
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public <T extends APersistentModel>void delete(Class<T> type, UUID id) throws PersistenceException {
    try {
      deleteModel(type, id);
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public <T extends APersistentModel> List<T> find(QueryBuilder<T> query) throws PersistenceException {
    try {
      PreparedStatement ps = buildQuery(query.getModel(), query, false, false, false);
      ResultSet rs = ps.executeQuery();
      List<T> res = mapRows(query.getModel(), rs);

      rs.close();
      ps.close();

      return res;
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public <T extends APersistentModel> int count(QueryBuilder<T> query) throws PersistenceException {
    try {
      PreparedStatement ps = buildQuery(query.getModel(), query, false, true, false);
      ResultSet rs = ps.executeQuery();

      if (!rs.next())
        return 0;

      int res = rs.getInt("count");

      rs.close();
      ps.close();

      return res;
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public <T extends APersistentModel> Optional<T> findFirst(QueryBuilder<T> query) throws PersistenceException {
    try {
      PreparedStatement ps = buildQuery(query.getModel(), query, true, false, false);
      ResultSet rs = ps.executeQuery();

      if (!rs.next())
        return Optional.empty();

      Optional<T> res = Optional.of(mapRow(query.getModel(), rs));

      rs.close();
      ps.close();

      return res;
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public <T extends APersistentModel> List<Map<String, Object>> findRaw(QueryBuilder<T> query, String... properties) {
    try {
      return readRowsRaw(query.getModel(), query, properties);
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public <T extends APersistentModel> List<Map<String, Object>> listRaw(Class<T> type, String... properties) {
    try {
      return readRowsRaw(type, null, properties);
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public void delete(APersistentModel model) throws PersistenceException {
    delete(model.getClass(), model.getId());
    refl.setFieldByName(model, "id", null);
  }

  @Override
  public <T extends APersistentModel> int delete(QueryBuilder<T> query) throws PersistenceException {
    try {
      PreparedStatement ps = buildQuery(query.getModel(), query, false, false, true);
      int ret = ps.executeUpdate();

      ps.close();
      return ret;
    } catch (PersistenceException e) {
      throw e;
    } catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public void cleanup() {
    this.disconnect();
  }

  @Override
  public void initialize() {}

  //=========================================================================//
  //                               Utilities                                 //
  //=========================================================================//

  ////////////////////////////////// Connection /////////////////////////////////////

  /**
   * Establish a connection to the database specified within the configuration file
   */
  private void connect() throws SQLException {
    String user = cfg.get(ConfigKey.DB_USERNAME).toString();
    String host = cfg.get(ConfigKey.DB_HOST).toString();
    String port = cfg.get(ConfigKey.DB_PORT).toString();
    database = cfg.get(ConfigKey.DB_DATABASE).toString();

    String resource = host + ":" + port + "/" + database;

    conn = DriverManager.getConnection(
      "jdbc:mysql://" + resource, user, cfg.get(ConfigKey.DB_PASSWORD).toString()
    );

    logger.logInfo("Connected to the Database using " + user + "@" + resource);
  }

  /**
   * Disconnect an active database connection
   */
  private void disconnect() {
    if (this.conn == null)
      return;

    try {
      this.conn.close();
      logger.logInfo("Disconnected from the database");
    } catch (SQLException e) {
      logger.logError(e);
    }
  }

  //////////////////////////////////// Tables ///////////////////////////////////////

  /**
   * Transform a db name representation (snake case)) into it's
   * model name representation (pascal case)
   * @param tableName Name to convert
   * @param capitalizeFirst Whether to capitalize the very first character
   * @return Converted name
   */
  private String dbNameToModelName(String tableName, boolean capitalizeFirst) {
    StringBuilder res = new StringBuilder();

    char[] chars = tableName.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];

      // Capitalize the first char if it's lowercase
      if (i == 0 && c >= 97 && c <= 122 && capitalizeFirst) {
        res.append(((char) (c - 32)));
        continue;
      }

      // Skip the underscore
      if (c == '_' && i != chars.length - 1) {

        // Two underscores in a row, marks a sub-model, leave as
        // is (and don't capitalize afterwards)
        if (chars[i + 1] == '_') {
          i++;
          res.append("__");
          continue;
        }

        char next = chars[++i];
        // Transform to uppercase
        res.append((char) (next - ((next >= 97 && next <= 122) ? 32 : 0)));
        continue;
      }

      // Append as is
      res.append(c);
    }

    return res.toString();
  }

  /**
   * Transform a model name (pascal case) into it's table name representation (snake
   * case), as MySQL doesn't support casing and this is the convention
   * @param modelName Name to convert
   * @return Converted name
   */
  private String modelNameToDBName(String modelName) {
    StringBuilder res = new StringBuilder();

    char[] chars = modelName.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];

      // Is an uppercase letter
      if (c >= 65 && c <= 90) {

        // Separate pascal casing using underscores
        if (i != 0)
          res.append('_');

        // Transform to lowercase
        res.append((char) (c + 32));
        continue;
      }

      // Append as is
      res.append(c);
    }

    return res.toString();
  }

  /**
   * Parses a table from it's model's metadata and adds it to the map. When a
   * unknown foreign field is encountered, the method tries to find a matching
   * transformer which will provide inlineable fields.
   * @param model Model to parse
   */
  private void parseTable(Class<? extends APersistentModel> model) throws Exception {
    // Table already parsed
    if (tables.containsKey(model))
      return;

    List<MysqlColumn> columns = new ArrayList<>();
    List<MysqlColumn> selfRefCols = new ArrayList<>();

    for (Field f : refl.findAllFields(model)) {

      // Skip non-model fields
      ModelProperty mp = f.getAnnotation(ModelProperty.class);
      if (mp == null)
        continue;

      // Skip non-inherited fields from superclasses
      if (!mp.isInherited() && f.getDeclaringClass() != model)
        continue;

      f.setAccessible(true);
      boolean isIdentifier = f.getName().equals("id");

      // Get the corresponding SQL type
      Optional<MysqlType> type = MysqlType.fromJavaType(
        f.getType(),
        // Unique fields or identifiers (primary key) require fixed-length datatypes
        mp.isUnique() || isIdentifier
      );

      // Could not find a matching SQL type directly
      if (type.isEmpty()) {

        // Try to resolve this field through a known transformer
        Optional<List<MysqlColumn>> transformed = inlineTransformedField(f, mp);
        if (transformed.isPresent()) {
          columns.addAll(transformed.get());
          continue;
        }

        throw new PersistenceException("Invalid type " + f.getType() + " for field " + f.getName() + " of " + model);
      }

      if (isIdentifier) {
        if (type.get() != MysqlType.UUID)
          throw new PersistenceException("Unsupported identifier type " + f.getType() + " for field " + f.getName() + " of " + model);

        columns.add(new MysqlColumn("id", type.get(), false, MigrationDefault.UNSPECIFIED, true, false, f, null, null, ForeignKeyAction.RESTRICT));
        continue;
      }

      MysqlTable foreignKey = null;
      Class<? extends APersistentModel> fkC = mp.foreignKey();

      // The foreign key type has been pointing at an abstraction, thus
      // just assume the current model as it's actual implementation
      if (fkC != APersistentModel.class && Modifier.isAbstract(fkC.getModifiers()))
        fkC = model;

      // Look up the foreign key table (if not self)
      // The base class's class is the "none" placeholder
      if (mp.foreignKey() != APersistentModel.class && fkC != model) {
        if (!tables.containsKey(fkC))
          parseTable(fkC);

        foreignKey = tables.get(fkC);

        if (foreignKey == null)
          throw new PersistenceException("Unknown foreign key class: " + fkC);
      }

      MysqlColumn col = new MysqlColumn(
        modelNameToDBName(f.getName()),
        type.get(), mp.isNullable(), mp.migrationDefault(), mp.isUnique(), mp.isInlineable(), f, null, foreignKey, mp.foreignChanges()
      );

      columns.add(col);

      // Add to a list to later update the self-ref foreign key field
      if (fkC == model)
        selfRefCols.add(col);
    }

    // No primary key field provided
    if (columns.stream().noneMatch(MysqlColumn::isPrimaryKey))
      throw new PersistenceException("Missing an identifier field in " + model);

    // Check if this model is used in combination with a registered transformer
    boolean isTransformer = false;
    for (IDataTransformer<?, ?> transformer : transformers) {
      if (!transformer.getKnownClass().equals(model))
        continue;

      isTransformer = true;
      break;
    }

    MysqlTable table = new MysqlTable(
      modelNameToDBName(model.getSimpleName()),
      columns,
      isTransformer
    );

    // Update private foreign key fields to self
    for (MysqlColumn selfRef : selfRefCols) {
      Field f = selfRef.getClass().getDeclaredField("foreignKey");
      f.setAccessible(true);
      f.set(selfRef, table);
    }

    // Register this table with it's model-class
    tables.put(model, table);
  }

  /**
   * Checks if a table already exists
   * @param table Table to check for
   * @return Existing state
   */
  private boolean isTableExisting(MysqlTable table) throws SQLException {
    PreparedStatement ps = conn.prepareStatement("SHOW TABLES LIKE '" + table.name() + "';");
    ResultSet rs = ps.executeQuery();
    boolean exists = rs.next();

    logStatement(ps);

    rs.close();
    ps.close();

    return exists;
  }

  /**
   * Migrates missing constraints like unique and foreign key, where constraints
   * are dropped if they don't appear in the local model and being created if they're
   * missing in the database
   * @param table Table to use as a diffing reference
   */
  private void migrateTableConstraints(MysqlTable table) throws SQLException {
    PreparedStatement ps = conn.prepareStatement("""
      SELECT
      a.CONSTRAINT_NAME,
      a.CONSTRAINT_TYPE,
      b.COLUMN_NAME,
      b.REFERENCED_TABLE_NAME
      FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS AS a
      LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS b
      ON a.CONSTRAINT_NAME = b.CONSTRAINT_NAME
      AND a.TABLE_NAME = b.TABLE_NAME
      WHERE a.TABLE_NAME = '%s'
      AND a.TABLE_SCHEMA = '%s';
      """.formatted(table.name(), database).stripIndent());

    // Mapping unique constraint names to their affected columns
    Map<String, List<String>> uniques = new HashMap<>();

    // Mapping foreign constraint names to (col, refTable)
    Map<String, Tuple<String, String>> foreigns = new HashMap<>();

    ResultSet rs = ps.executeQuery();
    while(rs.next()) {
      String type = rs.getString("CONSTRAINT_TYPE");
      String constrName = rs.getString("CONSTRAINT_NAME");
      String col = rs.getString("COLUMN_NAME");

      if (type.equals("UNIQUE")) {
        if (!uniques.containsKey(constrName))
          uniques.put(constrName, new ArrayList<>());
        uniques.get(constrName).add(col);
        continue;
      }

      if (type.equals("FOREIGN KEY")) {
        String refTable = rs.getString("REFERENCED_TABLE_NAME");
        foreigns.put(constrName, new Tuple<>(col, refTable));
        continue;
      }
    }

    List<String> uniqueModelCols = table.columns().stream()
      .filter(c -> !c.isPrimaryKey())
      .filter(MysqlColumn::isUnique)
      .map(MysqlColumn::getName)
      .collect(Collectors.toList());

    boolean dropUniConstrs = (
      // There are only obsolete unique constraints, drop all
      uniqueModelCols.size() == 0 && uniques.size() > 0 ||
      // This system will always only have a single unique constraint
      uniques.size() != 1
    );

    // If nothing matched to drop until now, check if the existing (first) constraint
    // is really containing all unique columns from the model
    if (!dropUniConstrs) {
      List<String> uniqueConstrCols = uniques.get(uniques.keySet().iterator().next());

      if (uniqueConstrCols != null) {
        Collections.sort(uniqueConstrCols);
        Collections.sort(uniqueModelCols);
        // The existing constraint didn't provide all unique model columns
        dropUniConstrs = !uniqueConstrCols.equals(uniqueModelCols);
      }
    }

    if (dropUniConstrs) {
      // Drop all unique constraints
      for (String constrName : uniques.keySet()) {
        PreparedStatement ps2 = this.conn.prepareStatement("ALTER TABLE `" + table.name() + "` DROP INDEX " + constrName + ";");
        logStatement(ps2);
        ps2.executeUpdate();
        ps2.close();
      }

      if (uniques.size() > 0)
        logger.logDebug("Dropped " + uniques.size() + " unique constraint from " + table.name());

      // Create a single unique constraint containing all unique columns
      if (uniqueModelCols.size() > 0) {
        PreparedStatement ps2 = this.conn.prepareStatement("ALTER TABLE `" + table.name() + "` ADD " + buildUniqueConstraint(table));
        logStatement(ps2);
        ps2.executeUpdate();
        ps2.close();
        logger.logDebug("Created unique constraint on " + table.name());
      }
    }

    List<Tuple<String, String>> foreignModelCols = table.columns().stream()
      .filter(c -> c.getForeignKey() != null)
      .map(c -> new Tuple<>(c.getName(), c.getForeignKey().name()))
      .collect(Collectors.toList());

    // Loop all existing foreign key columns
    for (Map.Entry<String, Tuple<String, String>> foreignConstr : foreigns.entrySet()) {
      Tuple<String, String> tableForeign = foreignConstr.getValue();

      // Check if the model has this foreign key
      boolean modelHas = false;
      for (Iterator<Tuple<String, String>> foreignModelIt = foreignModelCols.listIterator(); foreignModelIt.hasNext();) {
        Tuple<String, String> modelForeign = foreignModelIt.next();

        // If so, remove it from the local list
        if (modelForeign.a().equals(tableForeign.a()) && modelForeign.b().equals(tableForeign.b())) {
          foreignModelIt.remove();
          modelHas = true;
          break;
        }
      }

      // The model doesn't have this foreign key, drop it
      if (!modelHas) {
        PreparedStatement ps2 = conn.prepareStatement("ALTER TABLE `" + table.name() + "` DROP FOREIGN KEY " + foreignConstr.getKey() + ";");
        logStatement(ps2);
        ps2.executeUpdate();
        ps2.close();
        logger.logDebug("Dropped foreign key for column " + tableForeign.b() + " for " + table.name());
      }
    }

    // All still remaining columns weren't found in db and thus need to be created
    for (Tuple<String, String> foreignCol : foreignModelCols) {
      PreparedStatement ps2 = conn.prepareStatement("ALTER TABLE `" + table.name() + "` ADD FOREIGN KEY (`" + foreignCol.a() + "`) REFERENCES `" + foreignCol.b() + "`(`id`);");
      logStatement(ps2);
      ps2.executeUpdate();
      ps2.close();
      logger.logDebug("Created foreign key on " + table.name() + " from " + foreignCol.a() + " to " + foreignCol.b() + "(id)");
    }

    rs.close();
    ps.close();
  }

  /**
   * Migrates any missing table columns by adding them with their default value
   * or alter existing columns that differ from what's specified in the model
   * @param table Table to migrate
   */
  private void migrateTableColumns(MysqlTable table) throws SQLException {
    PreparedStatement ps = conn.prepareStatement("DESC `" + table.name() + "`;");
    logStatement(ps);

    List<String> foundCols = new ArrayList<>();
    ResultSet rs = ps.executeQuery();

    while (rs.next()) {
      String name = rs.getString("Field");
      foundCols.add(name);

      // Find the model's corresponding column type
      Optional<MysqlColumn> modelCol = table.columns()
          .stream()
          .filter(col -> col.getName().equals(name))
          .findFirst();

      // Column exists in the database but is not known as a model property
      // Just keep it and skip
      if (modelCol.isEmpty())
        continue;

      MysqlColumn col = modelCol.get();

      // Primary key columns are never altered
      if (col.isPrimaryKey())
        continue;

      // Stringify "null" defaults
      String def = rs.getString("Default");
      if (def == null)
        def = "null";

      // Migrate type or null-ness mismatch
      if (
        !col.getType().matchesSQLTypeStr(rs.getString("Type")) ||
        col.isNullable() != rs.getString("Null").equalsIgnoreCase("yes")
      ) {
        String newSig = buildColumnSignature(col, true);
        PreparedStatement uPs = conn.prepareStatement(
          "ALTER TABLE `" + table.name() + "` MODIFY " + newSig + ";"
        );

        logStatement(uPs);
        uPs.executeUpdate();
        uPs.close();

        logger.logInfo("Migrated column " + col.getName() + " of " + table.name() + " type to \"" + newSig + "\"");
      }

      // Migrate mismatching column default
      if (
        col.getMigrationDefault() != MigrationDefault.UNSPECIFIED &&
        !col.getMigrationDefault().matchesSqlValue(def)
      ) {
        if (col.getMigrationDefault() == MigrationDefault.NULL && !col.isNullable())
          throw new PersistenceException("Non-nullable column " + col.getName() + " of " + table.name() + " cannot have a null migration default");

        if (!col.getMigrationDefault().matchesSqlType(col.getType()))
          throw new PersistenceException("Column " + col.getName() + " of " + table.name() + " uses a type-mismatching migration default");

        PreparedStatement uPs = conn.prepareStatement(
          "ALTER TABLE `" + table.name() +
          "` ALTER `" + col.getName() + "` " +
          "SET DEFAULT " + col.getMigrationDefault().getSqlValues()[0] + ";"
        );

        logStatement(uPs);
        uPs.executeUpdate();
        uPs.close();

        logger.logInfo("Migrated column " + col.getName() + " of " + table.name() + " default to " + col.getMigrationDefault());
      }
    }

    // Create missing columns
    for (MysqlColumn col : table.columns()) {
      if (foundCols.contains(col.getName()))
        continue;

      PreparedStatement uPs = conn.prepareStatement(
        "ALTER TABLE `" + table.name() + "` ADD " + buildColumnSignature(col, false) + ";"
      );

      logStatement(uPs);
      uPs.executeUpdate();
      uPs.close();

      logger.logInfo("Created missing column " + col.getName() + " of " + table.name());
    }

    // Migrate missing / out-of-date constraints
    migrateTableConstraints(table);

    rs.close();
    ps.close();
  }

  /**
   * Build a column's signature from it's properties
   * @param column Column to build for
   * @param isModify Whether the result will be used in a column modification statement
   * @return Built signature
   */
  private String buildColumnSignature(MysqlColumn column, boolean isModify) {
    String foreignAction = "";
    if (column.getForeignAction() == ForeignKeyAction.DELETE_CASCADE)
      foreignAction = "ON DELETE CASCADE";
    else if (column.getForeignAction() == ForeignKeyAction.SET_NULL)
      foreignAction = "ON DELETE SET NULL";
    else if (column.getForeignAction() == ForeignKeyAction.RESTRICT)
      foreignAction = "ON DELETE RESTRICT";

    String ret = "`" + column.getName() + "`" + " " +
      // Type
      column.getType() + " " +

      // Nullability
      (column.isNullable() ? "NULL" : "NOT NULL") + " " +

      // Primary key
      (column.isPrimaryKey() ? "PRIMARY KEY " : "") +

      // Column default
      (column.getMigrationDefault() != MigrationDefault.UNSPECIFIED ? "DEFAULT " + column.getMigrationDefault() : "");

    // Also append the foreign key constraint in a comma separated instruction
    if (column.getForeignKey() != null && !isModify)
      ret += ", FOREIGN KEY (`" + column.getName() + "`) REFERENCES `" + column.getForeignKey().name() + "`(`id`) " + foreignAction;

    return ret.trim();
  }

  /**
   * Build a unique constraint which includes all unique columns of a table
   * @param table Table who's columns to use
   * @return Unique constraint, null if there's no unique column
   */
  private String buildUniqueConstraint(MysqlTable table) {
    if (table.columns().stream().noneMatch(MysqlColumn::isUnique))
      return null;

    List<String> tarColNames = table.columns().stream()
      .filter(MysqlColumn::isUnique)
      .filter(col -> !col.isPrimaryKey())
      .map(MysqlColumn::getName)
      .toList();

    // Triple underscore separates columns, since the single underscore
    // is reserved for casing and the dual for inlining columns
    String name = String.join("___", tarColNames);

    return "UNIQUE " + name + " (" + tarColNames.stream()
      .collect(Collectors.joining("`, `", "`", "`")) + ")";
  }

  /**
   * Dispatches a table creation statement if the table doesn't yet exist
   * @param table Table to create
   */
  private void createTableIfNotExists(MysqlTable table) throws SQLException {
    if (isTableExisting(table)) {
      migrateTableColumns(table);
      return;
    }

    StringBuilder stmt = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + table.name() + "`(");

    List<MysqlColumn> columns = table.columns();
    for (int i = 0; i < columns.size(); i++) {
      MysqlColumn col = columns.get(i);
      stmt
        .append(buildColumnSignature(col, false))
        .append(i == columns.size() - 1 ? "" : ", ");
    }

    String uniqueConstr = buildUniqueConstraint(table);
    if (uniqueConstr != null)
      stmt.append(", ").append(uniqueConstr);

    stmt.append(");");
    PreparedStatement ps = this.conn.prepareStatement(stmt.toString());
    logStatement(ps);

    ps.executeUpdate();
    ps.close();

    logger.logInfo("Created table " + table.name());
  }

  /**
   * Create all known tables which are not used for
   * transformers (as they're always inlined)
   */
  private void createAllTables() throws SQLException {
    for (MysqlTable table : tables.values()) {
      // Don't create inlined transformer tables
      if (!table.isTransformer())
        createTableIfNotExists(table);
    }
  }

  /**
   * Parse all tables by their meta-information into the required data-structure
   */
  private void parseAllTables() throws Exception {
    String pkg = getClass().getPackageName();

    // Look up all classes that are marked as models in ../models
    List<? extends Class<? extends APersistentModel>> models = refl.findImplClasses(
      plugin,
      pkg.substring(0, pkg.lastIndexOf('.')) + ".models",
      APersistentModel.class
    );

    // Parse them into tables
    for (Class<? extends APersistentModel> model : models) {

      // Don't create abstract class tables
      if (Modifier.isAbstract(model.getModifiers()))
        continue;

      parseTable(model);
    }
  }

  ///////////////////////////////// Query Builder /////////////////////////////////////

  /**
   * Get a table's column by it's name, if the name is null, the column will be null
   * @param table Table containing the column
   * @param name Name of the target column
   */
  private MysqlColumn getColumnByName(MysqlTable table, String name) {
    if (name == null)
      return null;

    return table.columns().stream()
      .filter(
        c -> dbNameToModelName(c.getName(), false).equals(name)
      )
      .findFirst()
      .orElseThrow(() -> new RuntimeException(
        "The query field " + name + " is not a member of the model " + dbNameToModelName(table.name(), true)
      ));
  }

  /**
   * Validate that a query field type is valid
   * @param col Column which specifies the type
   * @param field Name of the field, used for exceptions
   * @param value Value that has to conform to the specified type
   * @param isInFieldOp Whether this field is inside of a field operation
   * @param isWildcard Whether this field is used with a wildcard equality operation
   */
  private void validateQueryFieldType(
    MysqlColumn col,
    String field,
    Object value,
    boolean isInFieldOp,
    boolean isWildcard
  ) {
    // UUIDs are allowed to be compared with strings when using wildcard equality
    if (isWildcard && col.getType() == MysqlType.UUID && value.getClass() == String.class)
      return;

    boolean anyMatch = false;
    Class<?> queryType = value.getClass();

    Class<?>[] validTypes = isInFieldOp ? col.getType().getJavaEquivalentsForFieldOps() : col.getType().getJavaEquivalents();
    for (Class<?> targType : validTypes) {
      if (targType == queryType) {
        anyMatch = true;
        break;
      }
    }

    if (!anyMatch)
      throw new RuntimeException("The query field " + field + " is of invalid type " + queryType);
  }

  /**
   * Wrap a column name by a transformating function in order
   * to make it compatible with simple field operations
   * @param column Column to wrap
   * @return Column name wrapped in a transformating function, if necessary
   */
  private String wrapColumnForOp(MysqlColumn column) {
    return switch (column.getType()) {
      case DATETIME -> "UNIX_TIMESTAMP(`" + column.getName() + "`)";
      case INTEGER, DOUBLE, FLOAT -> "`" + column.getName() + "`";
      default -> throw new PersistenceException("The type " + column.getType() + " doesn't support field operations");
    };
  }

  /**
   * Stringify a field query to a partial statement, for example:
   * field=test, op=EQ, value=5 would yield: `test` == ? and add (INTEGER, 5) to params
   * @param query Query to stringify
   * @param table Table which this field has to be a member of
   * @param params Modifyable list of parameters to add the value parameter
   * @return Stringified query
   */
  private String stringifyFieldQuery(FieldQuery query, MysqlTable table, List<Tuple<MysqlType, Object>> params) {
    MysqlColumn targColA = getColumnByName(table, query.getFieldA());
    MysqlColumn targColB = getColumnByName(table, query.getFieldB());

    boolean isNull = query.getValue() == null;

    // Check if this will be a wildcard operation
    boolean isWildcard =
      query.getEqOp() == EqualityOperation.CONT ||
      query.getEqOp() == EqualityOperation.CONT_IC ||
      query.getEqOp() == EqualityOperation.STARTS ||
      query.getEqOp() == EqualityOperation.STARTS_IC ||
      query.getEqOp() == EqualityOperation.ENDS ||
      query.getEqOp() == EqualityOperation.ENDS_IC;

    // Validate that the column's types are compatible with the value's java type
    if (!isNull) {
      validateQueryFieldType(targColA, query.getFieldA(), query.getValue(), targColB != null, isWildcard);

      if (targColB != null)
        validateQueryFieldType(targColB, query.getFieldB(), query.getValue(), true, isWildcard);
    }

    if (!targColA.getType().supportsOp(query.getEqOp()))
      throw new PersistenceException("The query field " + query.getFieldA() + " does not support the operation " + query.getEqOp());

    if (targColB != null && !targColB.getType().supportsOp(query.getEqOp()))
      throw new PersistenceException("The query field " + query.getFieldB() + " does not support the operation " + query.getEqOp());

    String ph = "?";
    String fieldExpr;

    // When using operators, wrap columns with transformation functions (if necessary)
    if (query.getFieldOp() != null && targColB != null)
      fieldExpr = wrapColumnForOp(targColA) + " " + query.getFieldOp() + " " + wrapColumnForOp(targColB);
    else
      fieldExpr = "`" + targColA.getName() + "`";

    Object value = query.getValue();

    if (isWildcard) {
      // UUIDs need to be "stringified" to allow for wildcard OPs
      // When turning the binary columns to hex, there are no dashes, thus
      // strip all dashes off the value's UUID
      if (targColA.getType() == MysqlType.UUID) {
        fieldExpr = "HEX(`" + targColA.getName() + "`)";
        value = value.toString().replace("-", "");
      }

      // Escape all reserved wildcard characters using bang as an escape character
      value = value.toString()
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_")
        .replace("[", "![");

      // Contains (any<value>any)
      if (query.getEqOp() == EqualityOperation.CONT || query.getEqOp() == EqualityOperation.CONT_IC)
        value = "%" + value + "%";

      // Starts with (<value>any)
      if (query.getEqOp() == EqualityOperation.STARTS || query.getEqOp() == EqualityOperation.STARTS_IC)
        value = value + "%";

      // Ends with (any<value>)
      if (query.getEqOp() == EqualityOperation.ENDS || query.getEqOp() == EqualityOperation.ENDS_IC)
        value = "%" + value;
    }

    // If it's not a wildcard query: UUIDs need to be converted to binary
    else if (targColA.getType().equals(MysqlType.UUID))
      ph = uuidToBin("?", false);

    // Only add placeholder values if there actually was a placeholder appended
    if (!(isNull && (query.getEqOp() == EqualityOperation.EQ || query.getEqOp() == EqualityOperation.NEQ)))
      params.add(new Tuple<>(targColA.getType(), value));

    return switch (query.getEqOp()) {
      case EQ -> fieldExpr + " " + (isNull ? "IS NULL" : "= " + ph);
      case NEQ -> fieldExpr + " " + (isNull ? "IS NOT NULL" : "!= " + ph);
      case CONT, STARTS, ENDS -> fieldExpr + " LIKE " + ph + " ESCAPE '!'";
      case CONT_IC, STARTS_IC, ENDS_IC -> "LOWER(" + fieldExpr + ") LIKE LOWER(" + ph + ") ESCAPE '!'";
      case EQ_IC -> "LOWER(" + fieldExpr + ") = LOWER(" + ph + ")";
      case NEQ_IC -> "LOWER(" + fieldExpr + ") != LOWER(" + ph + ")";
      case LT -> fieldExpr + " < " + ph;
      case LTE -> fieldExpr + " <= " + ph;
      case GT -> fieldExpr + " > " + ph;
      case GTE -> fieldExpr + " >= " + ph;
    };
  }

  /**
   * Stringify a field query group to a partial statement, for example:
   * root=(field=test, op=EQ, value=5) additionals=[(AND, (field=test2, op=NEQ, value=10))] would yield:
   * (`test` == ? AND test2 != ?) and add (INTEGER, 5), (INTEGER, 10) to params
   * @param group Query group to stringify
   * @param table Table which this field has to be a member of
   * @param params Modifyable list of parameters to add the value parameter
   * @return Stringified query group
   */
  private String stringifyFieldQueryGroup(FieldQueryGroup group, MysqlTable table, List<Tuple<MysqlType, Object>> params) {
    StringBuilder groupStr = new StringBuilder("(");

    // Append the root (first entry with no connection prefix)
    groupStr.append(stringifyFieldQuery(group.getRoot(), table, params));

    // Append all additional queries with their connection leading them
    for (Tuple<QueryConnection, FieldQuery> additional : group.getAdditionals()) {
      groupStr.append(" ").append(additional.a()).append(" ");
      groupStr.append(stringifyFieldQuery(additional.b(), table, params));
    }

    return groupStr + ")";
  }

  /**
   * Stringify a sorting table (mapping field name to sorting direction), for example:
   * fieldA ASC, fieldB DESC
   * @param table Table which the sorting fields are members of
   * @param columns Columns and their sorting direction
   * @return Stringified order by clause content
   */
  private String stringifySorting(MysqlTable table, Map<String, Boolean> columns) {
    StringBuilder sortStr = new StringBuilder();

    int i = 0;
    for (Map.Entry<String, Boolean> column : columns.entrySet()) {
      MysqlColumn col = getColumnByName(table, column.getKey());
      sortStr.append("`").append(col.getName()).append("` ")
        .append(column.getValue() ? "ASC" : "DESC")
        .append(i++ != columns.size() - 1 ? ", " : "");
    }

    return sortStr.toString();
  }

  /**
   * Build a selecting query from a query builder's state
   * @param query Query builder to build from, leave at null to have no WHERE clause
   * @param onlyFirst Whether to only query for the first result
   * @param onlyCount Whether to only count the number of results instead of fetching the actual data
   * @param fields Fields to select, leave empty to select everything
   * @return Built query statement with all parameters applied
   */
  private<T extends APersistentModel> PreparedStatement buildQuery(
    Class<T> model,
    @Nullable QueryBuilder<?> query,
    boolean onlyFirst,
    boolean onlyCount,
    boolean delete,
    String ...fields
  ) throws Exception {
    MysqlTable table = getTableFromModel(model, false);

    StringBuilder stmt = new StringBuilder();

    // Selection mode
    if (!delete) {
      stmt.append("SELECT ");

      if (onlyCount)
        stmt.append("COUNT(*) AS `count`");

      else if (fields.length == 0)
        stmt.append("*");

      else {
        for (int i = 0; i < fields.length; i++) {
          String field = fields[i];
          String name = getColumnByName(table, field).getName();
          stmt.append("`").append(name).append("`").append(i != fields.length - 1 ? ", " : "");
        }
      }
    }

    // Deletion mode
    else
      stmt.append("DELETE");

    stmt.append(" FROM `").append(table.name()).append("`");
    List<Tuple<MysqlType, Object>> params = new ArrayList<>();

    if (query != null) {

      stmt.append(" WHERE ");

      if (query.getRoot() == null)
        throw new PersistenceException("Cannot perform an empty query");

      stmt.append(stringifyFieldQueryGroup(query.getRoot(), table, params));

      // Append all additional query groups with their connection leading them
      for (Tuple<QueryConnection, FieldQueryGroup> additional : query.getAdditionals()) {
        stmt.append(" ").append(additional.a()).append(" ");
        stmt.append(stringifyFieldQueryGroup(additional.b(), table, params));
      }

      // Only append limit/offset and ordering when reading
      if (!delete) {
        if (query.getLimit() != null || onlyFirst)
          stmt.append(" LIMIT ").append(onlyFirst ? 1 : query.getLimit());

        if (query.getSkip() != null)
          stmt.append(" OFFSET ").append(query.getSkip());

        if (query.getSorting().size() > 0) {
          stmt.append(" ORDER BY ");
          stmt.append(stringifySorting(table, query.getSorting()));
        }
      }
    }

    PreparedStatement ps = conn.prepareStatement(stmt + ";");

    int i = 0;
    for (Tuple<MysqlType, Object> param : params)
      ps.setObject(++i, translateValue(param.a(), param.b()));

    logStatement(ps);

    return ps;
  }

  ////////////////////////////////// Raw Reading //////////////////////////////////////

  /**
   * Read a ResultSet's rows of data as raw k-v pairs and collect these maps into a list
   * @param model Model used to represent the individual result rows
   * @param query Query builder to build from, leave at null to have no WHERE clause
   * @param properties Properties to select
   * @return List of raw k-v pairs, as many as available rows
   */
  private<T extends APersistentModel> List<Map<String, Object>> readRowsRaw(
    Class<T> model,
    @Nullable QueryBuilder<T> query,
    String[] properties
  ) throws Exception {
    List<Map<String, Object>> res = new ArrayList<>();
    MysqlTable table = getTableFromModel(model, false);

    PreparedStatement ps = buildQuery(model, query, false, false, false, properties);
    ResultSet rs = ps.executeQuery();

    List<String> colNames = Arrays.stream(properties)
      .map(this::modelNameToDBName)
      .toList();

    while(rs.next())
      res.add(readRowRaw(table, rs, colNames));

    rs.close();
    ps.close();

    return res;
  }

  /**
   * Read an individual row (the one currently selected by the ResultSet's
   * cursor) into a raw k-v map
   * @param table Table of the model reading from
   * @param rs ResultSet containing the row to be mapped
   * @param columns Selected columns that this ResultSet contains
   * @return Model with fields containing the row's data
   */
  private Map<String, Object> readRowRaw(MysqlTable table, ResultSet rs, List<String> columns) throws SQLException {
   Map<String, Object> res = new HashMap<>();

   for (String property : columns) {
     MysqlColumn matchingCol = table.columns().stream()
       .filter(col -> col.getName().equals(property))
       .findFirst()
       .orElseThrow(() -> new PersistenceException("Invalid column for reading raw: " + property));

     res.put(property, translateValue(matchingCol.getType(), rs.getObject(property)));
   }

   return res;
  }

  ////////////////////////////////// Row Mapping //////////////////////////////////////

  /**
   * Create a new empty instance of a model by invoking it's hidden default constructor
   * @param model Model to instantiate
   * @return Instantiated model
   */
  private<T extends APersistentModel> T newEmpty(Class<T> model) throws Exception {
    Constructor<T> ctor;
    try {
      ctor = model.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new PersistenceException("Model " + model + " provides no empty constructor");
    }

    // Create a new empty object from the hidden empty constructor
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  /**
   * Translate a value into the corresponding used java-type as
   * specified by the enum {@link MysqlType}
   * @param type Type of the value to translate
   * @param value Column value
   * @return Transformed value
   */
  private Object translateValue(MysqlType type, Object value) {
    // Leave null values as they are
    if (value == null)
      return null;

    if (type == MysqlType.UUID) {
      // Turn byte[]'s (binary columns) into UUIDs when reading
      if (value instanceof byte[] ba) {
        ByteBuffer bb = ByteBuffer.wrap(ba);
        value = new UUID(bb.getLong(), bb.getLong());
      }

      // Stringify UUIDs when writing
      else
        value = value.toString();
    }

    // Turn the driver's LocalDateTime into java's default Date
    else if (type == MysqlType.DATETIME) {
      if (value instanceof LocalDateTime ldt)
        value = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Value shouldn't need any transformation
    return value;
  }

  /**
   * Maps an individual row (the one currently selected by the ResultSet's
   * cursor) into it's corresponding model
   * @param model Model used to represent the row of data
   * @param rs ResultSet containing the row to be mapped
   * @return Model with fields containing the row's data
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private<T extends APersistentModel> T mapRow(
    Class<T> model,
    ResultSet rs
  ) throws Exception {
    MysqlTable table = getTableFromModel(model, false);
    List<MysqlColumn> remainingColumns = new ArrayList<>(table.columns());
    T inst = newEmpty(model);

    // Loop while there are still columns left to be mapped
    while (remainingColumns.size() > 0) {
      MysqlColumn col = remainingColumns.get(0);
      Field knownField = col.getKnownModelField();

      // This column requires a transformer
      if (knownField != null) {
        // Create a new instance of the transformed field's declaring class
        Class<? extends APersistentModel> knownModel = (Class<? extends APersistentModel>) knownField.getDeclaringClass();
        MysqlTable knownTable = getTableFromModel(knownModel, true);
        Object knownInst = newEmpty(knownModel);
        boolean knownHasNullFields = false;

        // Loop all columns from the known table and set the known instance's fields accordingly
        for (MysqlColumn knownCol : knownTable.columns()) {
          MysqlColumn targRemCol = remainingColumns.stream()
            .filter(rc ->
              rc.getKnownModelField() != null &&
              rc.getKnownModelField().equals(knownCol.getModelField())
            ).findFirst().orElse(null);

          // This column doesn't seem to be mapped
          if (targRemCol == null)
            continue;

          // Directly set the known model's field value to the corresponding column's value
          Object value = translateValue(col.getType(), rs.getObject(targRemCol.getName()));

          if (value == null)
            knownHasNullFields = true;

          knownCol.getModelField().set(knownInst, value);
          remainingColumns.remove(targRemCol);
        }

        // If any of the transformed field's is null, the whole known model becomes null
        if (knownHasNullFields)
          col.getModelField().set (inst, null);

        // Call the reviver on this known model to receive the foreign value to write to the row's model
        else {
          IDataTransformer<?, ?> dt = getTransformerByKnownField(knownField);
          col.getModelField().set(inst, callTransformerRevive(dt, knownInst));
        }

        continue;
      }

      Object value = rs.getObject(col.getName());
      Class<?> fieldType = col.getModelField().getType();

      // Revive enum fields
      if (fieldType.isEnum() && value instanceof String valS)
        value = Enum.valueOf((Class<Enum>) fieldType, valS);

      // Directly set the model's field value to the corresponding column's value
      col.getModelField().set(inst, translateValue(col.getType(), value));
      remainingColumns.remove(col);
    }

    return inst;
  }

  /**
   * Map a ResultSet's rows of data to the corresponding models and use transformers where necessary
   * @param model Model used to represent the individual result rows
   * @param rs ResultSet containing all the rows
   * @return List of models, as many as available rows
   */
  private<T extends APersistentModel> List<T> mapRows(Class<T> model, ResultSet rs) throws Exception {
    List<T> res = new ArrayList<>();

    while (rs.next())
      res.add(mapRow(model, rs));

    return res;
  }

  //////////////////////////////////// Deletion ////////////////////////////////////////

  /**
   * Delete an existing modelfrom the database by it's ID
   * @param id ID of the model
   */
  private void deleteModel(Class<? extends APersistentModel> type, UUID id) throws Exception {
    MysqlTable table = getTableFromModel(type, false);
    String idStr = id == null ? null : id.toString();

    PreparedStatement ps = conn.prepareStatement(
      "DELETE FROM `" + table.name() + "` WHERE `id` = " + uuidToBin(id) + ";"
    );

    logStatement(ps);

    if (ps.executeUpdate() == 0)
      throw new ModelNotFoundException(dbNameToModelName(table.name(), true), idStr);

    ps.close();
  }

  //////////////////////////////////// Writing ////////////////////////////////////////

  /**
   * Tries to resolve a transformed column into it's value after the
   * transformation has been applied
   * @param column Column to be transformed
   * @param model Model containing the data to transform in one of the fields
   * @param replaceCache Writeable cache used to store replace() results in and access
   *                     them over the (possibly) multiple field accesses of a transformed result
   * @return Transformed object on transformed columns, column's vanilla value otherwise
   */
  private Object resolveColumnValue(
    MysqlColumn column,
    APersistentModel model,
    Map<String, Object> replaceCache
  ) throws Exception {
    Object value = column.getModelField().get(model);
    Field knownModelField = column.getKnownModelField();
    IDataTransformer<?, ?> transformer = getTransformerByKnownField(knownModelField);

    // This column is not bein transformed
    if (knownModelField == null || transformer == null)
      return value;

    // Format: <field>__<inlined_field>
    String fieldName = column.getName().split("__")[0];

    // Only compute the replace if there's no cache entry yet
    Object replaced = replaceCache.get(fieldName);
    if (!replaceCache.containsKey(fieldName)) {
      replaced = callTransformerReplace(transformer, value);
      replaceCache.put(fieldName, replaced);
    }

    // The replacer returned null, thus all of it's fields are null
    if (replaced == null)
      return null;

    // Get the transformer column's value from the model the value just got replaced into
    return knownModelField.get(replaced);
  }

  /**
   * Check for duplicate keys of a model's unique columns and throw an exception on occurrence
   * @param model Model to check for
   * @param table Table that corresponds to this model
   * @param replaceCache Writeable cache used to store replace() results in and access
   *                     them over the (possibly) multiple field accesses of a transformed result
   */
  private void checkDuplicateKeys(
    APersistentModel model,
    MysqlTable table,
    Map<String, Object> replaceCache
  ) throws Exception {

    QueryBuilder<?> query = new QueryBuilder<>(model.getClass());
    List<Tuple<String, Object>> uniqueVals = new ArrayList<>();

    // Loop all unique keys
    for (MysqlColumn column : table.columns()) {
      if (column.isPrimaryKey() || !column.isUnique())
        continue;

      // String columns that are unique are always ignoring the casing
      EqualityOperation op = EqualityOperation.EQ;
      if (column.getType() == MysqlType.VARCHAR || column.getType() == MysqlType.TEXT)
        op = EqualityOperation.EQ_IC;

      Object value = resolveColumnValue(column, model, replaceCache);
      query.and(column.getName(), op, value);
      uniqueVals.add(new Tuple<>(column.getName(), value));
    }

    // And is not self
    query.and("id", EqualityOperation.NEQ, model.getId());

    // There's already a column with this unique field
    if (count(query) > 0)
      throw new DuplicatePropertyException(dbNameToModelName(table.name(), true), uniqueVals);
  }

  /**
   * Conversion of a UUID (dash-separated) to a 16 byte binary number
   * @param u UUID value
   * @return Conversion instruction
   */
  private String uuidToBin(@Nullable UUID u) {
    return uuidToBin((u == null ? "null" : u.toString()), true);
  }

  /**
   * Conversion of a UUID (dash-separated) to a 16 byte binary number
   * @param value String value to pass to the conversion function
   * @param immediate Whether this is an immediate value and no placeholder (?)
   * @return Conversion instruction
   */
  private String uuidToBin(String value, boolean immediate) {
    String im = immediate ? "'" : "";
    return "UNHEX(REPLACE(" + im + value + im + ", \"-\", \"\"))";
  }

  /**
   * Get a model's corresponding parsed table
   * @param model Model class
   * @param allowTransformers Whether to allow transformer models
   * @return Table instance
   * @throws PersistenceException Model not known
   */
  private MysqlTable getTableFromModel(Class<?> model, boolean allowTransformers) throws PersistenceException {
    MysqlTable table = tables.get(model);

    if (table == null)
      throw new PersistenceException("The model " + model.getSimpleName() + " is not registered!");

    if (!allowTransformers && table.isTransformer())
      throw new PersistenceException("Cannot directly write transformers: " + model.getSimpleName());

    return table;
  }

  /**
   * Write a model into the database and set it's auto-generated fields
   * @param model Model to write
   */
  private void writeModel(APersistentModel model) throws Exception {
    MysqlTable table = getTableFromModel(model.getClass(), false);

    // Ensure that there are no duplicate keys
    Map<String, Object> replaceCache = new HashMap<>();
    checkDuplicateKeys(model, table, replaceCache);

    boolean isInsert = model.getId() == null;
    List<MysqlColumn> columns = table.columns();

    StringBuilder stmt = new StringBuilder(
      isInsert ? "INSERT INTO `" + table.name() + "` (" : "UPDATE `" + table.name() + "` SET "
    );

    // Append a list of column names, in the order they appear in the list
    for (int i = 0; i < columns.size(); i++) {
      MysqlColumn column = columns.get(i);

      // Primary keys are never updated
      if (!isInsert && column.isPrimaryKey())
        continue;

      stmt
        .append("`")
        .append(column.getName())
        .append("`");

      // Also add a placeholder when updating
      if (!isInsert) {
        stmt.append(" = ");

        // UUIDs need to be converted to binary
        if (column.getType().equals(MysqlType.UUID))
          stmt.append(uuidToBin("?", false));

        else
          stmt.append('?');
      }

      stmt.append(i == columns.size() - 1 ? " " : ", ");
    }

    // Append VALUES clause only for insertions
    if (isInsert) {
      stmt.append(") VALUES (");

      for (int i = 0; i < columns.size(); i++) {
        MysqlColumn column = columns.get(i);

        // UUIDs need to be converted to binary
        if (column.getType().equals(MysqlType.UUID))
          stmt.append(uuidToBin("?", false));

        else
          stmt.append('?');

        stmt.append(i == columns.size() - 1 ? ");" : ", ");
      }
    }

    // Append an update filter
    else {
      stmt.append("WHERE `id` = ").append(uuidToBin(model.getId())).append(";");
    }

    PreparedStatement ps = conn.prepareStatement(stmt.toString());

    // Fill all placeholder values
    int i = 0;
    for (MysqlColumn column : columns) {
      Object value;

      // Generate a new UUID for the PK
      if (column.isPrimaryKey()) {

        // Primary keys are never updated
        if (!isInsert)
          continue;

        value = UUID.randomUUID();
        column.getModelField().set(model, value);
      }

      // Generate created at timestamp on insertions or set when missing on updates
      else if (
        column.getName().equals("created_at") &&
        (isInsert || model.getCreatedAt() == null)
      ) {
        value = new Date();
        column.getModelField().set(model, value);
      }

      // Updated at starts out as NULL for insertions or is updated on every update
      else if (column.getName().equals("updated_at")) {
        value = isInsert ? null : new Date();
        column.getModelField().set(model, value);
      }

      // Resolve the non-reserved column's value
      else
        value = resolveColumnValue(column, model, replaceCache);

      // UUIDs always need to be stringified
      if (column.getType().equals(MysqlType.UUID) && value != null)
        value = value.toString();

      // Save enums as a string by writing their constant's name
      else if (value != null && column.getModelField().getType().isEnum())
        value = ((Enum<?>) value).name();

      ps.setObject(++i, value);
    }

    logStatement(ps);
    ps.executeUpdate();
  }

  ////////////////////////////////// Transformers /////////////////////////////////////

  /**
   * Load all available transformers into the local list
   */
  private void loadTransformers() {
    ac.getAllInstances()
      .stream()
      .filter(IDataTransformer.class::isInstance)
      .map(IDataTransformer.class::cast)
      .forEach(dt -> this.transformers.add(((IDataTransformer<?, ?>) dt)));
  }

  /**
   * Tries to find a transformer for the provided type and returns a list
   * of inlined (filtered and prefixed) columns which that transformer's
   * known class describes. Parses transformer's known models into tables on
   * demand if they're not yet loaded.
   * @param f The field that's trying to be inlined
   * @param mp Original field's properties
   * @return Optional list of inlined columns, empty if no transformer supports this type
   */
  private Optional<List<MysqlColumn>> inlineTransformedField(Field f, ModelProperty mp) throws Exception {
    IDataTransformer<?, ?> match = null;
    for (IDataTransformer<?, ?> dt : transformers) {
      if (!dt.getForeignClass().equals(f.getType()))
        continue;

      match = dt;
      break;
    }

    if (match == null)
      return Optional.empty();

    Class<? extends APersistentModel> model = match.getKnownClass();

    // Make sure the transformer's known model is parsed as a table
    if (!tables.containsKey(model))
      parseTable(model);

    // Return a list of columns to be inlined into the requesting table
    MysqlTable table = tables.get(model);
    return Optional.of(
      table.columns()
        .stream()

        // Filter out non-transformer columns
        .filter(MysqlColumn::isInlineable)

        // Create a clone of this column that has the foreign field's name as a prefix
        // Use the nullable as well as the unique flags from the original column
        .map(c -> new MysqlColumn(
            f.getName() + "__" + c.getName(),
            c.getType(), mp.isNullable(), c.getMigrationDefault(),
            mp.isUnique(), true, f, c.getModelField(), c.getForeignKey(), c.getForeignAction()
        ))
        .toList()
    );
  }

  /**
   * Get a known field's corresponding transformer by it's declaring class
   * @param knownField Known field
   * @return Data transformer or null if this field is either null or has no transformer attached
   */
  private IDataTransformer<?, ?> getTransformerByKnownField(Field knownField) {
    if (knownField == null)
      return null;

    // Get the transformer by the field's declaring class (the known type)
    return transformers.stream()
      .filter(tr -> tr.getKnownClass().equals(knownField.getDeclaringClass()))
      .findFirst()
      .orElse(null);
  }

  /**
   * Call the transformers replace method to turn a foreign value into it's known model
   * @param transformer Transformer to be used
   * @param input Input value to the replace function
   * @return Replaced known model
   */
  private Object callTransformerReplace(IDataTransformer<?, ?> transformer, Object input) throws Exception {
    // Call the replace method on the model's column value
    Method replace = Arrays.stream(transformer.getClass().getMethods())
      .filter(m -> m.getName().equals("replace"))
      .findFirst().orElseThrow();

    // Return the method's result
    return replace.invoke(transformer, input);
  }

  /**
   * Call the transformers revive method to turn a known model into it's foreign value
   * @param transformer Transformer to be used
   * @param input Input value to the revive function
   * @return Revived foreign value
   */
  private Object callTransformerRevive(IDataTransformer<?, ?> transformer, Object input) throws Exception {
    // Call the revive method on the model's column value
    Method revive = Arrays.stream(transformer.getClass().getMethods())
      .filter(m -> m.getName().equals("revive"))
      .findFirst().orElseThrow();

    // Return the method's result
    return revive.invoke(transformer, input);
  }

  /**
   * Logs the prepared statement and makes sure that all color codes
   * are replaced so they don't affect printing
   * @param ps Statement to log
   */
  private void logStatement(PreparedStatement ps) {
    logger.logDebug(ps.toString().replace('§', '&'));
  }
}
