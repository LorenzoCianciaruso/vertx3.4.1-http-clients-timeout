package com.example.starter;

import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.Match;
import io.vertx.ext.dropwizard.MetricsService;

import java.util.Random;

public class MainVerticle extends AbstractVerticle {

  public static final String CLIENT1 = "client1";
  public static final String CLIENT2 = "client2";
  private int success = 0;
  private int failures = 0;

  @Override
  public void start(Future<Void> startFuture) {

    HttpClientOptions options1 = new HttpClientOptions()
      .setKeepAlive(false)
      .setLogActivity(true)
      .setMaxPoolSize(5)
      .setIdleTimeout(1)
      .setConnectTimeout(800)
      .setDefaultHost("httpbin.org")
      .setDefaultPort(80)
      .setMetricsName(CLIENT1);

    HttpClientOptions options2 = new HttpClientOptions()
      .setKeepAlive(false)
      .setLogActivity(true)
      .setMaxPoolSize(5)
      .setIdleTimeout(1)
      .setConnectTimeout(800)
      .setDefaultHost("httpbin.org")
      .setDefaultPort(80)
      .setMetricsName(CLIENT2);

    Vertx vertxWithMetrics = Vertx.vertx(new VertxOptions()
      .setMaxWorkerExecuteTime(600000000000L)
      .setEventLoopPoolSize(1)
      .setMetricsOptions(
      new DropwizardMetricsOptions()
        .setEnabled(true)
        .setJmxEnabled(true)
        .setJmxDomain("vertx")
        .addMonitoredHttpServerUri(new Match().setValue("/"))
        .addMonitoredHttpClientEndpoint(
          new Match()
            .setValue("https://httpbin.org"))
        .setRegistryName("vertx-registry")
    ));

    HttpClient client1 = vertxWithMetrics.createHttpClient(options1);
    HttpClient client2 = vertxWithMetrics.createHttpClient(options2);

    vertxWithMetrics.executeBlocking(future -> testClient(client1, vertxWithMetrics, future, CLIENT1), res -> {
    });

    vertxWithMetrics.executeBlocking(future -> testClient(client2, vertxWithMetrics, future, CLIENT2), res -> {
    });
  }

  private void testClient(HttpClient client, Vertx vertxWithMetrics, Future<Object> future, String name) {
    Random randomizer = new Random();

    while (failures < 100) {
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

      MetricsService metricsService = MetricsService.create(vertxWithMetrics);
      JsonObject metrics = metricsService.getMetricsSnapshot(vertxWithMetrics);
      System.out.println(metrics);

      request.end();

      try {
        Thread.sleep(random);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    future.complete();
  }
}
