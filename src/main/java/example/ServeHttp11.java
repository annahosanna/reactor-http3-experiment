package example;

import example.FortuneDatabase;
import example.ServeCommon;
import example.impl.BooleanObject;
import example.impl.ContentData;
import example.impl.WrappedString;
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
    ContentData contentData = new ContentData(request);
    Mono<ContentData> contentDataMono = Mono.just(contentData);
    WrappedString returnMessage = new WrappedString();
    Disposable returnContent = contentDataMono
      .flatMap(cdm -> cdm.validateMethod())
      .flatMap(cdm -> cdm.checkAuthentication())
      .flatMap(cdm -> cdm.checkSESSIONID())
      .flatMap(cdm -> cdm.processData())
      .subscribe(cdm -> {
        // Set each response field
        response.status(cdm.getResponseStatusCode());
        if (cdm.getResponseContentType() != null) {
          response.header("content-type", cdm.getResponseContentType());
        }
        if (cdm.getResponseCookie() != null) {
          response.addCookie(cdm.getResponseCookie());
        }
        returnMessage.setWrappedString(cdm.getResponseMessage());
      });
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println(request.method().name() + " HTTP/1.1");

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(Mono.just(returnMessage.getWrappedString()));
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
    ContentData contentData = new ContentData(request);
    Mono<ContentData> contentDataMono = Mono.just(contentData);
    WrappedString returnMessage = new WrappedString();
    Disposable returnContent = contentDataMono
      .flatMap(cdm -> cdm.validateMethod())
      .flatMap(cdm -> cdm.checkAuthentication())
      .flatMap(cdm -> cdm.checkSESSIONID())
      .flatMap(cdm -> cdm.processData())
      .subscribe();
    // .subscribe(cdm -> {
    //   // Set each response field
    //   response.status(cdm.getResponseStatusCode());
    //   if (cdm.getResponseContentType() != null) {
    //     response.header("content-type", cdm.getResponseContentType());
    //   }
    //   if (cdm.getResponseCookie() != null) {
    //     response.addCookie(cdm.getResponseCookie());
    //   }
    //   returnMessage.setWrappedString(cdm.getResponseMessage());
    // });
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println(request.method().name() + " HTTP/1.1");

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(Mono.just(returnMessage.getWrappedString()));
  }

  // 200 Update, 201 Create, 204 No Content (use 204 to indicate success, but no response content)
  // 415 Unsupported Media Type if not application/json or bad json
  public static NettyOutbound processRestPutV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    ContentData contentData = new ContentData(request);
    Mono<ContentData> contentDataMono = Mono.just(contentData);
    WrappedString returnMessage = new WrappedString();
    Disposable returnContent = contentDataMono
      .flatMap(cdm -> cdm.validateMethod())
      .flatMap(cdm -> cdm.checkAuthentication())
      .flatMap(cdm -> cdm.checkSESSIONID())
      .flatMap(cdm -> cdm.processData())
      .subscribe(cdm -> {
        // Set each response field
        response.status(cdm.getResponseStatusCode());
        if (cdm.getResponseContentType() != null) {
          response.header("content-type", cdm.getResponseContentType());
        }
        if (cdm.getResponseCookie() != null) {
          response.addCookie(cdm.getResponseCookie());
        }
        returnMessage.setWrappedString(cdm.getResponseMessage());
      });
    System.out.println("Client connected to " + request.hostName().toString());
    System.out.println(request.method().name() + " HTTP/1.1");

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(Mono.just(returnMessage.getWrappedString()));
  }
}
