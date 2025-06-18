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
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Post HTTP/1.1");
    // response.addCookie(ServeCommon.generateSessionId());
    Mono<String> monoString = Flux.from(
      ServeCommon.getFormData(request, response)
    )
      .next()
      .flatMap(data -> Mono.just(""));

    // The session cookie should have been sent by the client automatically
    Cookie sessionCookie = request.cookies().get("SESSIONID") != null
      ? request.cookies().get("SESSIONID").stream().findFirst().orElse(null)
      : null;
    // if (sessionCookie != null) {
    //   System.out.println("Session ID: " + sessionCookie.value());
    // }
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
    return response.sendString(Mono.just(""));
  }

  // Get is cachable unless cache-control header is set such as no-cache
  // 200 success, 415 Unsupported Media Type. 422 Unprocessable Content
  // A URI does not have to be a URL and thus depending on protocol may not have a hostname
  // Also check out org.apache.commons.validator.routines.UrlValidator
  // java.net.URL
  // https://github.com/apache/commons-validator/blob/master/src/main/java/org/apache/commons/validator/routines/UrlValidator.java
  public static NettyOutbound processGetV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Get HTTP/1.1");
    response.addCookie(ServeCommon.generateSessionId());
    Mono<String> responseContent = ServeCommon.responseTextR2DBC(
      request,
      response
    );
    return response.sendString(responseContent);
  }

  // 200 Update, 201 Create, 204 No Content (use 204 to indicate success, but no response content)
  // 415 Unsupported Media Type if not application/json or bad json
  public static NettyOutbound processRestPutV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Put HTTP/1.1");
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
