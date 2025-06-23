package example.impl;

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
  private int responseStatusCode = 401;
  private String responseMessage = null;
  private String responseContentType = null;
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

  public ContentData(HttpServerRequest request) {
    // extract each variable from request.
    // No need to have request as part of this object
    this.method = new String(request.method().name());
    // This actually a bit more complex, since I need to check for session id
    // this.sessionid = new String(request.cookies().get("SESSIONID"));
    Map<CharSequence, List<Cookie>> cookieMapList = request.allCookies();
    for (Map.Entry<
      CharSequence,
      List<Cookie>
    > entry : cookieMapList.entrySet()) {
      List<Cookie> cookieList = entry.getValue();
      for (Cookie cookie : cookieList) {
        if (cookie.name() == "SESSIONID") {
          if (!cookie.value().isBlank()) {
            this.sessionid = cookie.value();
          }
        }
      }
    }

    this.rawInputString = request
      .receive()
      .aggregate()
      .asString()
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
    this.headers = request.requestHeaders();
    String expectedToken = "Bearer secret-token";
    // -------------
    if (
      (this.headers.get(HttpHeaderNames.AUTHORIZATION) != null) &&
      (expectedToken.equals(this.headers.get(HttpHeaderNames.AUTHORIZATION)))
    ) {
      // Yay authenticated
    } else {
      // Not authenticated
      this.responseStatusCode = 401;
      this.responseContentType = "text/html";
      this.responseMessage = "<html>Access Denied</html>";
    }
    // .then()
    // .subscribe();
    // 422 Unprocessable Content if SESSIONID is missing
    if (this.sessionid == null) {
      this.responseStatusCode = 422;
      this.responseContentType = "text/html";
      this.responseMessage = "<html>SESSIONID is missing</html>";
    }

    if (
      (this.headers.get(HttpHeaderNames.CONTENT_TYPE) != null) &&
      (this.headers.get(HttpHeaderNames.CONTENT_TYPE)
          .toLowerCase()
          .startsWith(
            HttpHeaderValues.APPLICATION_JSON.toString().toLowerCase()
          )) &&
      (this.method == "PUT")
    ) {
      // Yay PUT
    } else if (
      (this.headers.get(HttpHeaderNames.CONTENT_TYPE) != null) &&
      (this.headers.get(HttpHeaderNames.CONTENT_TYPE)
          .toLowerCase()
          .startsWith(
            HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()
              .toLowerCase()
          )) &&
      (this.method == "POST")
    ) {
      // Yay POST
    } else {
      this.responseStatusCode = 415;
      this.responseMessage = "";
    }
  }

  public Mono<ContentData> checkAuthentication(String token) {
    // Check the method - if its GET then return as normal.
    // If its PUT or POST check the value of the token
    // If its wrong return Mono.empty()
    // Otherwise proceed as nomral
    // Set status 401
    // Set failure message
    return Mono.just(this);
  }

  public Mono<ContentData> checkSESSIONID() {
    // Check that session id is set if this is PUT or POST
    // Set status 422 and return Mono.empty()
    // otherwise return normally.
    return Mono.just(this);
  }

  public Mono<String> processData() {
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
    return Mono.just("this");
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

  // public ContentData setHeaders(Map<String, String> headers) {
  //   if (this.hasSetHeaders.flipToTrue()) {
  //     this.headers = headers;
  //   }
  //   return this;
  // }

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

  public int getStatusCode() {
    return this.responseStatusCode;
  }

  public ContentData setStatusCode(int statusCode) {
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
}
