package example.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// import java.util.concurrent.atomic.AtomicBoolean;

// This class just holds data, but does not call any processing methods.
// Then it can be passed to a flux or mono

public class ContentData {

  // The Key/Value pairs (where each map has two values {"Key":"whatever","Value":"whateverelse"})
  // Regular map in case there are duplicate keys
  private List<Map<String, String>> jsonArrayMap = null;
  // The SESSIONID cookie value, so that it only needs to be resolved once
  private String sessionid = null;
  // A Hash Map of cookies (loses data to duplicates)
  private Map<String, String> cookies = null;
  // A Hash Map of headers (loses data to duplicates)
  private Map<String, String> headers = null;

  // This object will not work if multiple clients were combined:
  // List<Map<String,Map<String,String>>>
  // Or just one:
  // Could be Map<String, Map<String, String>>
  // Or the values could be seperate
  // In fact the constructor should just take request, and resolve the rest.
  public ContentData(Map<String, String> keyValueMap, String sessionid) {
    this.jsonArrayMap = keyValueMap;
    this.sessionid = sessionid;
  }

  // public ContentData(
  //   Map<String, Map<String, String>> sessionidWithkeyValueMap
  // ) {
  //   // Since this is a map it could actually handle multiple sessionid.
  //   this.jsonArrayMap = jsonArrayMap;
  //   this.sessionid = sessionid;
  // }

  public Map<String, String> getArrayMap() {
    return this.jsonArrayMap;
  }

  public String getSessionId() {
    return this.sessionid;
  }

  public Flux<Map<String, String>> getArrayMapAsFlux() {
    return Flux.fromIterable(this.jsonArrayMap);
  }

  public Mono<String> getSessionIdAsMono() {
    return Mono.just(this.sessionid);
  }
}
