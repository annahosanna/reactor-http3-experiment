package example;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

public final class MVP {

  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create()
      .port(80)
      .wiretap(false)
      .compress(false)
      .protocol(HttpProtocol.HTTP11);
    server.warmup();
    DisposableServer disposableServer = server
      .route(routes ->
        routes.post("/fortune", (req, res) -> {
          Mono<String> returnContent = req
            .receive()
            .aggregate()
            .asString()
            .filter(str -> str.length() > 0);
          return res.sendString(returnContent);
        })
      )
      .bindNow();
    Mono.when(disposableServer.onDispose()).block();
  }
}
