package com.nielsviberg.urlPoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    vertx.deployVerticle(new DatabaseVerticle(), res -> {
      if (res.failed()) {
        startPromise.fail(res.cause());
        return;
      }
      vertx.deployVerticle(new HttpServerVerticle(), res2 -> {
        if (res2.failed()) {
          startPromise.fail(res2.cause());
          return;
        }
        startPromise.complete();
      });
    });
  }
}
