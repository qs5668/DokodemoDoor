package top.midream.ddoor.storage;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import top.midream.ddoor.door.DoorRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class MysqlStore implements DoorStore {

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS ddoor_doors (
              id CHAR(36) PRIMARY KEY,
              name VARCHAR(32) NOT NULL,
              owner CHAR(36) NOT NULL,
              world VARCHAR(64) NOT NULL,
              x INT NOT NULL,
              y INT NOT NULL,
              z INT NOT NULL,
              facing VARCHAR(16) NOT NULL,
              paired_id CHAR(36) NULL,
              created_at BIGINT NOT NULL,
              uses BIGINT NOT NULL DEFAULT 0
            )""";

    private final Plugin plugin;
    private Connection conn;

    public MysqlStore(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() throws Exception {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("storage.mysql");
        String host = cfg.getString("host", "localhost");
        int port = cfg.getInt("port", 3306);
        String db = cfg.getString("database", "ddoor");
        Properties props = new Properties();
        props.setProperty("user", cfg.getString("username", "root"));
        props.setProperty("password", cfg.getString("password", ""));
        props.setProperty("useSSL", "false");
        props.setProperty("autoReconnect", "true");
        conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + db, props);
        try (Statement st = conn.createStatement()) {
            st.execute(SCHEMA);
        }
    }

    @Override
    public List<DoorRecord> loadAll() throws Exception {
        List<DoorRecord> out = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM ddoor_doors")) {
            while (rs.next()) {
                out.add(SqliteStore.fromRow(rs));
            }
        }
        return out;
    }

    @Override
    public void upsert(DoorRecord door) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "REPLACE INTO ddoor_doors (id,name,owner,world,x,y,z,facing,paired_id,created_at,uses) VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
            SqliteStore.bind(ps, door);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(UUID id) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ddoor_doors WHERE id=?")) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {
        }
    }
}
