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
import reactor.netty.http.server.HttpServerRequest;

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
        this.conn.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY AUTO_INCREMENT, key VARCHAR(255) DEFAULT '', text VARCHAR(255) DEFAULT '')"
        );
      } catch (Exception e) {
        System.out.println("Error connecting to database");
        e.printStackTrace();
      }
    } catch (Exception e) {
      System.out.println("Error loading driver");
      e.printStackTrace();
    }

    try {
      this.server = Server.createTcpServer(
        "-tcp",
        "-tcpAllowOthers",
        "-tcpPort",
        "9092"
      );
      this.server.start();
    } catch (Exception e) {
      System.out.println("Error starting server");
      e.printStackTrace();
    }

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

    for (int i = 0; i < initialFortunes.length; i++) {
      FortuneDatabase.addFortune(initialFortunes[i]);
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

  public static void addFortuneData(
    String sessionid,
    Map<String, String> fortune
  ) {
    for (Map.Entry<String, String> entry : fortune.entrySet()) {
      addFortuneData(sessionid, entry.getKey(), entry.getValue());
    }
  }

  public static void addDataData(String sessionid, Map<String, String> data) {
    for (Map.Entry<String, String> entry : data.entrySet()) {
      addDataData(sessionid, entry.getKey(), entry.getValue());
    }
  }

  public static void addFortuneData(
    String sessionid,
    String key,
    String value
  ) {
    // System.out.println("Adding fortune - via string: " + fortune);
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("tcp://localhost:9092/mem:fortunes")
        .username("sa")
        .password("")
        .build()
    );

    String trimmedSessionId = new String(truncateString(sessionid, 254));
    String trimmedKey = new String(truncateString(key, 254));
    String trimmedValue = new String(truncateString(value, 254));
    try {
      Class.forName("org.h2.Driver");
      Mono<H2Result> insertData = Mono.from(connectionFactory.create()).flatMap(
          connection ->
            connection
              .createStatement("INSERT INTO fortunes (text) VALUES ($1)")
              .bind("$1", trimmedValue)
              .execute()
              .next()
              .doFinally(signalType -> connection.close())
        );
      Disposable disposable = insertData
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
    } catch (Exception e) {
      System.out.println("Error in addFortune");
      e.printStackTrace();
      // fortune = "";
    }
  }

  public static void addDataData(String sessionid, String key, String value) {
    // System.out.println("Adding fortune - via string: " + fortune);
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("tcp://localhost:9092/mem:fortunes")
        .username("sa")
        .password("")
        .build()
    );

    String trimmedSessionId = new String(truncateString(sessionid, 254));
    String trimmedKey = new String(truncateString(key, 254));
    String trimmedValue = new String(truncateString(value, 254));
    try {
      Class.forName("org.h2.Driver");
      Mono<H2Result> insertData = Mono.from(connectionFactory.create()).flatMap(
          connection ->
            connection
              .createStatement(
                "INSERT INTO  data (sessionid, jsonkey, jsonvalue) VALUES ($1, $2, $3)"
              )
              .bind("$1", trimmedSessionId)
              .bind("$2", trimmedKey)
              .bind("$3", trimmedValue)
              .execute()
              .next()
              .doFinally(signalType -> connection.close())
        );
      Disposable disposable = insertData
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
      // return insertFortune;
    } catch (Exception e) {
      System.out.println("Error in addFortune");
      e.printStackTrace();
      // fortune = "";
    }
  }

  public static void addFortune(String fortune, HttpServerRequest request) {
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

  public static void createDataTable() {
    // System.out.println("Adding fortune - via string: " + fortune);
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("tcp://localhost:9092/mem:fortunes")
        .username("sa")
        .password("")
        .build()
    );

    try {
      Class.forName("org.h2.Driver");
      Mono<H2Result> insertData = Mono.from(connectionFactory.create()).flatMap(
          connection ->
            connection
              .createStatement(
                "CREATE TABLE IF NOT EXISTS data (id BIGINT PRIMARY KEY AUTO_INCREMENT, sessionid VARCHAR(255), jsonkey VARCHAR(255), jsonvalue VARCHAR(255), time BIGINT DEFAULT (DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', CURRENT_TIMESTAMP())));"
              )
              .execute()
              .next()
              .doFinally(signalType -> connection.close())
        );
      Disposable disposable = insertData
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
      // return insertFortune;
    } catch (Exception e) {
      System.out.println("Error in addFortune");
      e.printStackTrace();
      // fortune = "";
    }
  }

  public static void createFortuneTable() {
    // System.out.println("Adding fortune - via string: " + fortune);
    H2ConnectionFactory connectionFactory = new H2ConnectionFactory(
      io.r2dbc.h2.H2ConnectionConfiguration.builder()
        .url("tcp://localhost:9092/mem:fortunes")
        .username("sa")
        .password("")
        .build()
    );

    try {
      Class.forName("org.h2.Driver");
      Mono<H2Result> insertData = Mono.from(connectionFactory.create()).flatMap(
          connection ->
            connection
              .createStatement(
                "CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY AUTO_INCREMENT, key VARCHAR(255) DEFAULT '', text VARCHAR(255) DEFAULT '')"
              )
              .execute()
              .next()
              .doFinally(signalType -> connection.close())
        );
      Disposable disposable = insertData
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
      // return insertFortune;
    } catch (Exception e) {
      System.out.println("Error in addFortune");
      e.printStackTrace();
      // fortune = "";
    }
  }
}
