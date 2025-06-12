package example;

import example.FortuneDatabase;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.resolver.HostsFileEntriesProvider.Parser;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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

public class ServeHttp3 {

  public ServeHttp3() {}

  public static NettyOutbound processPostV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Post HTTP/3");
    Mono<String> monoString = Flux.from(
      ServeCommon.getFormData(request, response)
    )
      .next()
      .flatMap(data -> Mono.just(""));

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

  public static NettyOutbound processGetV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Get HTTP/3");
    Mono<String> responseContent = ServeCommon.responseTextR2DBC(
      request,
      response
    ).subscribeOn(Schedulers.boundedElastic());
    response.header("content-type", "text/html");
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );

    return response.sendString(responseContent);
  }

  public static NettyOutbound processPutV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Put HTTP/3");
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
