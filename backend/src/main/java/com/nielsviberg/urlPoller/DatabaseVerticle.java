package com.nielsviberg.urlPoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
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

  public enum ErrorCodes {
    NO_ACTION_SPECIFIED,
    BAD_ACTION,
    DB_ERROR
  }

  private MySQLPool pool;
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class);
  private static final String SQL_CREATE_SERVICE_TABLE = "create table if not exists Service (Id integer auto_increment primary key, Url varchar(255), Name varchar(255), Status varchar(255), Created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, LastUpdated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP)";
  private static final String SQL_GET_SERVICES = "select * from Service";
  private static final String SQL_ADD_SERVICE = "INSERT INTO Service (Url, Name, Status, Created, LastUpdated) \n" +
    "VALUES (#{url},#{name},#{status}, current_timestamp(), current_timestamp());";
  private static final String SQL_DELETE_SERVICE = "DELETE FROM Service where Id=#{id}";
  private static final String SQL_UPDATE_SERVICE = "UPDATE Service SET Url=#{url}, Name=#{name} WHERE Id=#{id}";

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

  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains("action")) {
//      LOGGER.error("No action header specified for message with headers {} and body {}",
//        message.headers(), message.body().encodePrettily());
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

  private void getServices(Message<JsonObject> message){
    pool.query(SQL_GET_SERVICES).execute(res -> {
      if(res.failed()){
        reportQueryError(message, res.cause());
      }
      RowSet<Row> result = res.result();
      //TODO: add typing
      ArrayList services = new ArrayList();
      result.forEach(row -> services.add(row.toJson()));
      JsonObject json = new JsonObject()
        .put("services", services);
      message.reply(json);
    });
  }

  private void addService(Message<JsonObject> message){

    JsonObject json = message.body();

    // SQL template parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("url", json.getString("url"));
    parameters.put("name", json.getString("name"));

    SqlTemplate.forQuery(pool, SQL_ADD_SERVICE).execute(parameters).onSuccess(res -> {
      message.reply(new JsonObject());
    }).onFailure(res -> {
      reportQueryError(message, res.getCause());
    });
  }

  private void removeService(Message<JsonObject> message){

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

  private void updateService(Message<JsonObject> message){

    JsonObject json = message.body();

    // SQL template parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", json.getString("id"));
    parameters.put("url", json.getString("url"));
    parameters.put("name", json.getString("name"));

    SqlTemplate.forQuery(pool, SQL_UPDATE_SERVICE).execute(parameters).onSuccess(res -> {
      message.reply(new JsonObject());
    }).onFailure(res -> {
      reportQueryError(message, res.getCause());
    });
  }

  private void reportQueryError(Message<JsonObject> message, Throwable cause) {
    LOGGER.error("Database query error", cause);
    message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }
}
