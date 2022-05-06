package me.blvckbytes.blvcksys.persistence.mysql;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.di.IAutoConstructer;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.ModelNotFoundException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import me.blvckbytes.blvcksys.persistence.query.FieldQuery;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.persistence.query.QueryConnection;
import me.blvckbytes.blvcksys.persistence.transformers.IDataTransformer;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.util.Tuple;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;

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

      logger.logDebug(ps.toString());

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
      PreparedStatement ps = buildQuery(query, false);
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
  public <T extends APersistentModel> Optional<T> findFirst(QueryBuilder<T> query) throws PersistenceException {
    try {
      PreparedStatement ps = buildQuery(query, true);
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
  public void delete(APersistentModel model) throws PersistenceException {
    delete(model.getClass(), model.getId());
    refl.setFieldByName(model, "id", null);
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
    String database = cfg.get(ConfigKey.DB_DATABASE).toString();

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
    List<MysqlColumn> columns = new ArrayList<>();

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
        Optional<List<MysqlColumn>> transformed = inlineTransformedField(f);
        if (transformed.isPresent()) {
          columns.addAll(transformed.get());
          continue;
        }

        throw new PersistenceException("Invalid type " + f.getType() + " for field " + f.getName() + " of " + model);
      }

      if (isIdentifier) {
        if (type.get() != MysqlType.UUID)
          throw new PersistenceException("Unsupported identifier type " + f.getType() + " for field " + f.getName() + " of " + model);

        columns.add(new MysqlColumn("id", type.get(), false, true, false, f, null));
        continue;
      }

      columns.add(new MysqlColumn(
        modelNameToDBName(f.getName()),
        type.get(), mp.isNullable(), mp.isUnique(), mp.isInlineable(), f, null
      ));
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

    // Register this table with it's model-class
    tables.put(
      model,
      new MysqlTable(
        modelNameToDBName(model.getSimpleName()),
        columns,
        isTransformer
      )
    );
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

    logger.logDebug(ps.toString());

    rs.close();
    ps.close();

    return exists;
  }

  /**
   * Dispatches a table creation statement if the table doesn't yet exist
   * @param table Table to create
   */
  private void createTableIfNotExists(MysqlTable table) throws SQLException {
    if (isTableExisting(table))
      return;

    StringBuilder stmt = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + table.name() + "`(");

    List<MysqlColumn> columns = table.columns();
    for (int i = 0; i < columns.size(); i++) {
      MysqlColumn col = columns.get(i);
      stmt
        .append('`')
        .append(col.getName())
        .append("`")
        .append(' ')
        .append(col.getType())
        .append(' ')
        .append(col.isNullable() ? "NULL" : "NOT NULL")
        .append(col.isPrimaryKey() ? " PRIMARY KEY" : "")
        .append((col.isUnique() && !col.isPrimaryKey()) ? " UNIQUE" : "")
        .append(i == columns.size() - 1 ? "" : ", ");
    }

    stmt.append(");");
    PreparedStatement ps = this.conn.prepareStatement(stmt.toString());
    logger.logDebug(ps.toString());

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
    for (Class<? extends APersistentModel> model : models)
      parseTable(model);
  }

  ///////////////////////////////// Query Builder /////////////////////////////////////

  /**
   * Stringify a field query to a partial statement, for example:
   * field=test, op=EQ, value=5 would yield: `test` == ? and add (INTEGER, 5) to params
   * @param query Query to stringify
   * @param table Table which this field has to be a member of
   * @param params Modifyable map of parameters to add the value parameter
   * @return Stringified query
   */
  private String stringifyFieldQuery(FieldQuery query, MysqlTable table, Map<MysqlType, Object> params) {
    MysqlColumn targCol = table.columns().stream()
      .filter(
        c -> dbNameToModelName(c.getName(), false).equals(query.field())
      )
      .findFirst()
      .orElseThrow(() -> new RuntimeException(
        "The query field " + query.field() + " is not a member of the model " + dbNameToModelName(table.name(), true)
      ));

    Class<?> targType = targCol.getType().getJavaEquivalent();
    Class<?> queryType = query.value().getClass();
    if (targType != queryType)
      throw new RuntimeException("The query field " + query.field() + " is of invalid type " + queryType + " instead of " + targType);

    if (!targCol.getType().supportsOp(query.op()))
      throw new RuntimeException("The query field " + query.field() + " does not support the operation " + query.op());

    params.put(targCol.getType(), query.value());

    return switch (query.op()) {
      case EQ -> "`" + query.field() + "` = ?";
      case NEQ -> "`" + query.field() + "` != ?";
      case EQ_IC -> "LOWER(`" + query.field() + "`) = LOWER(?)";
      case NEQ_IC -> "LOWER(`" + query.field() + "`) != LOWER(?)";
      case LT -> "`" + query.field() + "` < ?";
      case LTE -> "`" + query.field() + "` <= ?";
      case GT -> "`" + query.field() + "` > ?";
      case GTE -> "`" + query.field() + "` >= ?";
    };
  }

  /**
   * Stringify a field query group to a partial statement, for example:
   * root=(field=test, op=EQ, value=5) additionals=[(AND, (field=test2, op=NEQ, value=10))] would yield:
   * (`test` == ? AND test2 != ?) and add (INTEGER, 5), (INTEGER, 10) to params
   * @param group Query group to stringify
   * @param table Table which this field has to be a member of
   * @param params Modifyable map of parameters to add the value parameter
   * @return Stringified query group
   */
  private String stringifyFieldQueryGroup(FieldQueryGroup group, MysqlTable table, Map<MysqlType, Object> params) {
    StringBuilder groupStr = new StringBuilder("(");

    // Append the root (first entry with no connection prefix)
    groupStr.append(stringifyFieldQuery(group.getRoot(), table, params));

    // Append all additional queries with their connection leading them
    for (Tuple<QueryConnection, FieldQuery> additional : group.getAdditionals()) {
      groupStr.append(additional.a()).append(" ");
      groupStr.append(stringifyFieldQuery(additional.b(), table, params));
    }

    return groupStr + ")";
  }

  /**
   * Build a selecting query from a query builder's state
   * @param query Query builder to build from
   * @param onlyFirst Whether to only query for the first result
   * @return Built query statement with all parameters applied
   */
  private PreparedStatement buildQuery(QueryBuilder<?> query, boolean onlyFirst) throws Exception {
    MysqlTable table = getTableFromModel(query.getModel(), false);

    StringBuilder stmt = new StringBuilder("SELECT * FROM `" + table.name() + "` WHERE ");
    Map<MysqlType, Object> params = new HashMap<>();

    stmt.append(stringifyFieldQueryGroup(query.getRoot(), table, params));

    // Append all additional query groups with their connection leading them
    for (Tuple<QueryConnection, FieldQueryGroup> additional : query.getAdditionals()) {
      stmt.append(" ").append(additional.a()).append(" ");
      stmt.append(stringifyFieldQueryGroup(additional.b(), table, params));
    }

    if (query.getLimit() != null || onlyFirst)
      stmt.append(" LIMIT ").append(onlyFirst ? 1 : query.getLimit());

    if (query.getSkip() != null)
      stmt.append(" OFFSET ").append(query.getSkip());

    PreparedStatement ps = conn.prepareStatement(stmt + ";");

    int i = 0;
    for (Map.Entry<MysqlType, Object> param : params.entrySet())
      ps.setObject(++i, translateValue(param.getKey(), param.getValue()));

    logger.logDebug(ps.toString());
    return ps;
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
    // Turn byte[]'s (binary columns) into UUIDs
    if (type == MysqlType.UUID) {
      ByteBuffer bb = ByteBuffer.wrap((byte[]) value);
      value = new UUID(bb.getLong(), bb.getLong());
    }

    // Turn the driver's LocalDateTime into java's default Date
    if (type == MysqlType.DATETIME) {
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
  @SuppressWarnings("unchecked")
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
          knownCol.getModelField().set(knownInst, value);
          remainingColumns.remove(targRemCol);
        }

        // Call the reviver on this known model to receive the foreign value to write to the row's model
        IDataTransformer<?, ?> dt = getTransformerByKnownField(knownField);
        col.getModelField().set(inst, callTransformerRevive(dt, knownInst));
        continue;
      }

      // Directly set the model's field value to the corresponding column's value
      col.getModelField().set(inst, translateValue(col.getType(), rs.getObject(col.getName())));
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

    logger.logDebug(ps.toString());

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
    boolean hasId = model.getId() != null;

    for (MysqlColumn column : table.columns()) {
      if (column.isPrimaryKey() || !column.isUnique())
        continue;

      Object value = resolveColumnValue(column, model, replaceCache);

      PreparedStatement ps = conn.prepareStatement(
        "SELECT COUNT(*) FROM `" +
        table.name() +
        "` WHERE `" +
        column.getName() +
        "` = ?" +
        // Skip self, if the model already has an ID
        (hasId ? " AND `id` != " + uuidToBin(model.getId()) : "") +
        ";"
      );

      ps.setObject(1, value);
      logger.logDebug(ps.toString());

      ResultSet rs = ps.executeQuery();

      // There was a result (where there shouldn't be), throw
      if (rs.next()) {
        if (rs.getInt(1) > 0)
          throw new DuplicatePropertyException(
            dbNameToModelName(table.name(), true),
            dbNameToModelName(column.getName(), false),
            value
          );
      }

      ps.close();
    }
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

    // INSERT INTO `x` (a, b, c) VALUES (?, ?, ?);
    // UPDATE `x` SET a = ?, b = ?, c = ? WHERE `id` = ?;
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
      Object value = null;

      // Generate a new UUID for the PK
      if (column.isPrimaryKey()) {

        // Primary keys are never updated
        if (!isInsert)
          continue;

        value = UUID.randomUUID();
        column.getModelField().set(model, value);
      }

      // Generate created at timestamp on insertions or set when missing on updates
      else if (column.getName().equals("created_at")) {
        if (isInsert || model.getCreatedAt() == null) {
          value = new Date();
          column.getModelField().set(model, value);
        }
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

      ps.setObject(++i, value);
    }

    logger.logDebug(ps.toString());
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
   * @return Optional list of inlined columns, empty if no transformer supports this type
   */
  private Optional<List<MysqlColumn>> inlineTransformedField(Field f) throws Exception {
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
        .map(c -> new MysqlColumn(
          f.getName() + "__" + c.getName(),
          c.getType(), c.isNullable(), c.isUnique(), true, f, c.getModelField()
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
}
