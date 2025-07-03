package example.impl;

import example.FortuneDatabaseR2DBC;
import example.ServeCommon;
import example.impl.BooleanObject;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;

// Little issue here - what if someone connects on port 443/tcp and then on 443/udp - do they share the same session cookie

// This object will not work if multiple clients were combined:
// List<Map<String,Map<String,String>>>
// Or just one:
// Could be Map<String, Map<String, String>>
// Or the values could be seperate
// In fact the constructor should just take request, and resolve the rest.

// Methods should pass this - then I can use ServeCommon.java.
// Each setter returns 'this' -> then it can just be passed along the pipeline
public class ContentData {

  // This only matters for POST requests and is not a requirement.
  // But the raw POST string should be processed here and turned into a JSON string.
  private String method = null;
  private String validatedMethod = null;
  // Based on the method this should either be processed like a POST urlencoded string,
  // or a JSON string - either way the format of the data needs to be validated.
  private Mono<String> rawInputString = null;
  // The raw JSON string in case it needs to be validated or parsed
  private String rawJSON = null;
  // The Key/Value pairs (where each map has two values {"Key":"whatever","Value":"whateverelse"})
  // Regular map in case there are duplicate keys
  private List<Map<String, String>> jsonArrayMap = null;
  // The SESSIONID cookie value, so that it only needs to be resolved once
  private String sessionid = null;
  // A Hash Map of cookies (loses data to duplicates) (but I do not care about other cookies)
  // private Map<String, String> cookies = null;
  // A Hash Map of headers (loses data to duplicates) - which headers do I care about
  private HttpHeaders headers = null;
  // Some default values
  private int responseStatusCode = 200;
  private String responseMessage = null;
  private String responseContentType = null;
  // Cookie are by path not port
  private Cookie responseCookie = null;
  private String responseAuthorizationHeader = null;
  private Map<CharSequence, List<Cookie>> cookieMapList = null;
  // Make sure the value is assigned once, and cannot be changed
  // An attempt to change it just returns the current value
  private BooleanObject hasSetMethod = new BooleanObject();
  private BooleanObject hasSetSessionID = new BooleanObject();
  private BooleanObject hasSetRawJSON = new BooleanObject();
  private BooleanObject hasSetHeaders = new BooleanObject();
  private BooleanObject hasSetRawInputString = new BooleanObject();
  private BooleanObject hasSetJSONArrayMap = new BooleanObject();
  private BooleanObject hasSetResponseMessage = new BooleanObject();
  private BooleanObject hasSetResponseStatusCode = new BooleanObject();
  private BooleanObject hasSetResponseContentType = new BooleanObject();
  private BooleanObject hasSetResponseCookie = new BooleanObject();
  private BooleanObject hasSetValidatedMethod = new BooleanObject();
  private BooleanObject hasSetResponseAuthorizationHeader = new BooleanObject();

  public ContentData(HttpServerRequest request) {
    // extract each variable from request.
    // No need to have request as part of this object
    this.method = new String(request.method().name());
    // This actually a bit more complex, since I need to check for session id
    // this.sessionid = new String(request.cookies().get("SESSIONID"));
    this.cookieMapList = request.allCookies();
    // Cookie sessionCookie = request.cookies().get("SESSIONID") != null
    //   ? request.cookies().get("SESSIONID").stream().findFirst().orElse(null)
    //   : null;
    // response.addCookie(ServeCommon.generateSessionId());
    this.headers = request.requestHeaders();
    this.rawInputString = request
      .receive()
      .aggregate()
      .asString()
      .flatMap(str -> {
        if (str.length() > 0) {
          System.out.println("Received data");
          return Mono.just(str);
        } else {
          System.out.println("No input data received");
          // Not going receive data from a get, but do not end the workflow.
          return Mono.just("");
        }
      })
      .subscribeOn(Schedulers.boundedElastic());
    this.setResponseAuthorizationHeader("Bearer secrettoken");
  }

  public Mono<ContentData> checkAuthentication() {
    // Check the method - if its GET then return as normal.
    // If its PUT or POST check the value of the token
    // If its wrong return Mono.empty()
    // Otherwise proceed as nomral
    // Set status 401
    // Set failure message

    // -------------
    boolean authenitcated = ((this.headers.get(HttpHeaderNames.AUTHORIZATION) !=
        null) &&
      (this.getResponseAuthorizationHeader().equals(
          this.headers.get(HttpHeaderNames.AUTHORIZATION)
        )));
    if ((this.validatedMethod.equals("GETHTML")) || (authenitcated == true)) {
      // Yay authenticated
      this.responseStatusCode = 200;
      return Mono.just(this);
    }
    this.responseStatusCode = 401;
    this.responseContentType = "text/html";
    this.responseMessage = "<html>Access Denied</html>";
    System.out.println("Access Denied");
    return Mono.empty();
  }

  public Mono<ContentData> checkSESSIONID() {
    // Check that session id is set if this is PUT or POST
    // Set status 422 and return Mono.empty()
    // otherwise return normally.
    String sessionid = null;
    for (Map.Entry<
      CharSequence,
      List<Cookie>
    > entry : this.cookieMapList.entrySet()) {
      List<Cookie> cookieList = entry.getValue();
      for (Cookie cookie : cookieList) {
        if (cookie.name().equals("SESSIONID")) {
          if (!cookie.value().isBlank()) {
            sessionid = cookie.value();
          }
        }
      }
    }
    if ((sessionid == null) && (this.validatedMethod.equals("GETHTML"))) {
      this.responseCookie = ServeCommon.generateSessionId();
      sessionid = this.responseCookie.value();
    } else if (sessionid == null) {
      this.responseStatusCode = 422;
      this.responseContentType = "text/html";
      this.responseMessage = "<html>SESSIONID is missing</html>";
      System.out.println("SESSIONID is missing");
      return Mono.empty();
    }
    this.sessionid = sessionid;
    return Mono.just(this);
  }

  public Mono<ContentData> validateMethod() {
    // Probably easier with enum and switches
    String contentType = null;
    String contentTypeTemp = null;
    if (this.headers.get(HttpHeaderNames.CONTENT_TYPE) != null) {
      contentTypeTemp = this.headers.get(HttpHeaderNames.CONTENT_TYPE)
        .toLowerCase()
        .trim();
    } else {
      contentTypeTemp = "";
    }
    if (
      contentTypeTemp.startsWith(
        HttpHeaderValues.APPLICATION_JSON.toString().toLowerCase().trim()
      )
    ) {
      System.out.println("ContentType JSON");
      contentType = "JSON";
    } else if (
      contentTypeTemp.startsWith(
        HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()
          .toLowerCase()
          .trim()
      )
    ) {
      System.out.println("ContentType URLENCODED");
      contentType = "URLENCODED";
    } else if (
      contentTypeTemp.startsWith(
        HttpHeaderValues.TEXT_HTML.toString().toLowerCase().trim()
      )
    ) {
      System.out.println("ContentType HTML");
      contentType = "HTML";
    } else {
      System.out.println("ContentType NONE");
      contentType = "NONE";
    }

    String acceptEncodingTemp = null;
    String acceptEncoding = null;
    if (this.headers.get(HttpHeaderNames.ACCEPT) != null) {
      String[] acceptArray = this.headers.get(HttpHeaderNames.ACCEPT).split(
        ","
      );
      if (acceptArray.length > 0) {
        acceptEncodingTemp = acceptArray[0].trim();
      }
    }

    if (
      (acceptEncodingTemp != null) &&
      (acceptEncodingTemp
          .toLowerCase()
          .startsWith(HttpHeaderValues.TEXT_HTML.toString().toLowerCase()))
    ) {
      System.out.println("AcceptEncoding HTML");
      acceptEncoding = "HTML";
    } else if (
      (acceptEncodingTemp != null) &&
      (acceptEncodingTemp
          .toLowerCase()
          .startsWith(
            HttpHeaderValues.APPLICATION_JSON.toString().toLowerCase()
          ))
    ) {
      System.out.println("AcceptEncoding JSON");
      acceptEncoding = "JSON";
    } else {
      System.out.println("AcceptEncoding NONE");
      acceptEncoding = "NONE";
    }

    if ((contentType.equals("JSON")) && (this.method.equals("PUT"))) {
      System.out.println("validatedMethod = PUT");
      this.validatedMethod = "PUT";
    } else if (
      (contentType.equals("URLENCODED")) && (this.method.equals("POST"))
    ) {
      System.out.println("validatedMethod = POST");
      this.validatedMethod = "POST";
    } else if (
      ((contentType.equals("HTML")) || (acceptEncoding.equals("HTML"))) &&
      (this.method.equals("GET"))
    ) {
      System.out.println("validatedMethod = GETHTML");
      this.validatedMethod = "GETHTML";
    } else if (
      ((contentType.equals("JSON")) || (acceptEncoding.equals("JSON"))) &&
      (this.method.equals("GET"))
    ) {
      System.out.println("validatedMethod = GETJSON");
      this.validatedMethod = "GETJSON";
    } else {
      this.validatedMethod = "";
      this.responseStatusCode = 415;
      this.responseMessage = "";
      System.out.println(
        "Unsupported Media Type: Accept:\"" +
        acceptEncoding +
        "\" ContentType:\"" +
        contentType +
        "\" Method:\"" +
        this.method +
        "\""
      );
      return Mono.empty();
    }
    return Mono.just(this);
  }

  public Mono<ContentData> processPostData() {
    // --------------------- POST ---------------------
    // How about post be for adding fortunes
    // Requires SESSIONID
    // Scrub the input string and split at & (each flux is "key=value")
    Flux<String> fluxString = ServeCommon.convertMonoToFlux(
      this.rawInputString
    );
    // This is a post so split Flux of "Key=Value" -> Map<String, String>
    Mono<Map<String, String>> monoMapStringString = fluxString.collectMap(
      ServeCommon::getFormParamName,
      ServeCommon::getFormParamValue
    );
    // Convert Map<String, String> to JSON
    Mono<String> convertMonoMapString =
      ServeCommon.convertMonoMapToMonoStringGeneric(monoMapStringString);

    // JSON -> Flux<Map<String, String>>
    Flux<Map<String, String>> fluxString3 =
      ServeCommon.doConvertJSONArrayToValues(convertMonoMapString);
    Flux<String> aFluxString = fluxString3.flatMap(fm -> {
      FortuneDatabaseR2DBC.addFortuneData(this.sessionid, fm);
      return Flux.just("");
    });

    Mono<String> waiter = aFluxString.last("");
    this.responseStatusCode = 204;
    this.responseMessage = "";

    return Mono.just(this);
  }

  public Mono<ContentData> processPutData() {
    // ---------------------PUT --------------------
    // For adding metadata
    // Requires SESSIONID
    // This should already be in JSON format
    Flux<Map<String, String>> fluxPutString =
      ServeCommon.doConvertJSONArrayToValues(this.rawInputString);
    // Update the database
    Mono<String> aMonoString1 = ServeCommon.updateDataDBWithFluxString(
      this.sessionid,
      fluxPutString
    );
    this.responseStatusCode = 204;
    this.responseMessage = "";

    return Mono.just(this);
  }

  public Mono<ContentData> processGetHtmlData() {
    // ----------------- GET text/html  --------------------
    // this.responseMessage = ServeCommon.htmlResponse();
    this.responseMessage =
      "<html><head></head><body>regular html</body></html>";
    this.responseStatusCode = 200;
    this.responseContentType = "text/html";

    return Mono.just(this);
  }

  public Mono<ContentData> processGetJSONData() {
    // ----------- GET application/json ----------
    // SESSIONID required
    // Return fortune
    this.responseContentType = "application/json";
    this.responseStatusCode = 200;
    //This returns untrusted content; however, the JS client will display it rather than being pushed by us.
    Mono<String> getFortuneMono = FortuneDatabaseR2DBC.getFortune().subscribeOn(
      Schedulers.boundedElastic()
    );

    Mono<String> createResponseText = getFortuneMono.flatMap(fortune -> {
      String response = "[{\"fortune\":\"" + fortune + "\"}]";
      this.responseMessage = response;
      return (Mono.just(response));
    });

    return Mono.just(this);
  }

  public Mono<ContentData> processData() {
    // Check that POST + Content_Type X_WWW_FORM_URLENCODED
    // Or PUT with content type APPLICATION_JSON
    // else set status code 415
    // else GET return html
    // process the data branching for PUT and POST
    // Finally both should result in a JSON string
    // Set status 422 and return Mono.empty()
    // otherwise return normally.
    // This return type should be the string (html)
    // sent to the client
    if (this.validatedMethod.equals("POST")) {
      return this.processPostData();
    } else if (this.validatedMethod.equals("PUT")) {
      return this.processPutData();
    } else if (this.validatedMethod.equals("GETHTML")) {
      return this.processGetHtmlData();
    } else if (this.validatedMethod.equals("GETJSON")) {
      return this.processGetJSONData();
    }
    System.out.println("Unsupported Method");
    this.responseStatusCode = 415;
    return Mono.empty();
  }

  // This way a modification to one method does not need to be duplicated
  public ContentData setMethod(String method) {
    // Although this still doesn't have a perfect concurrency garentee
    if (hasSetMethod.flipToTrue()) {
      this.method = method;
    }
    return this;
  }

  public String getMethod() {
    return this.method;
  }

  public ContentData setRawInputString(Mono<String> rawInputString) {
    if (hasSetRawInputString.flipToTrue()) {
      this.rawInputString = rawInputString;
    }
    return this;
  }

  public Mono<String> getRawInputString() {
    return this.rawInputString;
  }

  public ContentData setRawJSON(String rawJSON) {
    if (this.hasSetRawJSON.flipToTrue()) {
      this.rawJSON = rawJSON;
    }
    return this;
  }

  public String getRawJSON() {
    return this.rawJSON;
  }

  public ContentData setSessionId(String sessionid) {
    if (this.hasSetSessionID.flipToTrue()) {
      this.sessionid = sessionid;
    }
    return this;
  }

  public String getSessionId() {
    return this.sessionid;
  }

  public ContentData setJSONMap(List<Map<String, String>> jsonMap) {
    if (this.hasSetJSONArrayMap.flipToTrue()) {
      this.jsonArrayMap = jsonMap;
    }
    return this;
  }

  public Flux<Map<String, String>> getRawJSONAsFlux() {
    return Flux.fromIterable(this.jsonArrayMap);
  }

  public ContentData setHeaders(HttpHeaders headers) {
    if (this.hasSetHeaders.flipToTrue()) {
      this.headers = headers;
    }
    return this;
  }

  public HttpHeaders getHeaders() {
    return this.headers;
  }

  public ContentData setMessage(String message) {
    if (this.hasSetResponseMessage.flipToTrue()) {
      this.responseMessage = message;
    }
    return this;
  }

  public String getResponseMessage() {
    return this.responseMessage;
  }

  public int getResponseStatusCode() {
    return this.responseStatusCode;
  }

  public ContentData setResponseStatusCode(int statusCode) {
    if (this.hasSetResponseStatusCode.flipToTrue()) {
      this.responseStatusCode = statusCode;
    }
    return this;
  }

  public String getResponseContentType() {
    return this.responseContentType;
  }

  public ContentData setResponseContentType(String contentType) {
    if (this.hasSetResponseContentType.flipToTrue()) {
      this.responseContentType = contentType;
    }
    return this;
  }

  public Cookie getResponseCookie() {
    return this.responseCookie;
  }

  public ContentData setResponseCookie(Cookie responseCookie) {
    if (this.hasSetResponseCookie.flipToTrue()) {
      this.responseCookie = responseCookie;
    }
    return this;
  }

  public String getValidatedMethod() {
    return this.validatedMethod;
  }

  public ContentData setValidatedMethod(String validatedMethod) {
    if (this.hasSetValidatedMethod.flipToTrue()) {
      this.validatedMethod = validatedMethod;
    }
    return this;
  }

  public String getResponseAuthorizationHeader() {
    return this.responseAuthorizationHeader;
  }

  public void setResponseAuthorizationHeader(
    String responseAuthorizationHeader
  ) {
    if (this.hasSetResponseAuthorizationHeader.flipToTrue()) {
      this.responseAuthorizationHeader = responseAuthorizationHeader;
    }
  }
}
