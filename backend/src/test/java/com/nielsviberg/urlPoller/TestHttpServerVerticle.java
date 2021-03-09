package com.nielsviberg.urlPoller;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestHttpServerVerticle {
  @Test
  public void testAddAndGetService(Vertx vertx, VertxTestContext testContext) {
    final JsonObject newService = new JsonObject().put("name", "Kry").put("service", "https://kry.se");
    WebClient client = WebClient.create(vertx);
    client
      .post(8888, "localhost", "/")
      .sendJson(
        newService
      )
      .onComplete(res -> {
        System.out.println(res.result().statusCode());
        client.get(8888, "localhost", "/").send().onComplete(resGet -> {
          // Get the previously inserted service name and url
          JsonObject service = resGet.result().bodyAsJsonObject().getJsonArray("services").getJsonObject(0);
          String name = service.getValue("Name").toString();
          String url = service.getValue("Url").toString();

          // Adding of service should be successful
          assert (res.result().statusCode() == 204);
          // Fetching of services should be successful
          assert (resGet.result().statusCode() == 200);
          // Service should have the expected name
          assert (name.equals("Kry"));
          // Service should have the expected url
          assert (url.equals("https://kry.se"));

          testContext.completeNow();
        });
      });
  }
}
