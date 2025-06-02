package example;

import example.FortuneDatabase;
import example.ServeCommon;
import io.netty.channel.ChannelOption;
import java.io.File;
import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.Http3SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

public final class ReactorHttp3Experiment {

  static final boolean SECURE = true;
  static final int PORT = 443;
  static final boolean WIRETAP = false;
  static final boolean COMPRESS = true;

  public static void main(String[] args) throws Exception {
    FortuneDatabase fortuneDatabase = new FortuneDatabase();
    HttpServer serverV11 = HttpServer.create()
      .port(80)
      .wiretap(false)
      .compress(true)
      .protocol(HttpProtocol.HTTP11);

    serverV11.warmup();

    DisposableServer disposableServerV11 = serverV11
      .route(routes ->
        routes
          .get("/fortune", ServeHttp11::okResponseV11)
          .get("/favicon.ico", ServeCommon::returnFavicon)
          .post("/fortune", ServeHttp11::processPostV11)
          .put("/fortune", ServeHttp11::processPutV11)
      )
      .bindNow();

    HttpServer serverV2 = HttpServer.create()
      .port(PORT)
      .wiretap(false)
      .compress(COMPRESS)
      .protocol(HttpProtocol.H2)
      .secure(spec ->
        spec.sslContext(
          Http2SslContextSpec.forServer(
            new File("certs.pem"),
            new File("key.pem")
          )
        )
      );

    serverV2.warmup();

    DisposableServer disposableServerV2 = serverV2
      .route(routes ->
        routes
          .get("/fortune", ServeHttp2::okResponseV2)
          .get("/favicon.ico", ServeCommon::returnFavicon)
          .post("/fortune", ServeHttp2::processPostV2)
      )
      .bindNow();

    HttpServer serverV3 = HttpServer.create()
      .port(PORT)
      .wiretap(false)
      .compress(COMPRESS)
      .secure(spec ->
        spec.sslContext(
          Http3SslContextSpec.forServer(
            new File("key.pem"),
            null,
            new File("certs.pem")
          )
        )
      )
      .protocol(HttpProtocol.HTTP3)
      .http3Settings(spec ->
        spec
          .idleTimeout(Duration.ofSeconds(5))
          .maxData(10000000)
          .maxStreamDataBidirectionalLocal(1000000)
          .maxStreamDataBidirectionalRemote(1000000)
          .maxStreamsBidirectional(100)
      );

    serverV3.warmup();

    DisposableServer disposableServerV3 = serverV3
      .route(routes ->
        routes
          .get("/fortune", ServeHttp3::okResponseV3)
          .get("/favicon.ico", ServeCommon::returnFavicon)
          .post("/fortune", ServeHttp3::processPostV3)
      )
      .bindNow();

    Mono.when(
      disposableServerV11.onDispose(),
      disposableServerV2.onDispose(),
      disposableServerV3.onDispose()
    ).block();
  }
}
