package example.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// import java.util.concurrent.atomic.AtomicBoolean;

// Your asking yourself, isn't there already a class for this?
// It has to do with scope
public class ContentData {

  private List<Map<String, String>> jsonArrayMap = null;
  private String sessionid = null;

  public ContentData(List<Map<String, String>> jsonArrayMap, String sessionid) {
    this.jsonArrayMap = jsonArrayMap;
    this.sessionid = sessionid;
  }

  public List<Map<String, String>> getArrayMap() {
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
