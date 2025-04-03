package example;

import example.FortuneDatabase;
import example.ServeCommon;
import io.netty.handler.codec.http.HttpContent;
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

public class ServeHttp11 {

  public ServeHttp11() {}

  public static NettyOutbound processPostV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = ServeCommon.getFormData(request);
    ServeCommon.setCommonHeaders(response);
    return response.sendString(monoString);
  }

  public static NettyOutbound okResponseV11(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String responseText = ServeCommon.responseText();
    Mono<String> responseContent;
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
    response.header("content-length", Integer.toString(responseText.length()));
    responseContent = Mono.just(responseText);
    // response.header("ipgrade-insecure-requests", "1");
    response.header("upgrade", "h3, h2");
    response.header("connection", "Upgrade");
    response.header("alt-svc", "clear");
    return response.sendString(responseContent);
  }
}
