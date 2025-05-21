package example;

import example.FortuneDatabase;
import example.ServeCommon;
import io.netty.channel.ChannelOption;
import io.netty.resolver.HostsFileEntriesProvider.Parser;
import java.time.Duration;
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

public class ServeHttp11 {

  public ServeHttp11() {}

  public static NettyOutbound processPostV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = Flux.from(
      ServeCommon.getFormData(request, response)
    ).next();

    return response.sendString(monoString);
  }

  public static NettyOutbound okResponseV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> responseContent = ServeCommon.responseTextR2DBC()
      .subscribeOn(Schedulers.boundedElastic());
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

    response.header("content-type", "text/html");
    response.header("upgrade", "h3, h2");
    response.header("connection", "Upgrade");
    response.header("alt-svc", "clear");
    return response.sendString(responseContent);
  }
}
