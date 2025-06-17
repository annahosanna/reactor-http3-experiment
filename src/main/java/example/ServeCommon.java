package example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.impl.BooleanObject;
import example.impl.ContentTypeObject;
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

  /**
   * Return a blank svg as the favicon
   * @param request
   * @param response
   * @return
   */
  public static NettyOutbound returnFavicon(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    // The smallest SVG image possible
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

  /**
   * Return a robots.txt the disallows everything
   * @param request
   * @param response
   * @return
   */
  public static NettyOutbound returnRobotsTxt(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> responseContent = Mono.just("User-Agent: *\nDisallow: /\n");
    response.header("content-type", "text/plain");
    response.status(200);
    return response.sendString(responseContent);
  }

  /**
   * Return a web page
   * @param request
   * @param response
   * @return
   */
  public static NettyOutbound returnDefaultRoute(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    Mono<String> responseContent = Mono.just(htmlResponse());
    response.header("content-type", "text/html");
    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\" ma=1"
    );
    response.status(200);
    return response.sendString(responseContent);
  }

  /**
   * Parse them HttpData type for keys
   * @param httpData
   * @return
   */
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

  /**
   * Parse HttpData for values
   * @param httpData
   * @return
   */
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

  /**
   * Convert Map<key,value> to JSON {"key":"value"}
   * @param <K>
   * @param <V>
   * @param monoMap
   * @return
   */
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

  /**
   * Convert request ByteBuffer to string
   * @param request
   * @param response
   * @return
   */
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
          System.out.println("Received data");
          return Mono.just(str);
        } else {
          System.out.println("No data received");
          return Mono.empty();
        }
      })
      .subscribeOn(Schedulers.boundedElastic());

    return receivedData;
  }

  /**
   * Use the built in ReceiveForm method to obtain received data
   * This is slow but has the advantage that it can parse both x-www-form-urlencoded
   * and multipart/form-data
   * @param request
   * @return
   */
  public static Mono<String> getMonoStringFromFlux(HttpServerRequest request) {
    Flux<HttpData> fluxHttpData = request.receiveForm();
    Mono<Map<String, String>> monoMapStringHttpData = fluxHttpData.collectMap(
      ServeCommon::getHttpDataName,
      ServeCommon::getHttpDataValue
    );
    // Not right - this needs to be a flux not mono
    Mono<String> monoMapString = convertMonoMapToMonoStringGeneric(
      monoMapStringHttpData
    ).flatMap(ServeCommon::doFilter);
    return monoMapString;
  }

  /**
   * Html to Pass back if the user does no access the correct page
   * @return
   */
  public static String responseText() {
    return (
      "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body><a href=\"/fortune\">Your fortune:</a><br/>" +
      example.FortuneDatabase.getFortune() +
      "<br/><br/><form action=\"/fortune\" method=\"POST\"><div><label for=\"fortune\">Add a fortune</label><input type=\"text\" name=\"fortune\" id=\"fortune\" value=\"\" /></div><div><button type=\"submit\">Send request</button></div></form></body></html>"
    );
  }

  /**
   * Display a k/v pair Map.Entry and return it as a Flux
   * @param element
   * @return
   */
  public static Flux<Map.Entry<String, String>> displayHeaders(
    Map.Entry<String, String> element
  ) {
    String key = element.getKey();
    String value = element.getValue();
    System.out.println("Key: " + key + ", Value: " + value);
    return (Flux.just(element));
  }

  public static String contentTypeTest(
    ContentTypeObject contentTypeObject,
    HttpServerRequest request
  ) {
    String contentType = (request
          .requestHeaders()
          .get(HttpHeaderNames.CONTENT_TYPE) ==
        null)
      ? (((request.requestHeaders().get(HttpHeaderNames.ACCEPT) == null)
            ? null
            : (request.requestHeaders().get(HttpHeaderNames.ACCEPT))))
      : (request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE));
    return contentType;
  }

  public static void getContentType(
    ContentTypeObject contentTypeObject,
    HttpServerRequest request
  ) {
    String contentType = contentTypeTest(
      contentTypeObject,
      request
    ).toLowerCase();
    if (
      contentType.startsWith(
        HttpHeaderValues.TEXT_HTML.toString().toLowerCase()
      )
    ) {
      contentTypeObject.setIsHtml();
    } else if (
      contentType.startsWith(
        HttpHeaderValues.APPLICATION_JSON.toString().toLowerCase()
      )
    ) {
      contentTypeObject.setIsJson();
    } else if (
      contentType.startsWith(
        HttpHeaderValues.TEXT_PLAIN.toString().toLowerCase()
      )
    ) {
      contentTypeObject.setIsText();
    }
  }

  /**
   * Return a fortune in a format based on content type
   * @param request
   * @param response
   * @return
   */
  public static Mono<String> responseTextR2DBC(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    // Debugging
    Flux<Map.Entry<String, String>> headerFlux = Flux.fromIterable(
      request.requestHeaders().entries()
    ).flatMap(ServeCommon::displayHeaders);
    headerFlux.subscribe();

    response.header(
      "alt-svc",
      "h3=\":443\"; ma=2592000; persist=1, h2=\":443\"; ma=1"
    );
    response.header("cache-control", "no-cache");
    // Resolve content type before async stuff
    ContentTypeObject contentTypeObject = new ContentTypeObject();
    getContentType(contentTypeObject, request);
    // Start async stuff
    Mono<String> getFortuneMono = FortuneDatabaseR2DBC.getFortune()
      .subscribeOn(Schedulers.boundedElastic());

    // This could be a mono if I knew how to modify response and return a string
    if (contentTypeObject.getIsHtml()) {
      response.header("content-type", "text/html");
      response.status(200);
      Mono<String> createResponseText = getFortuneMono.flatMap(fortune -> {
        return (Mono.just(htmlResponse()));
      });
      return createResponseText;
    } else if (contentTypeObject.getIsText()) {
      response.header("content-type", "text/plain");
      response.status(200);

      // return getFortuneMono;
      return Mono.just(
        "Obtaining untrusted content is currently not supported"
      );
    } else if (contentTypeObject.getIsJson()) {
      response.header("content-type", "application/json");
      response.status(200);
      // Mono<String> createResponseText = getFortuneMono.flatMap(fortune -> {
      // return (Mono.just("[{\"fortune\":\"" + fortune + "\"}]"));
      // });
      //
      // return createResponseText;

      return Mono.just("[{\"fortune\":\"Untrusted content\"}]");
    } else {
      response.header("content-type", "text/html");
      response.status(415);
      return (
        Mono.just(
          "<!DOCTYPE html><html><head><link rel=\"icon\" href=\"data:,\"/></head><body>There was a problem processing your request: Unsupported media type</body></html>"
        )
      );
    }
  }

  /**
   * Update the JDBC database with a String
   * @param value
   */
  public static void updateDBWithString(String value) {
    if ((!Objects.isNull(value)) && (value.length() > 0)) {
      FortuneDatabase.addFortune(value);
    } else {
      // System.out.println("No value");
    }
  }

  /**
   * Update the R2DBC database with a String
   * @param value
   */
  public static void updateDBWithStringR2DBC(String value) {
    // System.out.println("updateDBWithStringR2DBC");
    if ((!Objects.isNull(value)) && (value.length() > 0)) {
      System.out.println("updateDBWithStringR2DBC Adding Fortune: " + value);
      FortuneDatabaseR2DBC.addFortune(value);
    } else {
      // System.out.println("No value");
    }
  }

  /**
   * Update the R2DBC database with a Map
   * @param value
   * @return
   */
  public static Flux<Map<String, String>> updateDBWithStringR2DBC(
    Map<String, String> value
  ) {
    if (!Objects.isNull(value)) {
      FortuneDatabaseR2DBC.addFortune(value);
    } else {
      // System.out.println("No value");
    }
    return Flux.empty();
  }

  /**
   * Convert a JSON string in the form if List<Map<String,String>>
   * to a key/value pair
   * @param result
   * @return
   */
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

  /**
   * Update the R2DBC database with the last k/v pair
   * @param result
   * @return
   */
  public static Mono<String> doFilter(String result) {
    Flux<String> valueOnly = doConvertJSONToValues(result);
    return updateDBWithFluxString(valueOnly);
  }

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
    Flux<String> fluxString2 = doConvertJSONToValues(convertMonoMapString);
    return updateDBWithFluxString(fluxString2);
  }

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

  /**
   * Sanitize a string that could be x-www-form-urlencoded
   * Remove invalid (not urlencoded) characters
   * Remove extra characters at the beginning and end
   * Remove ambigious key/value
   * Remove duplicate seperators
   * @param str the raw potential x-www-form-urlencoded string
   * @return    the list of (still urlencoded) key/value pairs
   */
  public static Flux<String> stringToFlux(String str) {
    String cleanString = str
      .replaceAll("[^a-zA-Z0-9*-_.+&=%\"]+", "") // Remove invalid characters
      .replaceAll("^[=&]+", "") // =x -> x          Remove leading = and &
      .replaceAll("^[^=&]+[&]+", "") // ^x& ->      Remove leading characters
      .replaceAll("^[^=&]+$", "") // ^x$ ->         Remove leading characters
      .replaceAll("[&][^&=]+$", "&") // &x$ -> &    Remove trailing characters
      .replaceAll("[&]+[=]+", "&") // &&== -> &     Missing key
      .replaceAll("[^&=]*[=]+[^&=]*[=]+", "&") //   Replace k==x== or ==x== or == with & Ambiguous
      .replaceAll("[&][^=]+[&]", "&") // &x& -> &   Ambiguous
      .replaceAll("[&]+", "&") // && -> &           Clean up extra &
      .replaceAll("[=]+", "=") // == -> =           Clean up extra =
      .replaceAll("^[&=]+", "") //                  Remove leading & or =
      .replaceAll("[&]+$", ""); //                  Remove trailing &
    List<String> list = new ArrayList<String>();
    // a=&b=
    if ((cleanString.contains("&")) && (cleanString.length() > 4)) {
      String[] stringArray = cleanString.split("&");
      for (int i = 0; i < stringArray.length; i++) {
        adjustEqualSign(list, stringArray[i]);
      }
      // b=
    } else if ((!cleanString.contains("&")) && (cleanString.length() > 1)) {
      adjustEqualSign(list, cleanString);
    } else {
      // Invalid
    }

    if (list.toArray().length == 0) {
      return Flux.empty();
    } else {
      return Flux.fromIterable(list);
    }
  }

  /**
   * Validates that a (sanitized) string could be a key/value pair
   * @param list the list of key/value pairs
   * @param str  the potential string to add to the list
   */
  public static void adjustEqualSign(List<String> list, String str) {
    if ((str.contains("=")) && (!str.startsWith("=")) && (str.length() > 0)) {
      list.add(str);
    }
  }

  /**
   * Wraps doConvertJSONToValues(String)
   * @param value  the Mono<String> with raw text
   * @return       the extracted values
   */
  public static Flux<String> doConvertJSONToValues(Mono<String> value) {
    System.out.println("doConvertJSONToValues - Mono -> Flux");

    Flux<String> flux = Flux.from(value);
    return flux.flatMap(ServeCommon::doConvertJSONToValues);
  }

  /**
   * Tests that the raw text string conforms to List<Map<String,String>>
   * Extracts the values from each Map Entry
   * Creates a Flux<String> containing the extracted values
   * @param result  a raw text string
   * @return        the extracted values
   */
  public static Flux<String> doConvertJSONToValues(String result) {
    System.out.println("doConvertJSONToValues - String -> Flux");

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

  /**
   * This method consumes the output of the response,
   * and validates that that the String conforms to the JSON form List<Map<String,String>>
   * and then updates the database
   * @param request  the HTTP request object
   * @param response the HTTP response object
   * @return         a Mono<String> object which is blank
   */
  public static Mono<String> processPutData(
    HttpServerRequest request,
    HttpServerResponse response
  ) {
    // Get raw data
    Mono<String> rawMonoString = getMonoString(request, response);
    // Confirm that it is List<Map<String,String>>
    Flux<String> fluxString = doConvertJSONToValues(rawMonoString);
    // Update the database
    return updateDBWithFluxString(fluxString);
  }

  /**
   * Update the database with a Flux of k/v pairs
   * @param fluxString
   * @return
   */
  public static Mono<String> updateDBWithFluxString(Flux<String> fluxString) {
    Flux<String> dbFlux = fluxString.flatMap(s -> {
      updateDBWithStringR2DBC(s);
      return Flux.just("");
    });
    Mono<String> waiter = dbFlux.last("");
    return waiter;
  }

  /**
   * Insert data from the database into a static web Page - Generated from ChatGTP with:
   * Generate source code for a force directed graph, using D3, based on key stroke repetition
   * for numbers, letters and space characters. Nodes should change color based on frequency.
   * Node starting color should be medium teal, with a white background. The graph should
   * dynamicaly resize to fit the window
   * and all nodes should fit within the window. Users should be able to interact with the graph.
   * The color of the letters on the nodes should be black. The nodes should be connected with
   * gray lines. The graph should enable the visualizion the transition between keys, and scale
   * nodes with key frequency. There should be a reset option.
   * @param fortune  the data to be inserted
   * @return         the web page from the resulting concatination
   */
  public static String htmlResponseD3() {
    String finalHtml =
      """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <title>Keystroke Frequency Graph</title>
          <style>
            html, body {
              margin: 0;
              height: 100%;
              font-family: sans-serif;
              display: flex;
              flex-direction: column;
              background: white;
            }

            #controls {
              display: flex;
              padding: 10px;
              background: #f2f2f2;
              border-bottom: 1px solid #ccc;
            }

            input {
              flex: 1;
              font-size: 1rem;
              padding: 8px;
            }

            button {
              margin-left: 10px;
              padding: 8px 12px;
              font-size: 1rem;
            }

            svg {
              flex: 1;
              width: 100%;
              background: white;
              cursor: move;
            }

            text.label {
              font-size: 12px;
              fill: black;
              pointer-events: none;
            }
          </style>
        </head>
        <body>

          <div id="controls">
            <input id="inputField" placeholder="Type letters, numbers, or space..." autofocus />
            <button id="resetButton">Reset</button>
          </div>

          <svg></svg>

          <script src="https://d3js.org/d3.v7.min.js"></script>
          <script>
            const svg = d3.select("svg");
            const container = svg.append("g");
            const linkGroup = container.append("g");
            const nodeGroup = container.append("g");
            const labelGroup = container.append("g");

            let nodes = [], links = [];
            const nodeMap = new Map(), linkMap = new Map();
            let lastKey = null;

            const input = document.getElementById("inputField");
            const resetButton = document.getElementById("resetButton");

            const width = () => window.innerWidth;
            const height = () => window.innerHeight - document.getElementById("controls").offsetHeight;

            const sizeScale = d3.scaleSqrt().domain([1, 50]).range([10, 40]);
            const colorScale = d3.scaleLinear()
              .domain([1, 50])
              .range(["#b2dfdb", "#00796b"]); // light teal to dark teal

            const simulation = d3.forceSimulation()
              .force("link", d3.forceLink().id(d => d.id).distance(100))
              .force("charge", d3.forceManyBody().strength(-300))
              .force("center", d3.forceCenter(width() / 2, height() / 2))
              .force("collide", d3.forceCollide().radius(d => sizeScale(d.count) + 5))
              .on("tick", ticked);

            svg.call(d3.zoom().on("zoom", e => container.attr("transform", e.transform)));
            svg.attr("width", width()).attr("height", height());

            input.addEventListener("keydown", e => {
              let key = e.key.toLowerCase();
              if (key === ' ') key = 'space';
              if (!/^[a-z0-9 ]$/.test(key)) return;

              if (!nodeMap.has(key)) {
                const node = { id: key, count: 1 };
                nodes.push(node);
                nodeMap.set(key, node);
              } else {
                nodeMap.get(key).count++;
              }

              if (lastKey && lastKey !== key) {
                const linkKey = `${lastKey}->${key}`;
                if (!linkMap.has(linkKey)) {
                  const link = { source: lastKey, target: key, count: 1 };
                  links.push(link);
                  linkMap.set(linkKey, link);
                } else {
                  linkMap.get(linkKey).count++;
                }
              }

              lastKey = key;
              updateGraph();
            });

            resetButton.addEventListener("click", () => {
              nodes = [];
              links = [];
              nodeMap.clear();
              linkMap.clear();
              lastKey = null;
              nodeGroup.selectAll("*").remove();
              linkGroup.selectAll("*").remove();
              labelGroup.selectAll("*").remove();
              simulation.nodes([]);
              simulation.force("link").links([]);
              simulation.alpha(0.1).restart();
            });

            function updateGraph() {
              simulation.nodes(nodes);
              simulation.force("link").links(links);
              simulation.alpha(0.9).restart();

              const linkSel = linkGroup.selectAll("line")
                .data(links, d => d.source.id + "-" + d.target.id);

              linkSel.exit().remove();

              linkSel.enter()
                .append("line")
                .attr("stroke", "#ccc")
                .attr("stroke-width", 1.5)
                .merge(linkSel);

              const nodeSel = nodeGroup.selectAll("circle")
                .data(nodes, d => d.id);

              nodeSel.exit().remove();

              nodeSel.enter()
                .append("circle")
                .call(drag(simulation))
                .merge(nodeSel)
                .attr("r", d => sizeScale(d.count))
                .attr("fill", d => colorScale(d.count));

              const labelSel = labelGroup.selectAll("text")
                .data(nodes, d => d.id);

              labelSel.exit().remove();

              labelSel.enter()
                .append("text")
                .attr("class", "label")
                .merge(labelSel)
                .text(d => d.id)
                .attr("text-anchor", "middle");
            }

            function ticked() {
              nodeGroup.selectAll("circle")
                .attr("cx", d => d.x = Math.max(30, Math.min(width() - 30, d.x)))
                .attr("cy", d => d.y = Math.max(30, Math.min(height() - 30, d.y)));

              linkGroup.selectAll("line")
                .attr("x1", d => d.source.x)
                .attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x)
                .attr("y2", d => d.target.y);

              labelGroup.selectAll("text")
                .attr("x", d => d.x)
                .attr("y", d => d.y + 4);
            }

            function drag(sim) {
              return d3.drag()
                .on("start", (e, d) => {
                  if (!e.active) sim.alphaTarget(0.3).restart();
                  d.fx = d.x;
                  d.fy = d.y;
                })
                .on("drag", (e, d) => {
                  d.fx = e.x;
                  d.fy = e.y;
                })
                .on("end", (e, d) => {
                  if (!e.active) sim.alphaTarget(0);
                  d.fx = null;
                  d.fy = null;
                });
            }

            window.addEventListener("resize", () => {
              svg.attr("width", width()).attr("height", height());
              simulation.force("center", d3.forceCenter(width() / 2, height() / 2));
            });
          </script>
        </body>
        </html>

      """;
    return finalHtml;
  }

  /*
  public static String htmlResponse() {
    String finalHtml =
      """
      """;
    return finalHtml;
  }
  */
  public static String htmlResponse() {
    String finalHtml =
      """
      """;
    return finalHtml;
  }

  public static Mono<String> checkAuthenticationHeader(
    HttpServerRequest request,
    BooleanObject success
  ) {
    String expectedToken = "Bearer secret-token";
    if (
      (request.requestHeaders().get(HttpHeaderNames.AUTHORIZATION) != null) &&
      (expectedToken.equals(
          request.requestHeaders().get(HttpHeaderNames.AUTHORIZATION)
        ))
    ) {
      success.setValue(true);
      return Mono.just("");
    }
    success.setValue(false);
    return Mono.empty();
  }

  public static String accessDeniedResponse() {
    String response =
      """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>Access Denied</title>
          <style>
            html, body {
              margin: 0;
              padding: 0;
              height: 100%;
              width: 100%;
              background-color: white;
            }
            svg {
              width: 100%;
              height: 100%;
              display: block;
            }
            line {
              stroke: red;
              stroke-width: 10;
              stroke-linecap: round;
            }
            text {
              fill: red;
              font-family: sans-serif;
              text-anchor: middle;
              dominant-baseline: middle;
            }
          </style>
        </head>
        <body>
          <svg viewBox="0 0 100 100" preserveAspectRatio="xMidYMid meet">
            <!-- Red X -->
            <line x1="10" y1="10" x2="90" y2="90"/>
            <line x1="90" y1="10" x2="10" y2="90"/>

            <!-- Access Denied text -->
            <text x="50" y="50" font-size="10">Access Denied</text>
          </svg>
        </body>
        </html>
      """;
    return response;
  }
}
