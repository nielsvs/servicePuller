package com.nielsviberg.urlPoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//TODO: split into multiple files
//TODO: use event bus for communicating instead
//TODO: sanitize input
public class MainVerticle extends AbstractVerticle {

  private MySQLPool pool;
  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  private static final String SQL_CREATE_SERVICE_TABLE = "create table if not exists Service (Id integer auto_increment primary key, Url varchar(255), Name varchar(255), Status varchar(255), Created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, LastUpdated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP)";
  private static final String SQL_GET_SERVICES = "select * from Service";
  private static final String SQL_ADD_SERVICE = "INSERT INTO Service (Url, Name, Status, Created, LastUpdated) \n" +
    "VALUES (#{url},#{name},#{status}, current_timestamp(), current_timestamp());";
  private static final String SQL_DELETE_SERVICE = "DELETE FROM Service where Id=#{id}";
  private static final String SQL_UPDATE_SERVICE = "UPDATE Service SET Url=#{url}, Name=#{name} WHERE Id=#{id}";

  // Fetch all services
  private void serviceGetHandler(RoutingContext context) {
    pool.query(SQL_GET_SERVICES).execute(res -> {
      if (res.failed()) {
        context.fail(res.cause());
      } else {
        RowSet<Row> result = res.result();
        //TODO: add typing
        ArrayList services = new ArrayList();
        result.forEach(row -> services.add(row.toJson()));
        JsonObject json = new JsonObject()
          .put("services", services);
        context.response().putHeader("content-type", "application/json");
        context.response().setStatusCode(200);
        context.response().end(json.encodePrettily());
      }
    });
  }

  //TODO: check if body can always be present, so we don't have to check for it's existence
  // Add service
  private void serviceAddHandler(RoutingContext context, WebClient client) {
    // Get values
    JsonObject body = context.getBodyAsJson();

    // body is required, notify user
    if (body == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "Nothing was sent to server, service url is required");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // Fetch service url and name
    String serviceUrl = body.getString("service");
    String name = body.getString("name");

    // url is required, notify user
    if (serviceUrl == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The url for the service is required. Please add the url.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // Validate url format
    UrlValidator urlValidator = new UrlValidator();
    if (!urlValidator.isValid(serviceUrl)) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The url for the service is incorrect. Please check the spelling of the url.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // Fetch url status
    client.requestAbs(HttpMethod.GET, serviceUrl).send().onSuccess(serviceRes -> {
      // Default value for failed requests
      String status = "FAIL";

      // Update status to sucess on any 200 status code resppnse
      if (serviceRes.statusCode() >= 200 && serviceRes.statusCode() <= 299) {
        status = "OK";
      }

      // SQL template parameters
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("url", serviceUrl);
      parameters.put("name", name);
      parameters.put("status", status);

      // Add service to DB
      SqlTemplate.forQuery(pool, SQL_ADD_SERVICE).execute(parameters).onSuccess(res -> {
        context.response().setStatusCode(201);
        context.response().end();
      }).onFailure(res -> {
        context.fail(res.getCause());
      });
    }).onFailure(res -> {
      JsonObject badRequest = new JsonObject();
      LOGGER.error("Error", res);
      badRequest.put("messag$e", "Service was unavailable, please try again.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
    });
  }

  // Remove service
  private void serviceDeletionHandler(RoutingContext context) {

    // Get values
    JsonObject body = context.getBodyAsJson();

    // body is required, notify user
    if (body == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "Nothing was sent to server, service url is required");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }
    // Get id of service
    String id = body.getString("id");

    // id is required, notify user
    if (id == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The id for the service is required. Please send the id.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // SQL template parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", id);

    // Remove service from DB
    SqlTemplate.forQuery(pool, SQL_DELETE_SERVICE).execute(parameters).onSuccess(res -> {
      context.response().putHeader("content-type", "application/json");
      context.response().setStatusCode(204);
      context.response().end();
    }).onFailure(res -> {
      context.fail(res.getCause());
    });
  }

  // Update service
  private void serviceUpdateHandler(RoutingContext context) {
    // Get values
    JsonObject body = context.getBodyAsJson();

    // body is required, notify user
    if (body == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "Nothing was sent to server, service id and url are required");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // Fetch service url and name
    String serviceUrl = body.getString("service");
    String name = body.getString("name");
    String id = body.getString("id");

    //id is required, notify user
    if (id == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The id for the service is required. Please add the id.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // url is required, notify user
    if (serviceUrl == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The url for the service is required. Please enter the url.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // Validate url format
    UrlValidator urlValidator = new UrlValidator();
    if (!urlValidator.isValid(serviceUrl)) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The url for the service is incorrect. Please check the spelling of the url.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    // SQL template parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("url", serviceUrl);
    parameters.put("name", name);
    parameters.put("id", id);

    // Remove service from DB
    SqlTemplate.forQuery(pool, SQL_UPDATE_SERVICE).execute(parameters).onSuccess(res -> {
      context.response().putHeader("content-type", "application/json");
      context.response().setStatusCode(204);
      context.response().end();
    }).onFailure(res -> {
      context.fail(res.getCause());
    });
  }

  // Initialize database connection and create necessary tables
  private Promise<Void> prepareDatabase() {
    Promise<Void> promise = Promise.promise();

    // TODO: move into config
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(3306)
      .setHost("localhost")
      .setDatabase("url-poller")
      .setUser("root")
      .setPassword("202$Stockholm");

    // Custom pool options
    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    // Create the pool
    pool = MySQLPool.pool(vertx, connectOptions, poolOptions);

    // Try to establish connection
    pool.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        promise.fail(ar.cause());
      } else {

        // Create necessary tables
        pool.query(SQL_CREATE_SERVICE_TABLE).execute(create -> {
          if (create.failed()) {
            LOGGER.error("Could not create Service table", create.cause());
            promise.fail(create.cause());
          } else {
            promise.complete();
          }
        });
      }
    });
    return promise;
  }

  private Promise<Void> startHttpServer() {
    Promise<Void> promise = Promise.promise();

    HttpServer server = vertx.createHttpServer();
    WebClient client = WebClient.create(vertx);

    Router router = Router.router(vertx);
    router.get("/").handler(this::serviceGetHandler);
    router.post().handler(BodyHandler.create());
    router.post("/").handler(ctx -> this.serviceAddHandler(ctx, client));
    router.delete().handler(BodyHandler.create());
    router.delete("/").handler(this::serviceDeletionHandler);
    router.put().handler(BodyHandler.create());
    router.put("/").handler(this::serviceUpdateHandler);

    server
      .requestHandler(router)
      .listen(8080, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port 8080");
          promise.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          promise.fail(ar.cause());
        }
      });

    return promise;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Future<Void> steps = prepareDatabase().future().compose(v -> startHttpServer().future());
    steps.onComplete(ar -> {
      if (ar.succeeded()) {
        startPromise.complete();
      } else {
        startPromise.fail(ar.cause());
      }
    });
  }
}
