package example;

import example.FortuneDatabase;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.h2.H2Result;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.h2.tools.Server;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class FortuneDatabaseR2DBC {

  private Connection conn = null;
  private Server server = null;

  public FortuneDatabaseR2DBC() {
    String dbUrl =
      "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    try {
      Class.forName("org.h2.Driver");
      try {
        this.conn = DriverManager.getConnection(dbUrl, "sa", "");
        this.conn.createStatement()
          .execute(
            "CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY AUTO_INCREMENT, text VARCHAR(255))"
          );
      } catch (Exception e) {
        System.out.println("Error connecting to database");
        e.printStackTrace();
      }
    } catch (Exception e) {
      System.out.println("Error loading driver");
      e.printStackTrace();
    }

    // Add test here to see that db is not gone

    try {
      this.server = Server.createTcpServer(
        "-tcp",
        "-tcpAllowOthers",
        "-tcpPort",
        "9092"
      );
      this.server.start();
      // System.out.println(this.server.getURL());
    } catch (Exception e) {
      System.out.println("Error starting server");
      e.printStackTrace();
    }

    // System.out.println(FortuneDatabase.getFortune());

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
    // Control multiple access by using tcp
    // Database URL: jdbc:h2:tcp://localhost/mem:fortunes
    // jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    // .doOnError(e -> log.error("Failed to save fortune", e))
    //     .onErrorResume(e -> Mono.empty())
    // Seems like h2 is being closed for some reason
    // ;INIT=CREATE SCHEMA IF NOT EXISTS test_schema

    for (int i = 0; i < initialFortunes.length; i++) {
      // System.out.println("Adding initial fortune: " + initialFortunes[i]);
      addFortune(initialFortunes[i]);
    }
  }

  public static Mono<String> getFortune() {
    Mono<String> fortuneMono = null;
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("tcp://localhost:9092/mem:fortunes")
        .username("sa")
        .password("")
        .build()
    );

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
            // System.out.println("Processing getFortune result");
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
    // System.out.println("Adding fortune - via map");
    HashMap<String, String> fortuneHashMap = new HashMap<String, String>(
      fortune
    );
    String[] fortunes = fortuneHashMap.values().toArray(new String[0]);
    for (int i = 0; i < fortunes.length; i++) {
      addFortune(fortunes[i]);
    }
  }

  // This works fine via jdbc
  public static void addFortune(String fortune) {
    System.out.println("Adding fortune - via string: " + fortune);
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("tcp://localhost:9092/mem:fortunes")
        .username("sa")
        .password("")
        .build()
    );

    String trimmedFortune = new String(truncateString(fortune, 254));
    try {
      Class.forName("org.h2.Driver");
      Mono<H2Result> insertFortune = Mono.from(
        connectionFactory.create()
      ).flatMap(connection ->
        connection
          .createStatement("INSERT INTO fortunes (text) VALUES ($1)")
          .bind("$1", trimmedFortune)
          .execute()
          .next()
          .doFinally(signalType -> connection.close())
      );
      Disposable disposable = insertFortune
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
      // return insertFortune;
    } catch (Exception e) {
      System.out.println("Error in addFortune");
      e.printStackTrace();
      // fortune = "";
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
