package com.nielsviberg.urlPoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.templates.SqlTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



public class DatabaseVerticle extends AbstractVerticle {

  private MySQLPool pool;
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class);

  // SQL queries
  private static final String SQL_CREATE_SERVICE_TABLE = "create table if not exists Service (Id integer auto_increment primary key, Url varchar(255), Name varchar(255), Status varchar(255), Created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, LastUpdated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP)";
  private static final String SQL_GET_SERVICES = "select * from Service";
  private static final String SQL_ADD_SERVICE = "INSERT INTO Service (Url, Name, Status, Created, LastUpdated) \n" +
    "VALUES (#{url},#{name},#{status}, current_timestamp(), current_timestamp());";
  private static final String SQL_DELETE_SERVICE = "DELETE FROM Service where Id=#{id}";
  private static final String SQL_UPDATE_SERVICE = "UPDATE Service SET Url=#{url}, Name=#{name}, LastUpdated=current_timestamp() WHERE Id=#{id}";
  private static final String SQL_UPDATE_WITH_STATUS_SERVICE = "UPDATE Service SET Url=#{url}, Name=#{name}, Status=#{status}, LastUpdated=current_timestamp() WHERE Id=#{id}";

  // DB configuration
  public static final String CONFIG_DB_PORT = "db.port";
  public static final String CONFIG_DB_DEFAULT_DB = "db.default.db";
  public static final String CONFIG_DB_HOST = "db.host";
  public static final String CONFIG_DB_MAX_POOL_SIZE = "db.max_pool_size";
  public static final String CONFIG_DB_QUEUE = "db.queue";

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // TODO: move user credentials into config
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(config().getInteger(CONFIG_DB_PORT, 3306))
      .setHost(config().getString(CONFIG_DB_HOST, "localhost"))
      .setDatabase(config().getString(CONFIG_DB_DEFAULT_DB, "url-poller"))
      .setUser("root")
      .setPassword("202$Stockholm");

    // Custom pool options
    PoolOptions poolOptions = new PoolOptions().setMaxSize(config().getInteger(CONFIG_DB_MAX_POOL_SIZE, 5));

    // Create the pool
    pool = MySQLPool.pool(vertx, connectOptions, poolOptions);

    // Try to establish connection
    pool.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        startPromise.fail(ar.cause());
      } else {

        // Create necessary tables
        pool.query(SQL_CREATE_SERVICE_TABLE).execute(create -> {
          if (create.failed()) {
            LOGGER.error("Could not create Service table", create.cause());
            startPromise.fail(create.cause());
          } else {
            vertx.eventBus().consumer(config().getString(CONFIG_DB_QUEUE, "db.queue"), this::onMessage);
            startPromise.complete();
          }
        });
      }
    });
  }

  /**
   * Get database action and perform operation according to message. Fails if message is not recognized.
   * @param message - eventbus message containing identifier to perform correct db operation
   */
  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains("action")) {
      LOGGER.error("No action header specified");
      message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
      return;
    }
    String action = message.headers().get("action");

    switch (action) {
      case "get-services":
        getServices(message);
        break;
      case "add-service":
        addService(message);
        break;
      case "remove-service":
        removeService(message);
        break;
      case "update-service":
        updateService(message);
        break;
      default:
        message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
    }
  }

  // Get services from db and send back via message
  private void getServices(Message<JsonObject> message) {
    pool.query(SQL_GET_SERVICES).execute(res -> {
      if (res.failed()) {
        reportQueryError(message, res.cause());
      }
      RowSet<Row> result = res.result();
      ArrayList<JsonObject> services = new ArrayList();
      result.forEach(row -> services.add(row.toJson()));
      JsonObject json = new JsonObject()
        .put("services", services);
      message.reply(json);
    });
  }

  // Add service to db
  private void addService(Message<JsonObject> message) {

    JsonObject json = message.body();

    // SQL template parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("url", json.getString("url"));
    parameters.put("name", json.getString("name"));
    parameters.put("status", json.getString("status"));

    SqlTemplate.forQuery(pool, SQL_ADD_SERVICE).execute(parameters).onSuccess(res -> {
      RowSet<Row> result = res.value();
      result.forEach(row -> {
        System.out.println(row.toJson());
      });
      message.reply(new JsonObject());
    }).onFailure(res -> {
      reportQueryError(message, res.getCause());
    });
  }

  // Delete service from db
  private void removeService(Message<JsonObject> message) {

    JsonObject json = message.body();

    // SQL template parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", json.getString("id"));

    SqlTemplate.forQuery(pool, SQL_DELETE_SERVICE).execute(parameters).onSuccess(res -> {
      message.reply(new JsonObject());
    }).onFailure(res -> {
      reportQueryError(message, res.getCause());
    });
  }

  // Update service from db with or without status
  private void updateService(Message<JsonObject> message) {
    String updateQuery = SQL_UPDATE_SERVICE;
    JsonObject json = message.body();
    Boolean updateStatus = json.getBoolean("updateStatus",false);

    // SQL template parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", json.getString("id"));
    parameters.put("url", json.getString("url"));
    parameters.put("name", json.getString("name"));

    // Add status update to parameters and use SQL update with status
    if (updateStatus) {
      parameters.put("status", json.getString("status"));
      updateQuery = SQL_UPDATE_WITH_STATUS_SERVICE;
    }

    SqlTemplate.forQuery(pool, updateQuery).execute(parameters).onSuccess(res -> {
      message.reply(new JsonObject());
    }).onFailure(res -> {
      reportQueryError(message, res.getCause());
    });
  }

  /**
   * Log error and fail message with same error
   * @param message - eventbus message
   * @param cause - cause of error
   */
  private void reportQueryError(Message<JsonObject> message, Throwable cause) {
    LOGGER.error("Database query error", cause);
    message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }
}
