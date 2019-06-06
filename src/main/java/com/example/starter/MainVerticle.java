package com.example.starter;

import com.example.starter.util.Runner;
import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;

import java.util.Random;

public class MainVerticle extends AbstractVerticle {

  private static final String CLIENT1 = "client1";
  private static final String CLIENT2 = "client2";
  private int success = 0;
  private int failures = 0;

  public static void main(String[] args) {
    Runner.runExample(MainVerticle.class);
  }

  @Override
  public void start(Future<Void> startFuture) {

    HttpClientOptions options = new HttpClientOptions()
      .setKeepAlive(false)
      .setLogActivity(true)
      .setMaxPoolSize(5)
      .setIdleTimeout(1)
      .setConnectTimeout(800)
      .setDefaultHost("httpbin.org")
      .setDefaultPort(80);

    HttpClient client1 = vertx.createHttpClient(options);
    HttpClient client2 = vertx.createHttpClient(options);

    Vertx vertx = Vertx.vertx(new VertxOptions()
        .setMaxWorkerExecuteTime(600000000000L)
        .setEventLoopPoolSize(1));

    vertx.executeBlocking(future -> {
      testClient(client1, CLIENT1);
      future.complete();
    }, res -> {
    });

    vertx.executeBlocking(future -> {
      testClient(client2, CLIENT2);
      future.complete();
    }, res -> {
    });
  }

  private void testClient(HttpClient client, String name) {
    System.out.println(name);
    Random randomizer = new Random();

    while (success + failures < 1000) {
      int random = randomizer.nextInt(1000 * 3);
      HttpClientRequest request = client.request(HttpMethod.GET, "/get");
      request.setTimeout(800);

      request.handler(t1 -> {
        success++;
        System.out.println("[" + name + "] SUCCESS! " + success + "/" + failures);
      });

      request.exceptionHandler(t -> {
        failures++;
        System.out.println("Connection=" + System.identityHashCode(request.connection()));
        System.out.println("[" + name + "] FAILURE! " + success + "/" + failures);
      });

      request.end();

      try {
        Thread.sleep(random);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
