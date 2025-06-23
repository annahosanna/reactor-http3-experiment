package example.impl;

import example.impl.BooleanObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

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
  private String rawInputString = null;
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
  private Map<String, String> headers = null;
  // Some default values
  private int statusCode = 401;
  private String message = null;
  // Make sure the value is assigned once, and cannot be changed
  // An attempt to change it just returns the current value
  private BooleanObject hasSetMethod = new BooleanObject();
  private BooleanObject hasSetSessionID = new BooleanObject();
  private BooleanObject hasSetRawJSON = new BooleanObject();
  private BooleanObject hasSetHeaders = new BooleanObject();
  private BooleanObject hasSetRawInputString = new BooleanObject();
  private BooleanObject hasSetJSONArrayMap = new BooleanObject();
  private BooleanObject hasSetMessage = new BooleanObject();
  private BooleanObject hasSetStatusCode = new BooleanObject();

  public ContentData(HttpServerRequest request) {
    // extract each variable from request.
    // No need to have request as part of this object
    this.method = new String(request.method().name());
    // This actually a bit more complex, since I need to check for session id
    this.sessionid = new String(request.cookies().get("SESSIONID").value());
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

  public ContentData setRawInputString(String rawInputString) {
    if (hasSetRawInputString.flipToTrue()) {
      this.rawInputString = rawInputString;
    }
    return this;
  }

  public String getRawInputString() {
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

  public ContentData setHeaders(Map<String, String> headers) {
    if (this.hasSetHeaders.flipToTrue()) {
      this.headers = headers;
    }
    return this;
  }

  public Map<String, String> getHeaders() {
    return this.headers;
  }

  public ContentData setMessage(String message) {
    if (this.hasSetMessage.flipToTrue) {
      this.message = message;
    }
    return this;
  }

  public String getMessage() {
    return this.message;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public ContentData setStatusCode(int statusCode) {
    if (this.hasSetStatusCode.flipToTrue()) {
      this.statusCode = statusCode;
    }
    return this;
  }
}
