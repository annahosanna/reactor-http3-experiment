package example;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.FortuneDatabase;
import example.ParameterPOJO;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

public class ServeCommon {

  public ServeCommon() {}

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

  public static NettyOutbound returnFavicon(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    String imageText = new String(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>"
    );
    Mono<String> responseContent;
    response.header("content-type", "image/svg+xml");
    response.header("content-length", Integer.toString(imageText.length()));
    responseContent = Mono.just(imageText);
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h3-29=\":443\"; ma=2592000; persist=1, h2=\":443\" ma=1"
    );
    return response.sendString(responseContent);
  }

  public static String getHttpDataName(HttpData httpData) {
    String name = new String();
    if (httpData instanceof Attribute) {
      Attribute attribute = (Attribute) httpData;
      name = attribute.getName() == null ? "NoName" : attribute.getName();
    } else if (httpData instanceof FileUpload) {
      FileUpload fileUpload = (FileUpload) httpData;
      name = fileUpload.getFilename();
    } else {
      name = "";
    }
    return name;
  }

  public static String getHttpDataValue(HttpData httpData) {
    String value = new String();
    if (httpData instanceof Attribute) {
      Attribute attribute = (Attribute) httpData;
      try {
        value = attribute.getValue();
      } catch (Exception e) {
        value = "";
      }
    } else if (httpData instanceof FileUpload) {
      FileUpload fileUpload = (FileUpload) httpData;
      value = fileUpload.getFilename();
    } else {
      value = "";
    }
    return value;
  }

  public static <K, V> Mono<String> convertMonoMapToString(
    Mono<Map<K, V>> monoMap
  ) {
    return monoMap.map(map ->
      map
        .entrySet()
        .stream()
        .map(entry -> {
          String value = new String();
          if (entry.getValue() instanceof Attribute) {
            Attribute attribute = (Attribute) entry.getValue();
            try {
              value = attribute.getValue();
            } catch (Exception e) {
              value = "";
            }
          } else if (entry.getValue() instanceof FileUpload) {
            FileUpload fileUpload = (FileUpload) entry.getValue();
            value = fileUpload.getFilename();
          } else {
            value = "";
          }

          return ("{" + entry.getKey() + ":" + value + "}");
        })
        .collect(Collectors.joining(", ", "{[", "]}"))
    );
  }

  // Convert Map to JSON
  // Should do this with a real JSON library in case there are any weird Key and Value
  public static <K, V> Mono<String> convertMonoMapToMonoStringGeneric(
    Mono<Map<K, V>> monoMap
  ) {
    return monoMap.map(map ->
      map
        .entrySet()
        .stream()
        .map(entry -> {
          if (
            (entry.getKey() instanceof String) &&
            (entry.getValue() instanceof String)
          ) {
            // Cheating and filtering : and , to make life easier later
            // Jackson to escape a string
            ObjectMapper mapper = new ObjectMapper();
            String key = new String();
            String value = new String();
            try {
              key = mapper.writeValueAsString((String) (entry.getKey()));
              value = mapper.writeValueAsString((String) (entry.getValue()));
            } catch (Exception e) {
              key = ((String) (entry.getKey())).replaceAll(
                  "[^a-zA-Z0-9.\\s]+",
                  ""
                );
              value = ((String) (entry.getValue())).replaceAll(
                  "[^a-zA-Z0-9.\\s]+",
                  ""
                );
            }
            return ("{\"" + key + "\":\"" + value + "\"}");
          } else {
            return (
              "{\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"}"
            );
          }
        })
        .collect(Collectors.joining(",", "{[", "]}"))
    );
  }

  // Convert receive ByteBufMono to StringMono
  public static Mono<String> getMonoString(HttpServerRequest request) {
    Mono<String> monoString = request
      .receive()
      .aggregate()
      .retain()
      .asString()
      .defaultIfEmpty("\"\":\"\"")
      .delayElement(Duration.ofMillis(100)); // Delay on next - no delay on empty
    return monoString;
  }

  public static Mono<String> getMonoStringFromFlux(HttpServerRequest request) {
    Flux<HttpData> fluxHttpData = request.receiveForm();
    Mono<Map<String, String>> monoMapStringHttpData = fluxHttpData
      .collectMap(ServeCommon::getHttpDataName, ServeCommon::getHttpDataValue)
      .delayElement(Duration.ofMillis(100));
    return ServeCommon.convertMonoMapToMonoStringGeneric(monoMapStringHttpData);
  }

  // It would be more flexable if I passed a lambda here
  public static void addMonoStringToDatabase(Mono<String> monoString) {
    monoString.doOnNext(result -> {
      System.out.println("Result: " + result);
    });
  }

  public static String responseText() {
    return (
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">Your fortune:</a><br/>" +
      example.FortuneDatabase.getFortune() +
      "<br/><br/><form action=\"/fortune\" method=\"POST\"><div><label for=\"fortune\">Add a fortune</label><input type=\"text\" name=\"fortune\" id=\"fortune\" value=\"\" /></div><div><button type=\"submit\">Send request</button></div></form></body></html>"
    );
  }

  // Only process first parameter
  public static boolean doFilter(String result) {
    ObjectMapper mapper = new ObjectMapper();
    String key = new String();
    String value = new String();
    try {
      // Convert string to pojo
      ParameterPOJO pojo = mapper.readValue(result, ParameterPOJO.class);
      // Get first item (paramter) in array
      Map<String, String> map = pojo.parameters.iterator().next();
      // Get first key
      key = map.keySet().iterator().next();
      // Get value of key
      value = map.get(key);
    } catch (Exception e) {
      if (result.length() > 0) {
        String[] ss = result.split(",");
        String[] sc = ss[0].split(":");
        key = sc[0];
        value = sc[1];
      }
      if (value.length() > 0) {
        String cleanValue = value
          .replaceAll("[^a-zA-Z0-9.,!?\\s]+", "")
          .replaceAll("^[\\s]+", "")
          .replaceAll("[\\s]+$", "");
        System.out.println("Value: " + cleanValue);
        FortuneDatabase.addFortune(cleanValue);
      } else {
        System.out.println("No value");
      }
    }
    return true;
  }

  // Example: fortune=abc%29%29%28*%26%5E%25%24%23%40abc&fortune2=def%7C%7D%7B%5B%5D%5C%3A%22%27%3B%3F%3E%3C%2C.%2Fdef
  // fortune=%21%40%23%24%25%5E%26*%28%29-_%2B%3D%7B%7D%7C%5B%5D%5C%3A%22%3B%27%3C%3E%3F%2C.%2F%7E++%60
  // Use regex to remove trailing newlines and other invalid char
  // Ok chars are: a-zA-Z0-9*-_.+&=
  // Split on &
  // Split on =
  // URL Decode each part
  public static Mono<String> getFormData(HttpServerRequest request) {
    if (
      request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE) != null &&
      request
        .requestHeaders()
        .get(HttpHeaderNames.CONTENT_TYPE)
        .toLowerCase()
        .startsWith(
          HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()
            .toLowerCase()
        )
    ) {
      request
        .requestHeaders()
        .set(
          HttpHeaderNames.CONTENT_TYPE,
          HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()
        );
    } else {
      return Mono.empty();
    }
    Mono<String> rawMonoString = getMonoString(request);
    Flux<String> fluxString = convertMonoToFlux(rawMonoString);
    Mono<Map<String, String>> monoMapStringString = fluxString
      .collectMap(ServeCommon::getFormParamName, ServeCommon::getFormParamValue)
      .delayElement(Duration.ofMillis(100));
    return (
      ServeCommon.convertMonoMapToMonoStringGeneric(monoMapStringString)
    ).filter(ServeCommon::doFilter);
  }

  public static String getFormParamName(String param) {
    String[] keyValuePair = param
      .replaceAll("[^a-zA-Z0-9*-_.+&=]+", "")
      .split("=");
    if (keyValuePair.length != 2) {
      return "Array to short";
    }
    return URLDecoder.decode(keyValuePair[0], StandardCharsets.UTF_8);
  }

  public static String getFormParamValue(String param) {
    String[] keyValuePair = param
      .replaceAll("[^a-zA-Z0-9*-_.+&=]+", "")
      .split("=");
    if (keyValuePair.length != 2) {
      return "Array to short";
    }
    return URLDecoder.decode(keyValuePair[1], StandardCharsets.UTF_8);
  }

  public static Flux<String> getParameterFlux(String parameter) {
    return Flux.fromIterable(Arrays.asList(parameter.split("&")));
  }

  public static Flux<String> convertMonoToFlux(Mono<String> rawFormData) {
    Flux<String> keyPairs = rawFormData.flatMapMany(ServeCommon::stringToFlux);
    return keyPairs;
  }

  public static Flux<String> stringToFlux(String str) {
    if (str.contains("&")) {
      return Flux.fromArray(str.split("&"));
    } else {
      return Flux.just(str);
    }
  }
}
