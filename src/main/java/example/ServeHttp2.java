package example;

import example.FortuneDatabase;
import example.ServeCommon;
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
// import java.util.concurrent.CountDownLatch;
// import java.util.function.Consumer;
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

public class ServeHttp2 {

  public ServeHttp2() {}

  public static NettyOutbound processPostV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    // ServeCommon.fixContentType(request);
    // System.out.println("Adding a fortune");
    Mono<String> monoString = ServeCommon.getFormData(request);
    monoString.subscribe(result -> { System.out.println("Lambda result: " + result);})
    return response.sendString(monoString);
  }

  public static NettyOutbound okResponseV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String responseText = ServeCommon.responseText();
    Mono<String> responseContent;
    System.out.println(request.path().toString() + " HTTP/2");

    response.header("content-type", "text/html");
    response.header("content-length", Integer.toString(responseText.length()));
    responseContent = Mono.just(responseText);
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000, h3-29=\":443\"; ma=2592000, h2=\":443\"; ma=1"
    );

    return response.sendString(responseContent);
  }
}
