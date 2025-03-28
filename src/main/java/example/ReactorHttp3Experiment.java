package example;

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
    HttpServer serverV11 = HttpServer.create()
      .port(80)
      .wiretap(false)
      .compress(true)
      .route(routes ->
        routes.route(r -> true, ReactorHttp3Experiment::okResponseV11)
      );

    serverV11 = serverV11.protocol(HttpProtocol.HTTP11);
    DisposableServer disposableServerV11 = serverV11.bindNow();

    HttpServer serverV2 = HttpServer.create()
      .port(PORT)
      .wiretap(true)
      .compress(COMPRESS)
      .route(routes ->
        routes.route(r -> true, ReactorHttp3Experiment::okResponseV2)
      );

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
    DisposableServer disposableServerV2 = serverV2.bindNow();

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
          .get("/fortune", ReactorHttp3Experiment::okResponseV3)
          .post("/fortune", ReactorHttp3Experiment::okResponseV3)
      )
      .bindNow();

    Mono.when(
      disposableServerV11.onDispose(),
      disposableServerV2.onDispose(),
      disposableServerV3.onDispose()
    ).block();
  }

  private static NettyOutbound processPostV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Flux<String> mapContent = request
      .receiveForm()
      .flatMap(data -> {
        try {
          return Mono.just("[" + data.getString() + "]");
        } catch (IOException e) {
          return Mono.just("[]");
        }
      });
    List<String> output = new ArrayList<>();
    mapContent.subscribe(output::add);
    String[] stringArray = output.toArray(new String[0]);
    String longString = new String();
    for (int i = 0; i < stringArray.length; i++) {
      longString.concat(stringArray[i] + " ");
    }
    Mono<String> responseContent = Mono.just(longString);
    return response.sendString(responseContent);
  }

  private static NettyOutbound okResponseV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String imageText = new String(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>"
    );
    String responseText = new String(
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">fortune</a></body></html>"
    );
    Mono<String> responseContent;
    System.out.println(
      request.hostName().toString() +
      " " +
      request.path().toString() +
      " HTTP 1.1"
    );

    response.status(301);
    try {
      response.header(
        "location",
        "https://" +
        java.net.InetAddress.getLocalHost().getHostName() +
        "/fortune"
      );
    } catch (Exception e) {
      response.header("location", "https://localhost/fortune");
    }

    if (
      request
        .path()
        .toString()
        .strip()
        .toLowerCase()
        .equals("favicon.ico".strip().toLowerCase()) ==
      true
    ) {
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
    // response.header("ipgrade-insecure-requests", "1");
    response.header("upgrade", "h3, h2");
    response.header("connection", "Upgrade");

    response.header("alt-svc", "clear");
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
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">fortune</a></body></html>"
    );
    Mono<String> responseContent;
    System.out.println(request.path().toString() + " HTTP/2");
    if (
      request
        .path()
        .toString()
        .strip()
        .toLowerCase()
        .equals("favicon.ico".strip().toLowerCase()) ==
      true
    ) {
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
      "h3=\":443\"; ma=2592000, h3-29=\":443\"; ma=2592000, h2=\":443\"; ma=1"
    );

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
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">fortune</a></body></html>"
    );
    Mono<String> responseContent;
    System.out.println(request.path().toString() + " HTTP/3");
    if (
      request
        .path()
        .toString()
        .strip()
        .toLowerCase()
        .equals("favicon.ico".strip().toLowerCase()) ==
      true
    ) {
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
      "h3=\":443\"; ma=2592000; persist=1, h3-29=\":443\"; ma=2592000; persist=1, h2=\":443\" ma=1"
    );

    return response.sendString(responseContent);
  }
}
