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
import reactor.netty.DisposableServer;
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

    serverV11 = serverV11.protocol(HttpProtocol.HTTP11);
    DisposableServer disposableServerV11 = serverV11.bindNow();

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
    DisposableServer disposableServerV2 = serverV2.bindNow();

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
    DisposableServer disposableServerV3 = serverV3.bindNow();

    Mono.when(
      disposableServerV11.onDispose(),
      disposableServerV2.onDispose(),
      disposableServerV3.onDispose()
    ).block();
    disposableServerV11.onDispose().block();
    disposableServerV2.onDispose().block();
    disposableServerV3.onDispose().block();
  }

  private static NettyOutbound okResponseV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String imageText = new String(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>"
    );
    String responseText = new String(
      "<!doctype html><html><a href=\"/fortune\">fortune</a></html>"
    );
    Mono<String> responseContent;
    System.out.println(request.hostName().toString());
    /*
     // For some reason this is redirecting even when the server is localhost
    if (request.hostName().toString() != "localhost") {
      response.status(301);
      response.header("location", "localhost");
    } else {
      response.status(426);
    }
    */
    response.status(426);
    
    System.out.println(request.path().toString());
    if (request.path().toString() == "/favicon.ico") {
      response.header("content-type", "image/svg+xml");
      response.header("content-length", Integer.toString(imageText.length()));
      responseContent = Mono.just(imageText);
    } else {
      response.header("Content-Type", "text/html");
      response.header(
        "content-Length",
        Integer.toString(responseText.length())
      );
      responseContent = Mono.just(responseText);
    }
    // response.header("ipgrade-insecure-requests", "1");
    response.header("upgrade", "HTTP/3.0");
    response.header("connection", "Upgrade");

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000,h3-29=\":443\"; ma=2592000"
    );
    return response.sendString(responseContent);
  }

  private static NettyOutbound okResponseV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String imageText = new String(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>"
    );
    String responseText = new String(
      "<!doctype html><html><a href=\"/fortune\">fortune</a></html>"
    );
    Mono<String> responseContent;
    System.out.println(request.path().toString());
    /*
    if (request.hostName().toString() != "localhost") {
      response.status(301);
      response.header("location", "localhost");
    } else {
      response.status(200);
    }
    */
    if (request.path().toString() == "/favicon.ico") {
      response.header("content-type", "image/svg+xml");
      response.header("content-length", Integer.toString(imageText.length()));
      responseContent = Mono.just(imageText);
    } else {
      response.header("content-type", "text/html");
      response.header(
        "content-length",
        Integer.toString(responseText.length())
      );
      responseContent = Mono.just(responseText);
    }

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000,h3-29=\":443\"; ma=2592000"
    );
    // response.header("Application-Protocol", "h3,quic,h2,http/1.1");

    return response.sendString(responseContent);
  }

  private static NettyOutbound okResponseV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String imageText = new String(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>"
    );
    String responseText = new String(
      "<!doctype html><html><a href=\"/fortune\">fortune</a></html>"
    );
    Mono<String> responseContent;
    /*
    if (request.hostName().toString() != "localhost") {
      response.status(301);
      response.header("location", "localhost");
    } else {
      response.status(200);
    }
    */
    System.out.println(request.path().toString());
    if (request.path().toString() == "/favicon.ico") {
      response.header("content-type", "image/svg+xml");
      response.header("content-Length", Integer.toString(imageText.length()));
      responseContent = Mono.just(imageText);
    } else {
      response.header("content-type", "text/html");
      response.header(
        "content-length",
        Integer.toString(responseText.length())
      );
      responseContent = Mono.just(responseText);
    }

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000,h3-29=\":443\"; ma=2592000"
    );

    responseContent = Mono.just(responseText);
    return response.sendString(responseContent);
  }
}
