package example.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

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
  private integer statusCode = 401;
  private String message = "Unauthorized";
  private HttpServerRequest request = null;

  // This object will not work if multiple clients were combined:
  // List<Map<String,Map<String,String>>>
  // Or just one:
  // Could be Map<String, Map<String, String>>
  // Or the values could be seperate
  // In fact the constructor should just take request, and resolve the rest.

  public ContentData(HttpServerRequest request) {
    this.request = request;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  // This way a modification to one method does not need to be duplicated
  public ContentData setMethod(String method) {
    setMethod(method);
    return this;
  }

  public String getMethod() {
    return this.method;
  }

  public void setRawInputString(String rawInputString) {
    this.rawInputString = rawInputString;
  }

  public ContentData setRawInputString(String rawInputString) {
    setRawInputString(rawInputString);
    return this;
  }

  public String getRawInputString() {
    return this.rawInputString;
  }

  public ContentData setRawJSON(String rawJSON) {
    setRawJSON(rawJSON);
    return this;
  }

  public void setRawJSON(String rawJSON) {
    this.rawJSON = rawJSON;
  }

  public String getRawJSON() {
    return this.rawJSON;
  }

  public void setSessionId(String sessionid) {
    this.sessionid = sessionid;
  }

  public ContentData setSessionId(String sessionid) {
    setSessionId(sessionid);
    return this;
  }

  public String getSessionId() {
    return this.sessionid;
  }

  public void setJSONMap(List<Map<String, String>> jsonMap) {
    this.jsonArrayMap = jsonMap;
  }

  public ContentData setJSONMap(List<Map<String, String>> jsonMap) {
    setJSONMap(jsonMap);
    return this;
  }

  public Flux<Map<String, String>> getRawJSONAsFlux() {
    return Flux.fromIterable(this.jsonArrayMap);
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public ContentData setHeaders(Map<String, String> headers) {
    setHeaders(headers);
    return this;
  }

  public Map<String, String> getHeaders() {
    return this.headers;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ContentData setMessage(String message) {
    setMessage(message);
    return this;
  }

  public String getMessage() {
    return this.message;
  }

  public integer getStatusCode() {
    return this.statusCode;
  }

  public void setStatusCode(integer statusCode) {
    this.statusCode = statusCode;
  }

  public ContentData setStatusCode(integer statusCode) {
    setStatusCode(statusCode);
    return this;
  }
}
