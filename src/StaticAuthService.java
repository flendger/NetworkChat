import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class StaticAuthService implements AuthService {
    private final Set<Record> records;

    public StaticAuthService() {
        records = new HashSet<>();
        records.add(new Record(1, "Barboss", "l1", "p1"));
        records.add(new Record(2, "Kelvin", "l2", "p2"));
        records.add(new Record(3, "Nicky", "l3", "p3"));
        records.add(new Record(4, "Klaus", "l4", "p4"));
    }

    @Override
    public Record findRecord(String login, String password) {
        for (Record record : records) {
            if (record.getLogin().equals(login) && record.getPassword().equals(password)) {
                return record;
            }
        }
        return null;
    }

    @Override
    public boolean changeName(Record record, String name) {
        return false;
    }
}