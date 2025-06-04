package example;

import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.h2.H2Result;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class FortuneDatabaseR2DBC {

  public FortuneDatabaseR2DBC() {
    initializeDatabase();
  }

  public static void initializeDatabase() {
    String[] initialFortunes = {
      "Today is your lucky day!",
      "Good fortune will come to you",
      "A pleasant surprise is waiting for you",
      "Fortune favors the bold",
      "A journey of a thousand miles begins with a single step",
      "The best time to plant a tree was 20 years ago. The second best time is now",
      "Be the change you wish to see in the world",
      "Today is the first day of the rest of your life",
      "Time is gold",
      "Patience is a virtue",
      "The future belongs to those who believe in the beauty of their dreams",
      "Life is what happens while you are busy making other plans",
    };
    String dbUrl =
      "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    try {
      Class.forName("org.h2.Driver");
      try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
        conn
          .createStatement()
          .execute(
            "CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY AUTO_INCREMENT, text VARCHAR(255))"
          );
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (int i = 0; i < initialFortunes.length; i++) {
      System.out.println("Adding initial fortune: " + initialFortunes[i]);
      addFortune(initialFortunes[i]);
    }
  }

  public static Mono<String> getFortune() {
    Mono<String> fortuneMono = null;
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("mem:fortunes;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;")
        .username("sa")
        .password("")
        .build()
    );

    String fortune = new String();

    try {
      Class.forName("org.h2.Driver");
      Flux<String> fortunesFlux = Mono.from(
        connectionFactory.create()
      ).flatMapMany(connection ->
        connection
          .createStatement(
            "SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1"
          )
          .execute()
          // H2Result
          .flatMap(result -> {
            return (
              result.map((row, rowMetadata) -> {
                return (row.get("text", String.class));
              })
            );
          })
          .doFinally(signalType -> connection.close())
      );

      Mono<List<String>> fortunesMono = fortunesFlux.collectList();
      fortuneMono = fortunesMono.flatMap(
        FortuneDatabaseR2DBC::convertMonoFortunesToFortune
      );
    } catch (Exception e) {
      e.printStackTrace();
      fortuneMono = Mono.just("");
    }

    return fortuneMono;
  }

  public static Mono<String> convertMonoFortunesToFortune(List<String> list) {
    if (!(list.isEmpty())) {
      return Mono.just(((List<String>) list).get(0));
    }
    return Mono.just("");
  }

  public static void addFortune(Map<String, String> fortune) {
    System.out.println("Adding fortune - via map");
    HashMap<String, String> fortuneHashMap = new HashMap<String, String>(
      fortune
    );
    String[] fortunes = fortuneHashMap.values().toArray(new String[0]);
    for (int i = 0; i < fortunes.length; i++) {
      addFortune(fortunes[i]);
    }
  }

  public static void addFortune(String fortune) {
    System.out.println("Adding fortune - via string: " + fortune);
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("mem:fortunes;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;")
        .username("sa")
        .password("")
        .build()
    );

    String trimmedFortune = new String(truncateString(fortune, 254));
    try {
      Class.forName("org.h2.Driver");

      Mono<Void> insertFortune = Mono.from(connectionFactory.create()).flatMap(
        connection -> {
          Mono<H2Result> insertData = Mono.from(
            connection
              .createStatement("INSERT INTO fortunes (text) VALUES ($1)")
              .bind("$1", trimmedFortune)
              .execute()
          );
          insertData.subscribeOn(Schedulers.boundedElastic()).subscribe();
          Mono<Void> closeConnection = Mono.from(connection.close());
          return closeConnection;
        }
      );
      insertFortune.subscribe();
    } catch (Exception e) {
      e.printStackTrace();
      fortune = "";
    }
  }

  public static String truncateString(String text, int maxLength) {
    if (text == null) {
      return null;
    }
    if (text.length() <= maxLength) {
      return text;
    } else {
      return text.substring(0, maxLength);
    }
  }
}
