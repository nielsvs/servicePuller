package com.nielsviberg.urlPoller;

import io.reactivex.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.Vertx;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Vertx vertx = Vertx.vertx();
    Single<String> dbVerticleDeployment = vertx.rxDeployVerticle(new DatabaseVerticle());

    // Ensure that both the database and the http server verticles are deployed properly
    dbVerticleDeployment.flatMap(id -> {
      Single<String> httpVerticleDeployment = vertx.rxDeployVerticle(
        new HttpServerVerticle());
      return httpVerticleDeployment;
    }).subscribe(id -> startPromise.complete(), startPromise::fail);
  }
}
