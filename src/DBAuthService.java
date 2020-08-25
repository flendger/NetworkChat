import java.sql.*;

public class DBAuthService implements AuthService {

    @Override
    public Record findRecord(String login, String password) {
        Record record = null;

        Connection conn = DBAuthService.connectDB();
        try {
            PreparedStatement st = conn.prepareStatement("SELECT *" +
                    "  FROM users\n" +
                    " WHERE login = ? AND \n" +
                    "       password = ?;");
            st.setString(1, login);
            st.setString(2, password);
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                record = new Record(rs.getInt("id"), rs.getString("name"), rs.getString("login"), rs.getString("password"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBAuthService.disconnectDB(conn);
            return record;
        }
    }

    private static Connection connectDB() {
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

    private static void disconnectDB(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


}