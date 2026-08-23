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
import org.bukkit.plugin.Plugin;
import top.midream.ddoor.door.DoorRecord;
import top.midream.ddoor.log.DoorLog;

import java.io.File;
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
import java.util.UUID;

public class SqliteStore implements DoorStore {

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS ddoor_doors (
              id CHAR(36) PRIMARY KEY,
              name TEXT NOT NULL,
              owner CHAR(36) NOT NULL,
              world TEXT NOT NULL,
              x INTEGER NOT NULL,
              y INTEGER NOT NULL,
              z INTEGER NOT NULL,
              facing TEXT NOT NULL,
              paired_id CHAR(36),
              created_at BIGINT NOT NULL,
              uses BIGINT NOT NULL DEFAULT 0,
              enabled INTEGER NOT NULL DEFAULT 1,
              entity_support INTEGER NOT NULL DEFAULT 0
            )""";

    private static final String SETTINGS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS ddoor_player_settings (
              uuid CHAR(36) PRIMARY KEY,
              mode VARCHAR(16) NOT NULL DEFAULT 'WALK',
              simple_info INTEGER NOT NULL DEFAULT 0
            )""";

    private static final String LOGS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS ddoor_logs (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              door_id CHAR(36) NOT NULL,
              door_name TEXT NOT NULL,
              world TEXT NOT NULL,
              x INTEGER NOT NULL,
              y INTEGER NOT NULL,
              z INTEGER NOT NULL,
              player_name VARCHAR(17) NOT NULL,
              action VARCHAR(12) NOT NULL,
              time BIGINT NOT NULL
            )""";

    private final Plugin plugin;
    private Connection conn;

    public SqliteStore(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() throws Exception {
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists()) dir.mkdirs();
        conn = DriverManager.getConnection("jdbc:sqlite:" + new File(dir, "ddoor.db").getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute(SCHEMA);
            st.execute(SETTINGS_SCHEMA);
            st.execute(LOGS_SCHEMA);
            st.execute("CREATE INDEX IF NOT EXISTS idx_ddoor_logs_time ON ddoor_logs(time)");
        }
        migrateDoorsEnabled();
        migrateDoorsEntitySupport();
    }

    /** v1.0.2 installs lack the enabled column — add it without touching data. */
    private void migrateDoorsEnabled() throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(ddoor_doors)")) {
            while (rs.next()) {
                if ("enabled".equalsIgnoreCase(rs.getString("name"))) return;
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE ddoor_doors ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1");
        }
    }

    /** v1.0.5 installs lack the entity_support column — add it without touching data. */
    private void migrateDoorsEntitySupport() throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(ddoor_doors)")) {
            while (rs.next()) {
                if ("entity_support".equalsIgnoreCase(rs.getString("name"))) return;
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE ddoor_doors ADD COLUMN entity_support INTEGER NOT NULL DEFAULT 0");
        }
    }

    @Override
    public List<DoorRecord> loadAll() throws Exception {
        List<DoorRecord> out = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM ddoor_doors")) {
            while (rs.next()) {
                out.add(fromRow(rs));
            }
        }
        return out;
    }

    @Override
    public void upsert(DoorRecord door) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO ddoor_doors (id,name,owner,world,x,y,z,facing,paired_id,created_at,uses,enabled,entity_support) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            bind(ps, door);
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
                "INSERT OR REPLACE INTO ddoor_player_settings (uuid,mode,simple_info) VALUES (?,?,?)")) {
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
            bindLog(ps, log);
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
                    out.add(fromLogRow(rs));
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

    static void bind(PreparedStatement ps, DoorRecord d) throws SQLException {
        ps.setString(1, d.id().toString());
        ps.setString(2, d.name());
        ps.setString(3, d.owner().toString());
        ps.setString(4, d.world());
        ps.setInt(5, d.x());
        ps.setInt(6, d.y());
        ps.setInt(7, d.z());
        ps.setString(8, d.facing().name());
        ps.setString(9, d.pairedId() == null ? null : d.pairedId().toString());
        ps.setLong(10, d.createdAt());
        ps.setLong(11, d.uses());
        ps.setInt(12, d.enabled() ? 1 : 0);
        ps.setInt(13, d.entities() ? 1 : 0);
    }

    static DoorRecord fromRow(ResultSet rs) throws SQLException {
        String paired = rs.getString("paired_id");
        boolean enabled = true;
        try {
            enabled = rs.getInt("enabled") != 0;
        } catch (SQLException legacy) {
            // pre-1.0.5 rows before migration completes — enabled defaults to true
        }
        boolean entities = false;
        try {
            entities = rs.getInt("entity_support") != 0;
        } catch (SQLException legacy) {
            // pre-1.0.7 rows before migration completes — entity support defaults to false
        }
        return new DoorRecord(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                UUID.fromString(rs.getString("owner")),
                rs.getString("world"),
                rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                BlockFace.valueOf(rs.getString("facing")),
                paired == null ? null : UUID.fromString(paired),
                rs.getLong("created_at"),
                rs.getLong("uses"),
                enabled,
                entities);
    }

    static void bindLog(PreparedStatement ps, DoorLog l) throws SQLException {
        ps.setString(1, l.doorId().toString());
        ps.setString(2, l.doorName());
        ps.setString(3, l.world());
        ps.setInt(4, l.x());
        ps.setInt(5, l.y());
        ps.setInt(6, l.z());
        ps.setString(7, l.playerName());
        ps.setString(8, l.action());
        ps.setLong(9, l.time());
    }

    static DoorLog fromLogRow(ResultSet rs) throws SQLException {
        return new DoorLog(
                UUID.fromString(rs.getString("door_id")),
                rs.getString("door_name"),
                rs.getString("world"),
                rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                rs.getString("player_name"),
                rs.getString("action"),
                rs.getLong("time"));
    }
}
