package example;

import example.FortuneDatabase;
import io.netty.buffer.ByteBuf;
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
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.DisposableServer;
import reactor.netty.NettyOutbound;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.Http3SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ServeHttp3 {

  public ServeHttp3() {}

  public static NettyOutbound processPostV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = ServeCommon.getFormData(request);
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
    response.header("content-length", "0");

    // ServeCommon.addPostToDatabase(request);
    return response.sendString(monoString);
  }

  public static NettyOutbound okResponseV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String responseText = ServeCommon.responseText();
    Mono<String> responseContent;
    response.header("content-type", "text/html");
    response.header("content-length", Integer.toString(responseText.length()));
    responseContent = Mono.just(responseText);
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h3-29=\":443\"; ma=2592000; persist=1, h2=\":443\" ma=1"
    );

    return response.sendString(responseContent);
  }
}
