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
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import me.blvckbytes.blvcksys.persistence.transformers.IDataTransformer;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
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
  private final List<? extends IDataTransformer<?, ?>> transformers;
  private final Connection conn;

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
  ) throws SQLException {
    this.logger = logger;
    this.refl = refl;
    this.plugin = plugin;
    this.ac = ac;
    this.cfg = cfg;

    this.conn = connect();
    this.transformers = loadTransformers();
    this.tables = parseAllTables();
    createAllTables();
  }

  //=========================================================================//
  //                                  API                                    //
  //=========================================================================//

  @Override
  public void store(APersistentModel model) throws PersistenceException {
    try {
      if (model.getId() == null) {
        insertModel(model);
        return;
      }

      // TODO: Update
    }

    // Re-throw persistence exceptions
    catch (PersistenceException e) {
      throw e;
    }

    // Map various other issues to "internal errors"
    catch (Exception e) {
      logger.logError(e);
      throw new PersistenceException("An internal error occurred");
    }
  }

  @Override
  public APersistentModel findById(UUID id) throws PersistenceException {
    return null;
  }

  @Override
  public APersistentModel findById(String id) throws PersistenceException {
    return null;
  }

  @Override
  public List<APersistentModel> list(Class<APersistentModel> type) {
    return null;
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
  private Connection connect() throws SQLException {
    String user = cfg.get(ConfigKey.DB_USERNAME).toString();
    String host = cfg.get(ConfigKey.DB_HOST).toString();
    String port = cfg.get(ConfigKey.DB_PORT).toString();
    String database = cfg.get(ConfigKey.DB_DATABASE).toString();

    String resource = host + ":" + port + "/" + database;

    Connection conn = DriverManager.getConnection(
      "jdbc:mysql://" + resource, user, cfg.get(ConfigKey.DB_PASSWORD).toString()
    );

    logger.logInfo("Connected to the Database using " + user + "@" + resource);
    return conn;
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
   * Transform a table name representation (snake case)) into it's
   * model name representation (pascal case)
   * @param tableName Name to convert
   * @param capitalizeFirst Whether to capitalize the very first character
   * @return Converted name
   */
  private String tableNameToModelName(String tableName, boolean capitalizeFirst) {
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
  private String modelNameToTableName(String modelName) {
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
   * @param parsedTables Map of already parsed tables, has to be writeable
   */
  private void parseTable(Class<? extends APersistentModel> model, Map<Class<? extends APersistentModel>, MysqlTable> parsedTables) {
    List<MysqlColumn> columns = new ArrayList<>();
    boolean foundIdColumn = false;

    // Loop all superclasses
    Class<?> c = model;
    while (c.getSuperclass() != null) {

      // Loop all fields of the current class
      for (Field f : c.getDeclaredFields()) {
        f.setAccessible(true);

        ModelProperty mp = f.getAnnotation(ModelProperty.class);

        // Skip non-model fields
        if (mp == null)
          continue;

        String name = mp.name().isBlank() ? f.getName() : mp.name();
        boolean isIdentifier = name.equals("id");

        // Get the corresponding SQL type
        Optional<MysqlType> type = MysqlType.fromJavaType(
          f.getType(),
          // Unique fields or identifiers (primary key) require fixed-length datatypes
          mp.isUnique() || isIdentifier
        );

        // Could not find a matching SQL type directly
        if (type.isEmpty()) {

          // Try to resolve this field through a known transformer
          Optional<List<MysqlColumn>> transformed = inlineTransformedField(f, parsedTables);
          if (transformed.isPresent()) {
            columns.addAll(transformed.get());
            continue;
          }

          throw new PersistenceException(
            "Could not find matching SQL-Type for " + model + ": " + f.getName() + " (" + f.getType() + ")"
          );
        }

        // Found an identifier field
        if (isIdentifier) {

          if (foundIdColumn)
            throw new PersistenceException("Duplicate identifier in " + model + ": " + f.getName());

          if (type.get() != MysqlType.UUID)
            throw new PersistenceException("Unsupported identifier type in " + model + ": " + f.getName());

          columns.add(new MysqlColumn("id", type.get(), false, true, false, null, f));
          foundIdColumn = true;
          continue;
        }

        // Skip non-inherited fields from superclasses
        if (!mp.isInherited() && c != model)
          continue;

        columns.add(new MysqlColumn(
          modelNameToTableName(name),
          type.get(), mp.isNullable(), mp.isUnique(), mp.isInlineable(), null, f
        ));
      }

      // Walk up the inheritance hierarchy
      c = c.getSuperclass();
    }

    if (!foundIdColumn)
      throw new PersistenceException("Missing identifier field in " + model);

    // Check for duplicate columns
    Set<String> colNames = new HashSet<>();
    for (MysqlColumn column : columns) {
      if (!colNames.add(column.getName()))
        throw new PersistenceException("Duplicate column in " + model + ": " + column.getName());
    }

    // Check if this model is used in combination with a registered transformer
    boolean isTransformer = false;
    for (IDataTransformer<?, ?> transformer : transformers) {
      if (!transformer.getKnownClass().equals(model))
        continue;

      isTransformer = true;
      break;
    }

    // Register this table with it's model-class
    parsedTables.put(
      model,
      new MysqlTable(
        modelNameToTableName(model.getSimpleName()),
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
    return conn.prepareStatement("SHOW TABLES LIKE '" + table.name() + "';").executeQuery().next();
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
    this.conn.prepareStatement(stmt.toString()).executeUpdate();
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
   * @return Map of the model-class and it's corresponding SQL table
   */
  private Map<Class<? extends APersistentModel>, MysqlTable> parseAllTables() {
    String pkg = getClass().getPackageName();

    Map<Class<? extends APersistentModel>, MysqlTable> res = new HashMap<>();

    // Look up all classes that are marked as models in ../models
    List<? extends Class<? extends APersistentModel>> models = refl.findImplClasses(
      plugin,
      pkg.substring(0, pkg.lastIndexOf('.')) + ".models",
      APersistentModel.class
    );

    // Parse them into tables
    for (Class<? extends APersistentModel> model : models)
      parseTable(model, res);

    return res;
  }

  /////////////////////////////////// Insertion ///////////////////////////////////////

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
    Map<IDataTransformer<?, ?>, Object> replaceCache
  ) throws Exception {
    Object value = column.getModelField().get(model);
    MysqlColumn col = column.getTransformerColumn();

    // This column is not being transformed, return the value as-is
    if (col == null)
      return value;

    // Get the transformer by the field's declaring class (the known type)
    IDataTransformer<?, ?> transformer = transformers.stream()
      .filter(tr -> tr.getKnownClass().equals(col.getModelField().getDeclaringClass()))
      .findFirst()
      .orElseThrow();

    // Call the replace method on the model's column value
    Method replace = Arrays.stream(transformer.getClass().getMethods())
      .filter(m -> m.getName().equals("replace"))
      .findFirst().orElseThrow();

    // Save the replace result into cache
    Object replaced = replace.invoke(transformer, value);
    replaceCache.put(transformer, replaced);

    // Get the transformer column's value from the model the value just got replaced into
    return col.getModelField().get(replaced);
  }

  /**
   * Check for duplicate keys of a model's unique columns and
   * throw an exception on occurrence
   * @param model Model to check for
   * @param table Table that corresponds to this model
   * @param replaceCache Writeable cache used to store replace() results in and access
   *                     them over the (possibly) multiple field accesses of a transformed result
   */
  private void checkDuplicateKeys(
    APersistentModel model,
    MysqlTable table,
    Map<IDataTransformer<?, ?>, Object> replaceCache
  ) throws Exception {
    for (MysqlColumn column : table.columns()) {
      if (column.isPrimaryKey() || !column.isUnique())
        continue;

      Object value = resolveColumnValue(column, model, replaceCache);

      PreparedStatement ps = conn.prepareStatement(
        "SELECT COUNT(*) FROM `" +
        table.name() +
        "` WHERE `" +
        column.getName() +
        "` = ?;"
      );

      ps.setObject(1, value);
      ResultSet rs = ps.executeQuery();

      // There was a result (where there shouldn't be), throw
      if (rs.next()) {
        if (rs.getInt(1) > 0)
          throw new DuplicatePropertyException(
            tableNameToModelName(table.name(), true),
            tableNameToModelName(column.getName(), false),
            value
          );
      }
    }
  }

  /**
   * Insert a model into the database and set it's auto-generated fields
   * @param model Model to insert
   */
  private void insertModel(APersistentModel model) throws Exception {
    MysqlTable table = tables.get(model.getClass());

    // Cannot insert unknown tables or transformers
    if (table == null || table.isTransformer())
      throw new PersistenceException("The model " + model + " is not registered!");

    Map<IDataTransformer<?, ?>, Object> replaceCache = new HashMap<>();

    // Ensure that there are no duplicate keys
    checkDuplicateKeys(model, table, replaceCache);

    List<MysqlColumn> columns = table.columns();
    StringBuilder stmt = new StringBuilder("INSERT INTO `" + table.name() + "` (");

    // Append a list of column names, in the order they appear in the list
    for (int i = 0; i < columns.size(); i++) {
      stmt
        .append("`")
        .append(columns.get(i).getName())
        .append("`")
        .append(i == columns.size() - 1 ? ")" : ", ");
    }

    stmt.append(" VALUES (");

    for (int i = 0; i < columns.size(); i++) {
      MysqlColumn column = columns.get(i);

      // UUIDs need to be converted to binary
      if (column.getType().equals(MysqlType.UUID))
        stmt.append("UNHEX(REPLACE(?, \"-\", \"\"))");

      else
        stmt.append('?');

      stmt.append(i == columns.size() - 1 ? ");" : ", ");
    }

    PreparedStatement ps = conn.prepareStatement(stmt.toString());

    int i = 0;
    for (MysqlColumn column : columns) {
      Object value;

      // Generate a new UUID for the PK
      if (column.isPrimaryKey()) {
        value = UUID.randomUUID();
        column.getModelField().set(model, value);
      }

      // Generate created at timestamp
      else if (column.getName().equals("created_at")) {
        value = new Date();
        column.getModelField().set(model, value);
      }

      // Updated at starts out as NULL
      else if (column.getName().equals("updated_at"))
        value = null;

      // Resolve the non-reserved column's value
      else
        value = resolveColumnValue(column, model, replaceCache);

      // UUIDs always need to be stringified
      if (column.getType().equals(MysqlType.UUID) && value != null)
        value = value.toString();

      ps.setObject(++i, value);
    }

    ps.executeUpdate();
  }

  ////////////////////////////////// Transformers /////////////////////////////////////

  /**
   * Load all available transformers into the local list
   */
  private List<? extends IDataTransformer<?, ?>> loadTransformers() {
    return ac.getAllInstances()
      .stream()
      .filter(IDataTransformer.class::isInstance)
      .map(IDataTransformer.class::cast)
      .map(dt -> (IDataTransformer<?, ?>) dt)
      .toList();
  }

  /**
   * Tries to find a transformer for the provided type and returns a list
   * of inlined (filtered and prefixed) columns which that transformer's
   * known class describes. Loads transformers into tables on demand
   * if they're not yet loaded.
   * @param f The field that's trying to be inlined
   * @param parsedTables Writable reference to a map of already parsed tables
   * @return Optional list of inlined columns, empty if no transformer supports this type
   */
  private Optional<List<MysqlColumn>> inlineTransformedField(
    Field f,
    Map<Class<? extends APersistentModel>, MysqlTable> parsedTables
  ) {
    // Search for a matching transformer
    for (IDataTransformer<?, ?> dt : transformers) {
      if (!dt.getForeignClass().equals(f.getType()))
        continue;

      Class<? extends APersistentModel> model = dt.getKnownClass();

      // Parse this table if it hasn't yet been parsed
      if (!parsedTables.containsKey(model))
        parseTable(model, parsedTables);

      // Return a list of columns to be inlined into the requesting table
      MysqlTable table = parsedTables.get(model);
      return Optional.of(
        table.columns()
          .stream()
          // Filter out non-transformer columns
          .filter(MysqlColumn::isInlineable)
          // Create a clone of this column that has the table's name as a prefix
          .map(c -> new MysqlColumn(
            table.name() + "_" + c.getName(),
            c.getType(), c.isNullable(), c.isUnique(), true, c, f
          ))
          .toList()
      );
    }

    return Optional.empty();
  }
}
