import java.sql.*;

public class DBAuthService implements AuthService {

    @Override
    public Record findRecord(String login, String password) {
        Record record = null;

        Connection conn = DBService.connectDB();
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
            DBService.disconnectDB(conn);
            return record;
        }
    }

    @Override
    public boolean changeName(Record record, String name) {
        Connection conn = DBService.connectDB();

        try {
            PreparedStatement st  = null;
            st = conn.prepareStatement("UPDATE users\n" +
                    "   SET name = ?\n" +
                    " WHERE id = ?;");

            st.setString(1, name);
            st.setInt(2, record.getId());
            st.executeUpdate();
            st.close();

            record.setName(name);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBService.disconnectDB(conn);
        }
    }


}