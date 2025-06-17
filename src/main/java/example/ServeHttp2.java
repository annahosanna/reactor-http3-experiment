package example;

import example.FortuneDatabase;
import example.ServeCommon;
import example.impl.BooleanObject;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.resolver.HostsFileEntriesProvider.Parser;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.NettyOutbound;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.resources.LoopResources;

public class ServeHttp2 {

  public ServeHttp2() {}

  public static NettyOutbound processPostV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Post HTTP/2");
    // Wow adding a flatMap must be the weirdest work around ever
    Mono<String> monoString = Flux.from(
      ServeCommon.getFormData(request, response)
    )
      .next()
      .flatMap(data -> Mono.just(""));

    // When I added block it worked, but block()/blockFirst()/blockLast() are not supported

    // Need to find another way to get hostname
    response.header(
      "location",
      "https://" + request.hostName().toString() + "/fortune"
    );
    response.status(302);
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(monoString);
  }

  public static NettyOutbound processGetV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Get HTTP/2");
    Mono<String> responseContent = ServeCommon.responseTextR2DBC(
      request,
      response
    );

    return response.sendString(responseContent);
  }

  public static NettyOutbound processPutV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Put HTTP/2");
    BooleanObject authenticatedResult = new BooleanObject();
    Mono.just(request)
      .flatMap(aRequest ->
        ServeCommon.checkAuthenticationHeader(aRequest, authenticatedResult)
      )
      .then()
      .subscribe();
    if (authenticatedResult.getValue() == false) {
      response.status(401);
      response.header("content-type", "text/html");
      return response.sendString(Mono.just("<html>Access Denied</html>"));
    }
    if (
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE) != null &&
      request
        .requestHeaders()
        .get(HttpHeaderNames.CONTENT_TYPE)
        .toLowerCase()
        .startsWith(HttpHeaderValues.APPLICATION_JSON.toString().toLowerCase())
    ) {} else {
      response.status(415);
      return response.sendString(Mono.just(""));
    }

    Mono<String> monoString = Flux.from(
      ServeCommon.processPutData(request, response)
    )
      .last()
      .flatMap(data -> Mono.just(""));
    response.status(204);
    response.header("content-type", "application/json");
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(monoString);
  }
}
