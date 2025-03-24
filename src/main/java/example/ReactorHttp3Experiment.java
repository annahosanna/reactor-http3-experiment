package example;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.time.Duration;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.Http3SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

// This is basically straight from the Reactor examples
public final class ReactorHttp3Experiment {

  static final boolean SECURE = true;
  static final int PORT = 8443;
  static final boolean WIRETAP = true;
  static final boolean COMPRESS = true;

  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create()
      .port(PORT)
      .wiretap(WIRETAP)
      .compress(COMPRESS)
      .route(r ->
        r.post("/echo", (req, res) ->
          res.header(CONTENT_TYPE, TEXT_PLAIN).send(req.receive().retain())
        )
      );

    if (SECURE) {
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      if (HTTP2) {
        server = server.secure(spec ->
          spec.sslContext(
            Http2SslContextSpec.forServer(ssc.certificate(), ssc.privateKey())
          )
        );
      } else if (HTTP3) {
        server = server.secure(spec ->
          spec.sslContext(
            Http3SslContextSpec.forServer(ssc.key(), null, ssc.cert())
          )
        );
      } else {
        server = server.secure(spec ->
          spec.sslContext(
            Http11SslContextSpec.forServer(ssc.certificate(), ssc.privateKey())
          )
        );
      }
    }

    if (HTTP2) {
      server = server.protocol(HttpProtocol.H2);
    }

    if (HTTP3) {
      server = server
        .protocol(HttpProtocol.HTTP3)
        .http3Settings(spec ->
          spec
            .idleTimeout(Duration.ofSeconds(5))
            .maxData(10000000)
            .maxStreamDataBidirectionalLocal(1000000)
            .maxStreamDataBidirectionalRemote(1000000)
            .maxStreamsBidirectional(100)
        );
    }

    server.bindNow().onDispose().block();
  }
}
