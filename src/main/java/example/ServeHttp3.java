package example;

import example.FortuneDatabase;
import example.impl.BooleanObject;
import example.impl.ContentData;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
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

public class ServeHttp3 {

  public ServeHttp3() {}

  public static NettyOutbound processPostV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Post HTTP/3");
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
    return response.sendString(monoString);
  }

  public static NettyOutbound processGetV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Get HTTP/3");
    response.addCookie(ServeCommon.generateSessionId());
    Mono<String> responseContent = ServeCommon.responseTextR2DBC(
      request,
      response
    );

    return response.sendString(responseContent);
  }

  // Record the session ID, and map of key/value pairs
  // Map<String, Map<String, String>> data = new Map<>();
  // Although I could store it as {"SESSIONID": "SessionID","whatever": "value"}
  // This would make it easier to pass around, but sessionid is no longer associated tightly with the data
  // And there would be no way to know if sessionid was passed in from a user
  public static NettyOutbound processPutV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    ContentData contentData = new ContentData(request);
    Mono<ContentData> contentDataMono = Mono.just(contentData);
    // Now I just need to chain everything together
    // Get the data.
    // Validate the data.
    // Call the routines to add to DB
    Mono<String> returnContent = contentDataMono
      .flatMap(cdm -> cdm.checkAuthentication("Token"))
      .flatMap(cdm -> cdm.checkSESSIONID())
      .flatMap(cdm -> {
        return cdm.processData();
      });
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println("Put HTTP/3");
    // Disposable testAuthenticated =
    // We know if the request is authenticated, but how to unwrap the value?
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
    // 422 Unprocessable Content if SESSIONID is missing
    BooleanObject sessionidResult = new BooleanObject();
    Mono.just(request)
      .flatMap(aRequest ->
        ServeCommon.checkSESSIONIDCookie(aRequest, sessionidResult)
      )
      .then()
      .subscribe();
    if (sessionidResult.getValue() == false) {
      response.status(422);
      response.header("content-type", "text/html");
      return response.sendString(
        Mono.just("<html>SESSIONID is missing</html>")
      );
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
