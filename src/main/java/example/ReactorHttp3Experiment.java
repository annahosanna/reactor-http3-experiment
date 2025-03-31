package example;

import example.FortuneDatabase;
import example.ServeCommon;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.NettyOutbound;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.Http3SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public final class ReactorHttp3Experiment {

  static final boolean SECURE = true;
  static final int PORT = 443;
  static final boolean WIRETAP = false; // Network traffic does not need to be logged
  static final boolean COMPRESS = true;

  public static void main(String[] args) throws Exception {
    FortuneDatabase fortuneDatabase = new FortuneDatabase();
    HttpServer serverV11 = HttpServer.create()
      .port(80)
      .wiretap(false)
      .compress(true); //      .route(routes -> routes.route(r -> true, // ServeHttp11::okResponseV11))

    serverV11 = serverV11.protocol(HttpProtocol.HTTP11);
    DisposableServer disposableServerV11 = serverV11
      .route(routes ->
        routes
          .get("/fortune", ServeHttp11::okResponseV11)
          .get("/favicon.ico", ServeCommon::returnFavicon)
          .post("/fortune", ServeHttp11::processPostV11)
      )
      .bindNow();

    HttpServer serverV2 = HttpServer.create()
      .port(PORT)
      .wiretap(true)
      .compress(COMPRESS); //      .route(routes -> routes.route(r -> true, ServeHttp2::okResponseV2))

    // Deprecated: SslProvider.SslContextSpec.sslContext(SslProvider.ProtocolSslContextSpec)
    // Instead the replacements are: https://projectreactor.io/docs/netty/release/api/reactor/netty/tcp/SslProvider.GenericSslContextSpec.html
    // Use: reactor.netty.http.Http2SslContextSpec
    serverV2 = serverV2.secure(spec ->
      spec.sslContext(
        Http2SslContextSpec.forServer(
          new File("certs.pem"),
          new File("key.pem")
        )
      )
    );
    serverV2 = serverV2.protocol(HttpProtocol.H2);
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
      .compress(COMPRESS);
    //      .route(routes ->
    //        routes.route(r -> true, ReactorHttp3Experiment::okResponseV3)
    //      );

    serverV3 = serverV3.secure(spec ->
      spec.sslContext(
        Http3SslContextSpec.forServer(
          new File("key.pem"),
          null,
          new File("certs.pem")
        )
      )
    );

    // QUIC does not know what the idletimeout etc should be, since it doesn't know HTTP3 will be the upper layer. Those need to be adjusted here
    serverV3 = serverV3
      .protocol(HttpProtocol.HTTP3)
      .http3Settings(spec ->
        spec
          .idleTimeout(Duration.ofSeconds(5))
          .maxData(10000000)
          .maxStreamDataBidirectionalLocal(1000000)
          .maxStreamDataBidirectionalRemote(1000000)
          .maxStreamsBidirectional(100)
      );
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
