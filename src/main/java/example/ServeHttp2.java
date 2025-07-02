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
    System.out.println("Put HTTP/2");

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(Mono.just(returnMessage.getWrappedString()));
  }

  public static NettyOutbound processGetV2(
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
    System.out.println("Put HTTP/2");

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(Mono.just(returnMessage.getWrappedString()));
  }

  public static NettyOutbound processPutV2(
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
    System.out.println("Put HTTP/2");

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    return response.sendString(Mono.just(returnMessage.getWrappedString()));
  }
}
