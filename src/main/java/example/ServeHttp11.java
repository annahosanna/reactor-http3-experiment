package example;

import example.FortuneDatabase;
import example.ServeCommon;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
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
    Mono<String> responseContent = ServeCommon.responseTextR2DBC();
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
    response.header("upgrade", "h3, h2");
    response.header("connection", "Upgrade");
    response.header("alt-svc", "clear");
    return response.sendString(responseContent);
  }
}
