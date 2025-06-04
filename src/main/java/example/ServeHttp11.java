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

public class ServeHttp11 {

  public ServeHttp11() {}

  // https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#alpn-protocol-ids
  // https://www.rfc-editor.org/rfc/rfc9110.html#POST
  // If appropriate 201 can be returned with the url of the location to access the data
  // 302 is probably a good idea (no response content)
  // Delete method is the opposite where a url to lose access to is specified
  public static NettyOutbound processPostV11(
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
    return response.sendString(Mono.just(""));
  }

  // Get is cachable unless cache-control header is set such as no-cache
  // 200 success, 415 Unsupported Media Type. 422 Unprocessable Content
  // A URI does not have to be a URL and thus depending on protocol may not have a hostname
  // Also check out org.apache.commons.validator.routines.UrlValidator
  // java.net.URL
  // https://github.com/apache/commons-validator/blob/master/src/main/java/org/apache/commons/validator/routines/UrlValidator.java
  public static NettyOutbound okResponseV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> responseContent = ServeCommon.responseTextR2DBC(
      request,
      response
    ).subscribeOn(Schedulers.boundedElastic());
    System.out.println(
      request.hostName().toString() +
      " " +
      request.path().toString() +
      " HTTP 1.1"
    );

    response.header("alt-svc", "h3=\":443\"; ma=2592000, h2=\":443\"; ma=1");
    return response.sendString(responseContent);
  }

  // 200 Update, 201 Create, 204 No Content (use 204 to indicate success, but no response content)
  // 415 Unsupported Media Type if not application/json or bad json
  public static NettyOutbound processRestPutV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = Flux.from(
      ServeCommon.processPutData(request, response)
    ).next();

    // response.status(204);
    return response.sendString(Mono.just(""));
  }
}
