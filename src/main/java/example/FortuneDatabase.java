package example;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import org.h2.Driver;

public class FortuneDatabase {

  // private final JdbcConnectionPool pool;

  // ds = new JdbcDataSource();
  // ds.setURL("jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1");

  public FortuneDatabase() {
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
      addFortune(initialFortunes[i]);
    }
  }

  public static String getFortune() {
    String fortune = new String();
    String dbUrl =
      "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    try {
      // Hope this does not rely on scope
      Class.forName("org.h2.Driver");
      try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
        PreparedStatement stmt = conn.prepareStatement(
          "SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1"
        );
        ResultSet rs = stmt.executeQuery();
        fortune = rs.next() ? rs.getString("text") : "No fortunes available";
      } catch (Exception e) {
        fortune = "";
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
      fortune = "";
    }

    return fortune;
  }

  public static void addFortune(String fortune) {
    String dbUrl =
      "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    String trimmedFortune = new String(truncateString(fortune, 254));
    try {
      // Hope this does not rely on scope
      Class.forName("org.h2.Driver");
      try (Connection conn = DriverManager.getConnection(dbUrl, "sa", "")) {
        conn
          .createStatement()
          .execute(
            "CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY AUTO_INCREMENT, text VARCHAR(255))"
          );
        String sql = "INSERT INTO fortunes (text) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
          stmt.setString(1, trimmedFortune);
          stmt.executeUpdate();
        } catch (Exception e) {
          e.printStackTrace();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
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
