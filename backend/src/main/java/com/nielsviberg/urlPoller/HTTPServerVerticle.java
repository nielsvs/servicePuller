package com.nielsviberg.urlPoller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.reactivex.Single;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.validation.ValidationHandler;
import io.vertx.reactivex.json.schema.SchemaParser;
import io.vertx.reactivex.json.schema.SchemaRouter;
import org.apache.commons.validator.routines.UrlValidator;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.vertx.json.schema.common.dsl.Schemas.*;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_DB_QUEUE = "db.queue";
  public static final String CONFIG_TIMEOUT = "timeout";

  private String DBQueue = "db.queue";

  private WebClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    DBQueue = config().getString(CONFIG_DB_QUEUE, "db.queue");

    HttpServer server = vertx.createHttpServer();
    client = WebClient.create(vertx);

    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createDraft7SchemaParser(schemaRouter);

    // Service url is required to add a new service, name is optional
    ObjectSchemaBuilder addBodySchemaBuilder = objectSchema()
      .requiredProperty("service", stringSchema())
      .property("name", stringSchema());
    // Service id is required to delete a service
    ObjectSchemaBuilder deleteBodySchemaBuilder = objectSchema()
      .requiredProperty("id", intSchema());

    // Service id, url and name are required to update a service
    ObjectSchemaBuilder updateBodySchemaBuilder = objectSchema()
      .requiredProperty("id", intSchema())
      .requiredProperty("service", stringSchema())
      .requiredProperty("name", stringSchema());

    // Validate handlers for add, delete and update.
    ValidationHandler addValidateHandler = ValidationHandler.newInstance(
      ValidationHandler
        .builder(schemaParser)
        .predicate(RequestPredicate.BODY_REQUIRED)
        .body(Bodies.json(addBodySchemaBuilder))
        .body(Bodies.formUrlEncoded(addBodySchemaBuilder))
        .build());

    ValidationHandler deleteValidateHandler = ValidationHandler.newInstance(
      ValidationHandler
        .builder(schemaParser)
        .predicate(RequestPredicate.BODY_REQUIRED)
        .body(Bodies.json(deleteBodySchemaBuilder))
        .body(Bodies.formUrlEncoded(deleteBodySchemaBuilder))
        .build());

    ValidationHandler updateValidateHandler = ValidationHandler.newInstance(
      ValidationHandler
        .builder(schemaParser)
        .predicate(RequestPredicate.BODY_REQUIRED)
        .body(Bodies.json(updateBodySchemaBuilder))
        .body(Bodies.formUrlEncoded(updateBodySchemaBuilder))
        .build());

    // Routing
    Router router = Router.router(vertx);
    router.options("/").handler(CorsHandler.create("*")
      .allowedMethod(HttpMethod.OPTIONS)
      .allowedMethod(HttpMethod.DELETE)
      .allowedMethod(HttpMethod.POST)
      .allowedMethod(HttpMethod.PUT)
      .allowedHeader("Access-Control-Request-Method")
      .allowedHeader("Access-Control-Allow-Credentials")
      .allowedHeader("Access-Control-Allow-Origin")
      .allowedHeader("Access-Control-Allow-Headers")
      .allowedHeader("Content-Type"));
    router
      .get("/")
      .handler(CorsHandler.create("*")
        .allowedMethod(HttpMethod.GET)
        .allowedHeader("Access-Control-Request-Method")
        .allowedHeader("Access-Control-Allow-Credentials")
        .allowedHeader("Access-Control-Allow-Origin")
        .allowedHeader("Access-Control-Allow-Headers")
        .allowedHeader("Content-Type"))
      .handler(this::serviceGetHandler);
    router.post().handler(BodyHandler.create());
    router
      .post("/")
      .handler(CorsHandler.create("*")
        .allowedMethod(HttpMethod.OPTIONS)
        .allowedMethod(HttpMethod.POST)
        .allowedHeader("Access-Control-Request-Method")
        .allowedHeader("Access-Control-Allow-Credentials")
        .allowedHeader("Access-Control-Allow-Origin")
        .allowedHeader("Access-Control-Allow-Headers")
        .allowedHeader("Content-Type"))
      .handler(addValidateHandler)
      .handler(this::serviceAddHandler);
    router.delete().handler(BodyHandler.create());
    router
      .delete("/")
      .handler(CorsHandler.create("*")
        .allowedMethod(HttpMethod.OPTIONS)
        .allowedMethod(HttpMethod.DELETE)
        .allowedHeader("Access-Control-Request-Method")
        .allowedHeader("Access-Control-Allow-Credentials")
        .allowedHeader("Access-Control-Allow-Origin")
        .allowedHeader("Access-Control-Allow-Headers")
        .allowedHeader("Content-Type"))
      .handler(deleteValidateHandler)
      .handler(this::serviceDeletionHandler);
    router.put().handler(BodyHandler.create());
    router
      .put("/")
      .handler(CorsHandler.create("*")
        .allowedMethod(HttpMethod.OPTIONS)
        .allowedMethod(HttpMethod.PUT)
        .allowedHeader("Access-Control-Request-Method")
        .allowedHeader("Access-Control-Allow-Credentials")
        .allowedHeader("Access-Control-Allow-Origin")
        .allowedHeader("Access-Control-Allow-Headers")
        .allowedHeader("Content-Type"))
      .handler(updateValidateHandler)
      .handler(this::serviceUpdateHandler);

    // Update service records every minutes
    Runnable updateServicesRunnable = this::updateServices;
    ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    exec.scheduleAtFixedRate(updateServicesRunnable, 0, 1, TimeUnit.MINUTES);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8888);
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

  /**
   * Iterate through all the current services, check the current status of the specified
   * url and update the service entry
   */
  private void updateServices() {
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-services");
    vertx.eventBus().request(DBQueue, null, options, reply -> {
      if (reply.failed()) {
        LOGGER.error("Could not fetch services for scheduled update");
        return;
      }
      JsonObject body = (JsonObject) reply.result().body();

      Gson gson = new Gson();
      Type serviceListType = new TypeToken<ArrayList<Service>>() {
      }.getType();
      ArrayList<Service> services = gson.fromJson(String.valueOf(body.getJsonArray("services")), serviceListType);

      // Check status of all services
      services.forEach(service -> {
        Single<HttpResponse<Buffer>> statusSingle = client
          .getAbs(service.Url)
          .timeout(config().getInteger(CONFIG_TIMEOUT, 4000))
          .rxSend();

        statusSingle
          .map(response -> {
            int statusCode = response.statusCode();
            String status = "FAIL";
            if (statusCode >= 200 && statusCode < 300) {
              status = "OK";
            }
            return status;
          })
          .onErrorReturn(id -> "FAIL")
          .subscribe(status -> {
            // Update status of services
            JsonObject json = new JsonObject();
            json.put("id", service.Id);
            json.put("url", service.Url);
            json.put("name", service.Name);
            json.put("status", status);
            json.put("updateStatus", true);

            DeliveryOptions updateOptions = new DeliveryOptions().addHeader("action", "update-service");
            vertx.eventBus().request(DBQueue, json, updateOptions);
          });
      });
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

    // Fetch service url and name
    String serviceUrl = body.getString("service");
    String name = body.getString("name");

    // Validate url format
    Boolean validUrl = this.validateUrl(serviceUrl, context);
    if (!validUrl) return;

    // Get status of service
    Single<HttpResponse<Buffer>> statusSingle = client
      .getAbs(serviceUrl)
      .timeout(config().getInteger(CONFIG_TIMEOUT, 4000))
      .rxSend();

    statusSingle
      .map(response -> {
        int statusCode = response.statusCode();
        String status = "FAIL";
        if (statusCode >= 200 && statusCode < 300) {
          status = "OK";
        }
        return status;
      })
      .onErrorReturn(id -> "FAIL")
      .subscribe(status -> {
        JsonObject json = new JsonObject();
        json.put("url", serviceUrl);
        json.put("name", name);
        json.put("status", status);

        DeliveryOptions options = new DeliveryOptions().addHeader("action", "add-service");
        vertx.eventBus().request(DBQueue, json, options, reply -> {
          if (reply.failed()) {
            context.fail(reply.cause());
            return;
          }

          System.out.println(reply.result());

          context.response().setStatusCode(204);
          context.response().end();
        });
      });
  }

  private void serviceDeletionHandler(RoutingContext context) {
    // Get values
    JsonObject body = context.getBodyAsJson();

    // Fetch id
    String id = body.getString("id");

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

    // Fetch service id, url and name
    String id = body.getString("id");
    String serviceUrl = body.getString("service");
    String name = body.getString("name");

    // Validate url format
    Boolean validUrl = this.validateUrl(serviceUrl, context);
    if (!validUrl) return;

    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("url", serviceUrl);
    json.put("name", name);

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

  /**
   * Validate url, send error to user and return if url is valid or not
   *
   * @param url     - url to be validated
   * @param context - Routing context
   * @return - url validity
   */
  private Boolean validateUrl(String url, RoutingContext context) {
    // Validate url format
    UrlValidator urlValidator = new UrlValidator();
    if (!urlValidator.isValid(url)) {
      JsonObject badRequest = new JsonObject();
      badRequest.put("message", "The url for the service is incorrect. Please check the spelling of the url.");
      context.response().setStatusCode(400);
      context.response().end(badRequest.encodePrettily());
      return false;
    }
    return true;
  }
}

