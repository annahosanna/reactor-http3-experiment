package example;

import example.FortuneDatabase;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ServeHttp3 {

  public ServeHttp3() {}

  public static NettyOutbound processPostV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = ServeCommon.getFormData(request);
    ServeCommon.setCommonHeaders(response);
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
