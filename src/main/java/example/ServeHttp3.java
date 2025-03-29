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

public class ServeHttp3 {

  public ServeHttp3() {}

  public static void fixContentType(HttpServerRequest request) {
    // String contentType = request
    //  .requestHeaders()
    //  .get(HttpHeaderNames.CONTENT_TYPE);
    if (
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE) != null &&
      request
        .requestHeaders()
        .get(HttpHeaderNames.CONTENT_TYPE)
        .startsWith(
          HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()
        )
    ) {
      request
        .requestHeaders()
        .set(
          HttpHeaderNames.CONTENT_TYPE,
          HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()
        );
    }
  }

  public static NettyOutbound processPostV3D1(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println(
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE)
    );
    fixContentType(request);
    response.status(200);

    String contentText = new String();
    Flux<HttpData> fluxHttpData = request.receiveForm();
    fluxHttpData
      // flatMap
      .subscribe(result -> System.out.println("Result: " + result.getName()));
    response.status(200);
    Mono<String> responseContent = Mono.just(contentText);
    return response.sendString(responseContent);
  }

  public static NettyOutbound processPostV3D2(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println(
      "Processing post " +
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE)
    );
    fixContentType(request);

    // I have implemented this so many ways. Pretty sure the data being sent is not getting to the flux, or the flux is not returning all of the data
    String contentText = new String();
    Flux<HttpData> fluxHttpData = request.receiveForm();
    Flux<String> fluxString = fluxHttpData.flatMap(lambdaHttpData ->
      Flux.just(lambdaHttpData.toString())
    );
    fluxString.subscribe(System.out::println);
    response.status(200);
    Mono<String> responseContent = Mono.just(contentText);
    return response.sendString(responseContent);
  }

  public static NettyOutbound processPostV3D3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println(
      "Processing post " +
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE)
    );
    fixContentType(request);
    response.status(200);

    String contentText = new String();
    Flux<HttpData> fluxHttpData = request.receiveForm();
    // request.receiveForm(builder -> builder.streaming(false))
    fluxHttpData.subscribe(
      data -> {
        System.out.println("Received data: ");
        if (data instanceof Attribute) {
          System.out.println("Processing Attribute");
          Attribute attribute = (Attribute) data;
          String name = attribute.getName();
          String value = new String();
          try {
            value = attribute.getValue();
          } catch (Exception e) {
            value = "";
          }
          System.out.println("Attribute: " + name + " = " + value);
        } else {
          System.out.println("Instance of something else");
        }
      },
      error -> {
        // Handle errors
        System.err.println("Error: " + error);
      },
      () -> {
        // Handle completion
        System.out.println("Data stream complete");
      }
    );

    response.status(200);
    Mono<String> responseContent = Mono.just(contentText);
    return response.sendString(responseContent);
  }

  public static NettyOutbound processPostV3D4(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println(
      "Processing post " +
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE)
    );
    fixContentType(request);
    response.status(200);

    String contentText = new String();
    Flux<HttpData> fluxHttpData = request.receiveForm();
    fluxHttpData.subscribe(
      httpData -> {
        // Process the HttpData here
        System.out.println("Received data: " + httpData);
        // ...
      },
      error -> {
        // Handle errors
        System.err.println("Error: " + error);
      },
      () -> {
        // Handle completion
        System.out.println("Data stream complete");
      }
    );
    response.status(200);
    Mono<String> responseContent = Mono.just(contentText);
    return response.sendString(responseContent);
  }

  public static NettyOutbound processPostV3D5(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println(
      "Processing post " +
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE)
    );
    fixContentType(request);
    response.status(200);

    // I have implemented this so many ways. Pretty sure the data being sent is not getting to the flux
    String contentText = new String();
    Flux<HttpData> fluxHttpData = request.receiveForm();
    fluxHttpData
      .flatMap(data -> {
        System.out.println("Processing data");
        // Process the HttpData (e.g., attributes or file uploads)
        if (data instanceof Attribute) {
          System.out.println("Processing Attribute");
          Attribute attribute = (Attribute) data;
          String name = attribute.getName();
          String value = new String();
          try {
            value = attribute.getValue();
          } catch (Exception e) {
            value = "";
          }
          System.out.println("Attribute: " + name + " = " + value);
        } else if (data instanceof FileUpload) {
          System.out.println("Processing FileUpload");
          FileUpload attribute = (FileUpload) data;
          String name = attribute.getName();
          String value = new String();
          try {
            value = attribute.getString();
          } catch (Exception e) {
            value = "";
          }
          System.out.println("Attribute: = " + name + " = " + value);
        } else {
          System.out.println("Received InternalAttribute");
        }
        return Flux.empty(); // Indicate completion
      })
      .then(response.status(200).send()) // Send a success response
      .subscribe();

    response.status(200);
    Mono<String> responseContent = Mono.just(contentText);
    return response.sendString(responseContent);
  }

  public static NettyOutbound processPostV3D6(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    System.out.println(
      "Processing post " +
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE)
    );
    fixContentType(request);
    response.status(200);

    // I have implemented this so many ways. Pretty sure the data being sent is not getting to the flux
    String contentText = new String();
    Flux<HttpData> fluxHttpData = request.receiveForm();
    fluxHttpData
      .flatMap(data -> {
        System.out.println("Processing data");
        // Process the HttpData (e.g., attributes or file uploads)
        if (data instanceof Attribute) {
          System.out.println("Processing Attribute");
          Attribute attribute = (Attribute) data;
          String name = attribute.getName();
          String value = new String();
          try {
            value = attribute.getValue();
          } catch (Exception e) {
            value = "";
          }
          System.out.println("Attribute: " + name + " = " + value);
        } else if (data instanceof FileUpload) {
          System.out.println("Processing FileUpload");
          FileUpload attribute = (FileUpload) data;
          String name = attribute.getName();
          String value = new String();
          try {
            value = attribute.getString();
          } catch (Exception e) {
            value = "";
          }
          System.out.println("Attribute: = " + name + " = " + value);
        } else {
          System.out.println("Received InternalAttribute");
        }
        return Flux.empty(); // Indicate completion
      })
      .then(response.status(200).send()) // Send a success response
      .subscribe();

    response.status(200);
    Mono<String> responseContent = Mono.just(contentText);
    return response.sendString(responseContent);
  }

  public static NettyOutbound okResponseV3(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String imageText = new String(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>"
    );
    String responseText = new String(
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">fortune</a><br>" +
      example.FortuneDatabase.getFortune() +
      "</br><form action=\"/fortune\" method=\"POST\"><div><label for=\"fortune\">Add a fortune</label><input name=\"fortune\" id=\"fortune\" value=\"\" /></div><div><button>Send request</button></div></form></body></html>"
    );
    Mono<String> responseContent;
    System.out.println(request.path().toString() + " HTTP/3");
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
      "h3=\":443\"; ma=2592000; persist=1, h3-29=\":443\"; ma=2592000; persist=1, h2=\":443\" ma=1"
    );

    return response.sendString(responseContent);
  }
}
