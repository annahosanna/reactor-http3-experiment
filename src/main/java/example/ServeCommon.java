package example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.NettyOutbound;
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
    Mono<String> responseContent = Mono.just(imageText);
    response.header("content-type", "image/svg+xml");
    response.header("content-length", Integer.toString(imageText.length()));
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\" ma=1"
    );
    return response.sendString(responseContent);
  }

  public static NettyOutbound returnRobotsTxt(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> responseContent = Mono.just("User-Agent: *\nDisallow: *\n");
    response.header("content-type", "text/plain");
    response.status(200);
    return response.sendString(responseContent);
  }

  public static NettyOutbound returnDefaultRoute(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> responseContent = Mono.just(htmlResponse(""));
    response.header("content-type", "text/html");
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\" ma=1"
    );
    response.status(200);
    return response.sendString(responseContent);
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

  // This could be turned into a Jackson serialized object using a POJO like: public class Pojo { String Key; String Value;}
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
                value = mapper.writeValueAsString(null);
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
            // return ("{\"Key\":" + key + ",\"Value\":" + value + "}");
            return ("{" + key + ":" + value + "}");
          } else {
            return ("{\"Key\":null}");
          }
        })
        .collect(Collectors.joining(",", "[", "]"))
    );
  }

  // Convert receive ByteBufMono to StringMono
  public static Mono<String> getMonoString(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> receivedData = request
      .receive()
      .aggregate()
      .asString()
      // .filter(str -> str.length() > 0)
      .flatMap(str -> {
        if (str.length() > 0) {
          System.out.println("Received data: " + str);
          return Mono.just(str);
        } else {
          System.out.println("No data received");
          return Mono.empty();
        }
      })
      .subscribeOn(Schedulers.boundedElastic());

    return receivedData;
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
    Mono<String> monoMapString = convertMonoMapToMonoStringGeneric(
      monoMapStringHttpData
    ).flatMap(ServeCommon::doFilter);
    return monoMapString;
  }

  // Random note to self .zipWith(Flux.interval())) can be used to create a delay
  public static String responseText() {
    return (
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">Your fortune:</a><br/>" +
      example.FortuneDatabase.getFortune() +
      "<br/><br/><form action=\"/fortune\" method=\"POST\"><div><label for=\"fortune\">Add a fortune</label><input type=\"text\" name=\"fortune\" id=\"fortune\" value=\"\" /></div><div><button type=\"submit\">Send request</button></div></form></body></html>"
    );
  }

  // public static Flux<Map.Entry<String, String>> displayHeaders(<Map.Entry<String, String>>element) {
  public static Flux<Map.Entry<String, String>> displayHeaders(
    Map.Entry<String, String> element
  ) {
    String key = element.getKey();
    String value = element.getValue();
    System.out.println("Key: " + key + ", Value: " + value);
    return (Flux.just(element));
  }

  public static Mono<String> responseTextR2DBC(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Flux<Map.Entry<String, String>> headerFlux = Flux.fromIterable(
      request.requestHeaders().entries()
    ).flatMap(ServeCommon::displayHeaders);
    headerFlux.subscribe();

    Mono<String> getFortuneMono = FortuneDatabaseR2DBC.getFortune()
      .subscribeOn(Schedulers.boundedElastic());
    // This could be a mono if I knew how to modify response and return a string
    if (
      ((request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE) != null) &&
        (request
            .requestHeaders()
            .get(HttpHeaderNames.CONTENT_TYPE)
            .toLowerCase()
            .startsWith(
              HttpHeaderValues.TEXT_HTML.toString().toLowerCase()
            ))) ||
      (((request.requestHeaders().get(HttpHeaderNames.ACCEPT) != null) &&
          (request
              .requestHeaders()
              .get(HttpHeaderNames.ACCEPT)
              .toLowerCase()
              .startsWith(
                HttpHeaderValues.TEXT_HTML.toString().toLowerCase()
              ))))
    ) {
      response.header("cache-control", "no-cache");
      response.header("content-type", "text/html");
      response.status(200);

      Mono<String> createResponseText = getFortuneMono.flatMap(fortune -> {
        return (Mono.just(htmlResponse(fortune)));
      });
      return createResponseText;
    } else if (
      ((request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE) != null) &&
        (request
            .requestHeaders()
            .get(HttpHeaderNames.CONTENT_TYPE)
            .toLowerCase()
            .startsWith(
              HttpHeaderValues.APPLICATION_JSON.toString().toLowerCase()
            ))) ||
      (((request.requestHeaders().get(HttpHeaderNames.ACCEPT) != null) &&
          (request
              .requestHeaders()
              .get(HttpHeaderNames.ACCEPT)
              .toLowerCase()
              .startsWith(
                HttpHeaderValues.APPLICATION_JSON.toString().toLowerCase()
              ))))
    ) {
      response.header("cache-control", "no-cache");
      response.header("content-type", "application/json");
      response.status(200);
      Mono<String> createResponseText = getFortuneMono.flatMap(fortune -> {
        return (Mono.just("[{\"fortune\":\"" + fortune + "\"}]"));
      });
      return createResponseText;
    } else if (
      ((request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE) != null) &&
        (request
            .requestHeaders()
            .get(HttpHeaderNames.CONTENT_TYPE)
            .toLowerCase()
            .startsWith(
              HttpHeaderValues.TEXT_PLAIN.toString().toLowerCase()
            ))) ||
      (((request.requestHeaders().get(HttpHeaderNames.ACCEPT) != null) &&
          (request
              .requestHeaders()
              .get(HttpHeaderNames.ACCEPT)
              .toLowerCase()
              .startsWith(
                HttpHeaderValues.TEXT_PLAIN.toString().toLowerCase()
              ))))
    ) {
      response.header("cache-control", "no-cache");
      response.header("content-type", "application/json");
      response.status(200);
      Mono<String> createResponseText = getFortuneMono.flatMap(fortune -> {
        return (Mono.just(fortune));
      });
      return createResponseText;
    }
    // Default
    response.status(415);
    return (
      Mono.just(
        "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body>There was a problem processing your request: Unsupported media type</body></html>"
      )
    );
  }

  // This can be called from then()
  public static void updateDBWithString(String value) {
    if ((!Objects.isNull(value)) && (value.length() > 0)) {
      FortuneDatabase.addFortune(value);
    } else {
      // System.out.println("No value");
    }
  }

  // This can be called from then()
  public static void updateDBWithStringR2DBC(String value) {
    // System.out.println("updateDBWithStringR2DBC");
    if ((!Objects.isNull(value)) && (value.length() > 0)) {
      System.out.println("updateDBWithStringR2DBC Adding Fortune: " + value);
      FortuneDatabaseR2DBC.addFortune(value);
    } else {
      // System.out.println("No value");
    }
  }

  public static Flux<Map<String, String>> updateDBWithStringR2DBC(
    Map<String, String> value
  ) {
    if (!Objects.isNull(value)) {
      FortuneDatabaseR2DBC.addFortune(value);
    } else {
      // System.out.println("No value");
    }
    // This does nothing
    // ArrayList<Map<String, String>> kv = new ArrayList<Map<String, String>>(1);
    // kv.add(new HashMap<String, String>());
    // return Flux.fromIterable(kv);
    return Flux.empty();
  }

  public static Mono<String> doConvertJSONToValue(String result) {
    String key = new String();
    String value = new String();
    if (result.length() > 8) {
      try {
        List<Map<String, String>> pojo = new ObjectMapper()
          .readValue(result, new TypeReference<List<Map<String, String>>>() {});
        System.out.println("JSON value: " + result);
        // Just read one object
        Map<String, String> map = pojo.iterator().next();
        key = map.get("Key");
        value = map.get("Value");
        return (Mono.just(result));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return (Mono.empty());
  }

  // Avoid leaks by using: releaseBody(), toBodilessEntity(), bodyToMono(void.class)
  // Only process first parameter. JDBC Code in lambda blocks although H2 is in memory so I am not really sure R2DBC is worth it.
  public static Mono<String> doFilter(String result) {
    Mono<String> valueOnly = doConvertJSONToValue(result);
    return valueOnly.flatMap(value -> {
      updateDBWithStringR2DBC(value);
      return Mono.just(value);
    });
  }

  // public static boolean doFilter2(String result) {
  //   System.out.println("JSON value: " + result);
  //   String key = new String();
  //   String value = new String();
  //   try {
  //     List<Map<String, String>> pojo = new ObjectMapper()
  //       .readValue(result, new TypeReference<List<Map<String, String>>>() {});
  //     Map<String, String> map = pojo.iterator().next();
  //     key = map.get("Key");
  //     value = map.get("Value");
  //   } catch (Exception e) {
  //     e.printStackTrace();
  //     if (result.length() > 0) {
  //       String[] ss = result.split(",");
  //       String[] sc = ss[0].split(":");
  //       key = sc[0].replaceAll("[^a-zA-Z0-9.,!?\\\\\\s]+", "");
  //       value = sc[1].replaceAll("[^a-zA-Z0-9.,!?\\\\\\s]+", "");
  //     }
  //   }

  //   updateDBWithString(value);

  //   return true;
  // }

  public static Mono<String> getFormData(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
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
      response.status(415);
      return (
        Mono.just(
          "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body>There was a problem processing your request: Unsupported media type</body></html>"
        )
      );
    }
    Mono<String> rawMonoString = getMonoString(request, response);
    // validate/scrub form data string and return k=v flux
    Flux<String> fluxString = convertMonoToFlux(rawMonoString);
    Mono<Map<String, String>> monoMapStringString = fluxString.collectMap(
      ServeCommon::getFormParamName,
      ServeCommon::getFormParamValue
    );
    Mono<String> convertMonoMapString = convertMonoMapToMonoStringGeneric(
      monoMapStringString
    );
    // A Mono<String> of json data
    /*
    Mono<String> returnMonoString = convertMonoMapString.flatMap(
      ServeCommon::doFilter
    );
    return returnMonoString;
    // .filter(ServeCommon::doFilter2);
    */
    Flux<String> fluxString2 = doConvertJSONToValues(convertMonoMapString);
    //);
    // fluxString.subscribe();

    // Does not matter if there is a waiter. It never invokes flatMap here or in doConvertJSONToValues
    // next() had no effect either
    Flux<String> dbFlux = fluxString2.flatMap(s -> {
      updateDBWithStringR2DBC(s);
      return Flux.just("");
    });
    Mono<String> waiter = dbFlux.last("");
    return waiter;
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
      String keyValue = keyValuePair.replaceAll("[=][^=]*$", "");
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

  // " is just a char, not a string terminator
  public static String getFormParamValue(String param) {
    String keyValuePair = param;
    if (param.length() - param.replace("=", "").length() != 1) {
      return null;
    } else {
      String valueValue = keyValuePair.replaceAll("^[^=]*[=]", "");
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

  // The bad thing about a POST is that there is that all values are considered strings. Whereas with JSON there are datatypes
  public static Flux<String> stringToFlux(String str) {
    String cleanString = str
      .replaceAll("[^a-zA-Z0-9*-_.+&=%\"]+", "")
      .replaceAll("[&][^=]+[&]", "&")
      .replaceAll("[&]+[=]+", "&")
      .replaceAll("[^&=]*[=]+[^&=]*[=]+", "&")
      .replaceAll("[&]+", "&")
      .replaceAll("[=]+", "=")
      .replaceAll("^[&]", "")
      .replaceAll("[&]$", "");
    List<String> list = new ArrayList<String>();
    if (cleanString.contains("&")) {
      String[] stringArray = cleanString.split("&");
      for (int i = 0; i < stringArray.length; i++) {
        adjustEqualSign(list, stringArray[i]);
      }
    } else {
      adjustEqualSign(list, cleanString);
    }

    if (list.toArray().length == 0) {
      return Flux.empty();
    } else {
      return Flux.fromIterable(list);
    }
  }

  public static void adjustEqualSign(List<String> list, String str) {
    if ((str.contains("=")) && (!str.startsWith("=")) && (str.length() > 0)) {
      list.add(str);
    }
  }

  public static Flux<String> doConvertJSONToValues(Mono<String> value) {
    System.out.println("doConvertJSONToValues - Mono -> Flux");
    // Fix this to flatMap
    // Decorate mono as flux

    Flux<String> flux = Flux.from(value);
    return flux.flatMap(ServeCommon::doConvertJSONToValues);
    // return Flux.empty();
  }

  // Really what I want to do is convert the returnValue's map to a flux
  // Invoke via flatMapMany
  public static Flux<String> doConvertJSONToValues(String result) {
    System.out.println("doConvertJSONToValues - String -> Flux");

    // Just so we know what we are parsing
    System.out.println(result);
    // Do not waste time parsing the impossible
    // [{"":""}]
    if (result.length() > 8) {
      try {
        // First convert the json string to an object
        // Then convert the List of Maps to only map values
        // System.out.println("parse result");
        List<Map<String, String>> returnValue = new ObjectMapper()
          .readValue(result, new TypeReference<List<Map<String, String>>>() {});

        List<String> values = new ArrayList<String>();

        for (Map<String, String> element : returnValue) {
          for (Map.Entry<String, String> entry : element.entrySet()) {
            if (entry.getValue() == null) {
              continue;
            }
            values.add(entry.getValue());
          }
        }
        if (values.toArray().length == 0) {
          return Flux.empty();
        }
        return Flux.fromIterable(values);
      } catch (Exception e) {
        e.printStackTrace();
        // response.status(422);
        return Flux.empty();
      }
    } else {
      System.out.println(
        "String to short to be valid JSON for List<Map<String, String>>"
      );
      return Flux.empty();
    }
  }

  // Only accept json
  public static Mono<String> processPutData(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    // System.out.println("processPutData");
    Mono<String> rawMonoString = getMonoString(request, response);
    // Check if rawMonoString is valid JSON with flatMap using jackson or fastjson.
    // If its valid then return the string, otherwise return {}.
    // Flux<Map<String, String>> fluxString = rawMonoString.flatMapMany(
    //ServeCommon::doConvertJSONToValues
    // );
    //Flux<String> fluxString = rawMonoString.thenMany(
    //ServeCommon::doConvertJSONToValues
    //);
    // fluxString.subscribe();
    Flux<String> fluxString = doConvertJSONToValues(rawMonoString);
    //);
    // fluxString.subscribe();

    // Does not matter if there is a waiter. It never invokes flatMap here or in doConvertJSONToValues
    // next() had no effect either
    Flux<String> dbFlux = fluxString.flatMap(s -> {
      updateDBWithStringR2DBC(s);
      return Flux.just("");
    });
    Mono<String> waiter = dbFlux.last("");
    return waiter;
  }

  public static String htmlResponse(String fortune) {
    String htmlHeader =
      """
      <!DOCTYPE html>
        <html lang="en">
          <head>
            <meta charset="utf-8" />
            <link rel=\"icon\" href=\"data:,\"/>
          </head>
        <body>
          <p>
            <a href=\"/fortune\">Your fortune:</a>
      """;
    String htmlFooter =
      """
          </p>
          <p>
            <form action=\"/fortune\" method=\"POST\">
              <label for=\"fortune\">Add a fortune</label>
              <input type=\"text\" name=\"fortune\" id=\"fortune\" value=\"\" />
            </p>
            <p>
              <button type=\"submit\">Send request</button>
            </p>
          </form>
        </body>
      </html>
      """;
    String finalHtml = htmlHeader + fortune + htmlFooter;
    return finalHtml;
  }
}
