package example;

import example.FortuneDatabase;
import example.ServeCommon;
import io.netty.channel.ChannelOption;
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

public class ServeHttp2 {

  public ServeHttp2() {}

  public static NettyOutbound processPostV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = Flux.from(
      ServeCommon.getFormData(request, response)
    ).next();
    Map<String, String> uriParams = request.params();
    uriParams.forEach((key, value) -> {
      System.out.println(key + ": " + value);
    });
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

  public static NettyOutbound okResponseV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    //String responseText = ServeCommon.responseText();
    Mono<String> responseContent = ServeCommon.responseTextR2DBC(
      request,
      response
    ).subscribeOn(Schedulers.boundedElastic());
    System.out.println(
      request.hostName().toString() +
      " " +
      request.path().toString() +
      " HTTP/2"
    );
    response.header("content-type", "text/html");
    response.header("alt-svc", "h3=\":443\"; ma=2592000, h2=\":443\"; ma=1");

    return response.sendString(responseContent);
  }

  public static NettyOutbound processPutV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = Flux.from(
      ServeCommon.processPutData(request, response)
    ).next();
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    response.status(200);
    return response.sendString(Mono.just(""));
  }
}
