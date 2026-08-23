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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
              uses BIGINT NOT NULL DEFAULT 0
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
                "INSERT OR REPLACE INTO ddoor_doors (id,name,owner,world,x,y,z,facing,paired_id,created_at,uses) VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
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
    }

    static DoorRecord fromRow(ResultSet rs) throws SQLException {
        String paired = rs.getString("paired_id");
        return new DoorRecord(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                UUID.fromString(rs.getString("owner")),
                rs.getString("world"),
                rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                BlockFace.valueOf(rs.getString("facing")),
                paired == null ? null : UUID.fromString(paired),
                rs.getLong("created_at"),
                rs.getLong("uses"));
    }
}
