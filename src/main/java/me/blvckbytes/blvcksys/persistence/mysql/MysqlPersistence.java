package me.blvckbytes.blvcksys.persistence.mysql;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.di.IAutoConstructer;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.ModelIdentifier;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.APersistentModel;
import me.blvckbytes.blvcksys.persistence.transformers.IDataTransformer;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

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
    // Insert
    if (model.getId() == null) {
      return;
    }

    // Update
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
        ModelProperty mp = f.getAnnotation(ModelProperty.class);
        ModelIdentifier mi = f.getAnnotation(ModelIdentifier.class);

        // Get the corresponding SQL type
        Optional<MysqlType> type = MysqlType.fromJavaType(
          f.getType(),
          // Unique fields or identifiers (primary key) require fixed-length datatypes
          (mp != null && mp.isUnique()) || (mi != null)
        );

        // Could not find a matching SQL type directly
        if (type.isEmpty()) {

          // Try to resolve this field through a known transformer
          Optional<List<MysqlColumn>> transformed = inlineTransformedField(f.getType(), parsedTables);
          if (transformed.isPresent()) {
            columns.addAll(transformed.get());
            continue;
          }

          throw new PersistenceException(
            "Could not find matching SQL-Type for " + model + ": " + f.getName() + " (" + f.getType() + ")"
          );
        }

        // Found an identifier field
        if (mi != null) {

          if (foundIdColumn)
            throw new PersistenceException("Duplicate identifier in " + model + ": " + f.getName());

          columns.add(new MysqlColumn("id", type.get(), false, true, true, false));
          foundIdColumn = true;
          continue;
        }

        // Found a property field
        if (mp != null) {

          // Skip non-inherited fields from superclasses
          if (!mp.isInherited() && c != model)
            continue;

          columns.add(new MysqlColumn(
            mp.name().isBlank() ? f.getName() : mp.name(),
            type.get(), mp.isNullable(), false, mp.isUnique(), mp.isInlineable()
          ));
        }
      }

      // Walk up the inheritance hierarchy
      c = c.getSuperclass();
    }

    if (!foundIdColumn)
      throw new PersistenceException("Missing identifier field in " + model);

    // Check for duplicate columns
    Set<String> colNames = new HashSet<>();
    for (MysqlColumn column : columns) {
      if (!colNames.add(column.name()))
        throw new PersistenceException("Duplicate column in " + model + ": " + column.name());
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
        .append(col.name())
        .append("`")
        .append(' ')
        .append(col.type())
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
   * @param foreignType Foreign type to inline
   * @param parsedTables Writable reference to a map of already parsed tables
   * @return Optional list of inlined columns, empty if no transformer supports this type
   */
  private Optional<List<MysqlColumn>> inlineTransformedField(
    Class<?> foreignType,
    Map<Class<? extends APersistentModel>, MysqlTable> parsedTables
  ) {
    // Search for a matching transformer
    for (IDataTransformer<?, ?> df : transformers) {
      if (!df.getForeignClass().equals(foreignType))
        continue;
      Class<? extends APersistentModel> model = df.getKnownClass();

      // Parse this table if it hasn't yet been parsed
      if (!parsedTables.containsKey(model))
        parseTable(model, parsedTables);

      // Return a list of columns to be inlined into the requesting table
      MysqlTable table = parsedTables.get(model);
      return Optional.of(
        table.columns()
          .stream()
          // Filter out non-inlineable columns
          .filter(MysqlColumn::isInlineable)
          // Create a clone of this column that has the table's name as a prefix
          .map(c -> new MysqlColumn(
            table.name() + "_" + c.name(),
            c.type(), c.isNullable(), c.isPrimaryKey(), c.isUnique(), true
          ))
          .toList()
      );
    }

    return Optional.empty();
  }
}
