package example;

// import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
// import example.FortuneDatabase;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
// import java.io.File;
// import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
// import java.util.ArrayList;
// import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
// import java.util.function.Consumer;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
// import reactor.netty.DisposableServer;
import reactor.netty.NettyOutbound;
// import reactor.netty.http.Http2SslContextSpec;
// import reactor.netty.http.Http3SslContextSpec;
// import reactor.netty.http.HttpProtocol;
// import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ServeCommon {

  public ServeCommon() {}

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

  public static void setCommonHeaders(HttpServerResponse response) {
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
  }

  public static String getHttpDataName(HttpData httpData) {
    String name = new String();
    if (httpData instanceof Attribute) {
      Attribute attribute = (Attribute) httpData;
      name = ((attribute.getName() == null) || (attribute.getName().isEmpty()))
        ? "null"
        : attribute.getName();
    } else if (httpData instanceof FileUpload) {
      FileUpload fileUpload = (FileUpload) httpData;
      name = fileUpload.getFilename();
    } else {
      name = "null";
    }
    return name;
  }

  public static String getHttpDataValue(HttpData httpData) {
    String value = new String();
    if (httpData instanceof Attribute) {
      Attribute attribute = (Attribute) httpData;
      try {
        value = (attribute.getValue() == null) ? null : attribute.getValue();
      } catch (Exception e) {
        value = null;
      }
    } else if (httpData instanceof FileUpload) {
      // FileUpload fileUpload = (FileUpload) httpData;
      value = null;
    } else {
      value = null;
    }
    return value;
  }

  public static <K, V> Mono<String> convertMonoMapToMonoStringGeneric(
    Mono<Map<K, V>> monoMap
  ) {
    return monoMap.map(map ->
      map
        .entrySet()
        .stream()
        .map(entry -> {
          if (
            ((Objects.isNull(entry.getKey())) ||
              (entry.getKey() instanceof String)) &&
            ((Objects.isNull(entry.getValue())) ||
              (entry.getValue() instanceof String))
          ) {
            ObjectMapper mapper = new ObjectMapper();
            String key = new String();
            String value = new String();
            try {
              if (Objects.isNull(entry.getKey())) {
                key = "\"null\"";
              } else {
                key = mapper.writeValueAsString((String) (entry.getKey()));
              }
              if (Objects.isNull(entry.getValue())) {
                value = "null";
              } else {
                value = mapper.writeValueAsString((String) (entry.getValue()));
              }
            } catch (Exception e) {
              System.out.println("Problem converting String to JSON String");
              key = ("\"" + ((String) (entry.getKey())) + "\"").replaceAll(
                  "[^a-zA-Z0-9.\\s]+",
                  ""
                );
              value = ("\"" + ((String) (entry.getValue())) + "\"").replaceAll(
                  "[^a-zA-Z0-9.\\s]+",
                  ""
                );
            }
            return ("{\"Key\":" + key + ",\"Value\":" + value + "}");
          } else {
            return ("{\"Key\":\"null\",\"Value\":null\"}");
          }
        })
        .collect(Collectors.joining(",", "[", "]"))
    );
  }

  // Convert receive ByteBufMono to StringMono
  public static Mono<String> getMonoString(HttpServerRequest request) {
    Mono<String> monoString = request
      .receive()
      .aggregate()
      .retain()
      .asString()
      .delayElement(Duration.ofMillis(50))
      .defaultIfEmpty("\"\"=\"\"");
    //.delayElement(Duration.ofMillis(100)); // Uses "parallel" scheduler. Delay notification that stream is ready to be consumed (unless it is empty)
    return monoString;
  }

  // Wrap request to json conversion
  public static Mono<String> getMonoStringFromFlux(HttpServerRequest request) {
    // Built in function to convert a Mono<ByteBuf> into an HttpData object for each parameter (Flux)
    Flux<HttpData> fluxHttpData = request.receiveForm();
    // This applies a key function, and a value (defaults to the flux object). In this case a value function has been added as well to return a String rather than HttpData
    Mono<Map<String, String>> monoMapStringHttpData = fluxHttpData.collectMap(
      ServeCommon::getHttpDataName,
      ServeCommon::getHttpDataValue
    );
    //.delayElement(Duration.ofMillis(100));
    // Collect all of the map entries into a single string
    return ServeCommon.convertMonoMapToMonoStringGeneric(
      monoMapStringHttpData
    ).filter(ServeCommon::doFilter);
  }

  public static String responseText() {
    return (
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">Your fortune:</a><br/>" +
      example.FortuneDatabase.getFortune() +
      "<br/><br/><form action=\"/fortune\" method=\"POST\"><div><label for=\"fortune\">Add a fortune</label><input type=\"text\" name=\"fortune\" id=\"fortune\" value=\"\" /></div><div><button type=\"submit\">Send request</button></div></form></body></html>"
    );
  }

  // This can be called from then()
  public static void updateDBWithString(String value) {
    System.out.println("Value:" + value);
    if ((!Objects.isNull(value)) && (value.length() > 0)) {
      FortuneDatabase.addFortune(value);
    } else {
      System.out.println("No value");
    }
  }

  // Only process first parameter. JDBC Code in lambda blocks so need to move this to a seperate worker thread with callable - although H2 is in memory so I am not really sure if the overhead is worth it.
  public static boolean doFilter(String result) {
    System.out.println("JSON value: " + result);
    String key = new String();
    String value = new String();
    try {
      List<Map<String, String>> pojo = new ObjectMapper()
        .readValue(result, new TypeReference<List<Map<String, String>>>() {});
      Map<String, String> map = pojo.iterator().next();
      key = map.get("Key");
      value = map.get("Value");
    } catch (Exception e) {
      e.printStackTrace();
      if (result.length() > 0) {
        String[] ss = result.split(",");
        String[] sc = ss[0].split(":");
        key = sc[0].replaceAll("[^a-zA-Z0-9.,!?\\\\\\s]+", "");
        value = sc[1].replaceAll("[^a-zA-Z0-9.,!?\\\\\\s]+", "");
      }
    }

    updateDBWithString(value);

    return true;
  }

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
    Mono<Map<String, String>> monoMapStringString = fluxString.collectMap(
      ServeCommon::getFormParamName,
      ServeCommon::getFormParamValue
    );
    //.delayElement(Duration.ofMillis(100));
    return (
      ServeCommon.convertMonoMapToMonoStringGeneric(monoMapStringString)
    ).filter(ServeCommon::doFilter);
  }

  // Functions for maps
  // Ok chars in post param a-zA-Z0-9*-_.+&=%
  // Split on & (trim off ends)
  // Split on = (could be null(- will split still work)
  // 'null', 'true', 'false', number, string are valid in json
  public static String getFormParamName(String param) {
    String keyValuePair = param;
    if (param.length() - param.replace("=", "").length() != 1) {
      // bad
      return "null";
    } else {
      String keyValue = keyValuePair
        .replaceAll("[^a-zA-Z0-9*-_.+&=%]+", "")
        .replaceAll("[=][a-zA-Z0-9*-_.+&%]*$", "");
      if (keyValue.length() == 0) {
        return "null";
      } else {
        try {
          return URLDecoder.decode(keyValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
          return "null";
        }
      }
    }
  }

  public static String getFormParamValue(String param) {
    String keyValuePair = param;
    if (param.length() - param.replace("=", "").length() != 1) {
      return null;
    } else {
      String valueValue = keyValuePair
        .replaceAll("[^a-zA-Z0-9*-_.+&=%]+", "")
        .replaceAll("^[a-zA-Z0-9*-_.+&%]+[=]", "");
      if (valueValue.length() == 0) {
        return null;
      } else {
        try {
          return URLDecoder.decode(valueValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
          return null;
        }
      }
    }
  }

  public static Flux<String> convertMonoToFlux(Mono<String> rawFormData) {
    Flux<String> keyPairs = rawFormData.flatMapMany(ServeCommon::stringToFlux);
    return keyPairs;
  }

  public static Flux<String> stringToFlux(String str) {
    String cleanString = str
      .replaceAll("[^a-zA-Z0-9*-_.+&=%]+", "")
      .replaceAll("[&]+", "&")
      .replaceAll("[=][a-zA-Z0-9*-_.+%]+[=]", "=")
      .replaceAll("[=]+", "=")
      .replaceAll("^[=]+", "")
      .replaceAll("^[&]+", "")
      .replaceAll("[&]+$", "")
      .replaceAll("[&][=]", "&null=");
    // .replaceAll("[&][a-zA-Z0-9*-_.+%]+$", "&null=");
    //.replaceAll("^[a-zA-Z0-9*-_.+%]+$", "null=");
    System.out.println("Post param: " + cleanString);
    if (cleanString.contains("&")) {
      return Flux.fromArray(cleanString.split("&"));
    } else {
      return Flux.just(cleanString);
    }
  }
}
