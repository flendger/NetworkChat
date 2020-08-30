import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBService {
    public static Connection connectDB() {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:chat.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            return conn;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            return conn;
        }
    }

    public static void disconnectDB(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
