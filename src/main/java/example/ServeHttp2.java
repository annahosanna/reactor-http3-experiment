package example;

import example.FortuneDatabase;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
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

public class ServeHttp2 {

  public ServeHttp2() {}

  public static NettyOutbound okResponseV2(
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
}
