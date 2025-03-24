package example;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.*;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.Http3SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

// This is basically straight from the Reactor examples
// https://github.com/reactor/reactor-netty/tree/main/reactor-netty-examples/src/main/java/reactor/netty/examples/http
public final class ReactorHttp3Experiment {

  static final boolean SECURE = true;
  static final int PORT = 8443;
  static final boolean WIRETAP = true;
  static final boolean COMPRESS = true;

  public static void main(String[] args) throws Exception {
    HttpServer serverV11 = HttpServer.create()
      .port(80)
      .wiretap(WIRETAP)
      .compress(false)
      .route(routes ->
        routes.route(r -> true, ReactorHttp3Experiment::okResponseV11)
      );

    HttpServer serverV2 = HttpServer.create()
      .port(PORT)
      .wiretap(WIRETAP)
      .compress(COMPRESS)
      .route(routes ->
        routes.route(r -> true, ReactorHttp3Experiment::okResponseV2)
      );

    serverV2 = serverV2.secure(spec ->
      spec.sslContext(
        Http2SslContextSpec.forServer(
          new File("certs.pem"),
          new File("key.pem")
        )
      )
    );
    serverV2 = serverV2.protocol(HttpProtocol.H2);

    HttpServer serverV3 = HttpServer.create()
      .port(PORT)
      .wiretap(WIRETAP)
      .compress(COMPRESS)
      .route(routes ->
        routes.route(r -> true, ReactorHttp3Experiment::okResponseV3)
      );

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

    serverV11.bindNow().onDispose().block();
    serverV2.bindNow().onDispose().block();
    serverV3.bindNow().onDispose().block();
  }

  private static NettyOutbound okResponseV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String responseText = new String(
      "<html><a href=\"/fortune\">fortune</a></html>"
    );
    response.status(426);
    response.header("Content-Type", "text/html");
    response.header("Content-Length", Integer.toString(responseText.length()));
    response.header("Upgrade-Insecure-Requests", "1");
    Mono<String> responseContent;
    responseContent = Mono.just(responseText);
    return response.sendString(responseContent);
  }

  private static NettyOutbound okResponseV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String responseText = new String(
      "<html><a href=\"/fortune\">fortune</a></html>"
    );
    response.status(200);
    response.header("Content-Type", "text/html");
    response.header("Content-Length", Integer.toString(responseText.length()));
    response.header(
      "Alt-Svc",
      "h3=\":443\"; ma=86400, h3-29=\":443\"; ma=86400, h3-Q050=\":443\"; ma=86400, h3-Q046=\":443\"; ma=86400, h3-Q043=\":443\"; ma=86400, quic=\":443\"; ma=86400; v=\"43,46\""
    );
    response.header("Application-Protocol", "h3,quic,h2,http/1.1");
    Mono<String> responseContent;
    responseContent = Mono.just(responseText);
    return response.sendString(responseContent);
  }

  private static NettyOutbound okResponseV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String responseText = new String(
      "<html><a href=\"/fortune\">fortune</a></html>"
    );
    response.status(200);
    response.header("Content-Type", "text/html");
    response.header("Content-Length", Integer.toString(responseText.length()));
    response.header("Alt-Svc", "h3=\":443\"; ma=86400");
    Mono<String> responseContent;
    responseContent = Mono.just(responseText);
    return response.sendString(responseContent);
  }
}
