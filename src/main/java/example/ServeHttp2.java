package example;

import example.FortuneDatabase;
import example.ServeCommon;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ServeHttp2 {

  public ServeHttp2() {}

  public static NettyOutbound processPostV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> monoString = ServeCommon.getFormData(request);
    // Mono<String> monoString = ServeCommon.getMonoStringFromFlux(request);
    ServeCommon.setCommonHeaders(response);
    return response.sendString(monoString);
  }

  public static NettyOutbound okResponseV2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    //String responseText = ServeCommon.responseText();
    Mono<String> responseContent = ServeCommon.responseTextR2DBC();
    System.out.println(request.path().toString() + " HTTP/2");

    response.header("content-type", "text/html");
    // response.header("content-length", Integer.toString(responseText.length()));
    // responseContent = Mono.just(responseText);
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000, h3-29=\":443\"; ma=2592000, h2=\":443\"; ma=1"
    );

    return response.sendString(responseContent);
  }
}
