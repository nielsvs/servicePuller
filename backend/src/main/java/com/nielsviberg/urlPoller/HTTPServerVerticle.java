package com.nielsviberg.urlPoller;

import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.validator.routines.UrlValidator;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_DB_QUEUE = "db.queue";

  private String DBQueue = "db.queue";

  private WebClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    DBQueue = config().getString(CONFIG_DB_QUEUE, "db.queue");

    HttpServer server = vertx.createHttpServer();
    client = WebClient.create(vertx);

    Router router = Router.router(vertx);
    router.get("/").handler(this::serviceGetHandler);
    router.post().handler(BodyHandler.create());
    router.post("/").handler(this::serviceAddHandler);
    router.delete().handler(BodyHandler.create());
    router.delete("/").handler(this::serviceDeletionHandler);
    router.put().handler(BodyHandler.create());
    router.put("/").handler(this::serviceUpdateHandler);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
    server
      .requestHandler(router)
      .listen(portNumber, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port 8080");
          startPromise.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          startPromise.fail(ar.cause());
        }
      });
  }

  private void serviceGetHandler(RoutingContext context) {
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-services");
    vertx.eventBus().request(DBQueue, null, options, reply -> {
      if (reply.failed()) {
        context.fail(reply.cause());
        return;
      }
      JsonObject body = (JsonObject) reply.result().body();
      context.response().putHeader("content-type", "application/json");
      context.response().setStatusCode(200);
      context.response().end(body.encodePrettily());
    });
  }

  private void serviceAddHandler(RoutingContext context) {
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

    getStatus(serviceUrl, res -> {
      if()
      res.result();
    })

    JsonObject json = new JsonObject();
    json.put("url", serviceUrl);
    json.put("name", name);
    // json.put("status", status);

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "add-service");
    vertx.eventBus().request(DBQueue, json, options, reply -> {
      if (reply.failed()) {
        context.fail(reply.cause());
        return;
      }
      context.response().setStatusCode(204);
      context.response().end();
    });
  }

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

    // Fetch id
    String id = body.getString("id");

    // url is required, notify user
    if (id == null) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The id for the service is required. Please add the id.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return;
    }

    JsonObject json = new JsonObject();
    json.put("id", id);

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "remove-service");
    vertx.eventBus().request(DBQueue, json, options, reply -> {
      if (reply.failed()) {
        context.fail(reply.cause());
        return;
      }
      context.response().setStatusCode(204);
      context.response().end();
    });
  }

  private void serviceUpdateHandler(RoutingContext context) {
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
    String id = body.getString("id");
    String serviceUrl = body.getString("service");
    String name = body.getString("name");

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

    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("url", serviceUrl);
    json.put("name", name);
    // json.put("status", status);

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "update-service");
    vertx.eventBus().request(DBQueue, json, options, reply -> {
      if (reply.failed()) {
        context.fail(reply.cause());
        return;
      }
      context.response().setStatusCode(200);
      context.response().end();
    });
  }
}

