/*
 * DokodemoDoor — pair-based cross-world door portals for Minecraft.
 * Copyright (C) 2026 qs5668
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package top.midream.ddoor.storage;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.log.DoorLog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
              uses BIGINT NOT NULL DEFAULT 0,
              enabled TINYINT NOT NULL DEFAULT 1
            )""";

    private static final String SETTINGS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS ddoor_player_settings (
              uuid CHAR(36) PRIMARY KEY,
              mode VARCHAR(16) NOT NULL DEFAULT 'WALK',
              simple_info TINYINT NOT NULL DEFAULT 0
            )""";

    private static final String LOGS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS ddoor_logs (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              door_id CHAR(36) NOT NULL,
              door_name VARCHAR(32) NOT NULL,
              world VARCHAR(64) NOT NULL,
              x INT NOT NULL,
              y INT NOT NULL,
              z INT NOT NULL,
              player_name VARCHAR(17) NOT NULL,
              action VARCHAR(12) NOT NULL,
              time BIGINT NOT NULL,
              INDEX idx_ddoor_logs_time (time)
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
            st.execute(SETTINGS_SCHEMA);
            st.execute(LOGS_SCHEMA);
        }
        migrateDoorsEnabled();
    }

    /** v1.0.2 installs lack the enabled column — add it without touching data. */
    private void migrateDoorsEnabled() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ddoor_doors' AND COLUMN_NAME='enabled'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE ddoor_doors ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1");
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
                "REPLACE INTO ddoor_doors (id,name,owner,world,x,y,z,facing,paired_id,created_at,uses,enabled) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
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
    public Map<UUID, PlayerPrefs> loadPlayerSettings() throws Exception {
        Map<UUID, PlayerPrefs> out = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid,mode,simple_info FROM ddoor_player_settings")) {
            while (rs.next()) {
                out.put(UUID.fromString(rs.getString("uuid")),
                        new PlayerPrefs(rs.getString("mode"), rs.getInt("simple_info") != 0));
            }
        }
        return out;
    }

    @Override
    public void savePlayerSettings(UUID uuid, String mode, boolean simpleInfo) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "REPLACE INTO ddoor_player_settings (uuid,mode,simple_info) VALUES (?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, mode);
            ps.setInt(3, simpleInfo ? 1 : 0);
            ps.executeUpdate();
        }
    }

    @Override
    public void insertLog(DoorLog log) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ddoor_logs (door_id,door_name,world,x,y,z,player_name,action,time) VALUES (?,?,?,?,?,?,?,?,?)")) {
            SqliteStore.bindLog(ps, log);
            ps.executeUpdate();
        }
    }

    @Override
    public List<DoorLog> loadRecentLogs(long sinceMillis) throws Exception {
        List<DoorLog> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT door_id,door_name,world,x,y,z,player_name,action,time FROM ddoor_logs WHERE time>=? ORDER BY time DESC")) {
            ps.setLong(1, sinceMillis);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(SqliteStore.fromLogRow(rs));
                }
            }
        }
        return out;
    }

    @Override
    public void cleanupLogs(long beforeMillis) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ddoor_logs WHERE time<?")) {
            ps.setLong(1, beforeMillis);
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
