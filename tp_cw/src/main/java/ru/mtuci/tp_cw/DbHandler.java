package ru.mtuci.tp_cw;

import java.sql.*;
import java.util.ArrayList;

public final class DbHandler {
    private String connString;

    public DbHandler(String path) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connString = "jdbc:sqlite:" + path;
        var conn = DriverManager.getConnection(connString);
        var st = conn.createStatement();
        st.execute("CREATE TABLE IF NOT EXISTS 'log_entries'" +
            "('lane' INTEGER, 'gap' REAL, 'speed' REAL, 'datetime' INTEGER," +
            "'volume' INTEGER, 'occupancy' REAL," +
            "PRIMARY KEY ('lane', 'datetime'));");
        conn.close();
    }

    public interface AddEntriesCB {
        public void afterBatchInsert(int batchSize, int inserted);
    }

    public int addEntries(ArrayList<LogEntry> entries, AddEntriesCB batchCb) throws SQLException {
        var conn = DriverManager.getConnection(connString);
        conn.setAutoCommit(false);
        var insertSt = conn.prepareStatement("INSERT OR IGNORE INTO 'log_entries'" +
            "('lane', 'gap', 'speed', 'datetime', 'volume', 'occupancy') VALUES (?,?,?,?,?,?)");
        final int batchSize = 10000;
        int count = 0, inserted = 0, insertedTotal = 0;
        for (var entry : entries) {
            addEntryToBatch(entry, insertSt);
            if (++count % batchSize == 0) {
                inserted = getNumberOfSuccessInBatch(insertSt.executeBatch());
                insertedTotal += inserted;
                conn.commit();
                batchCb.afterBatchInsert(batchSize, inserted);
            }
        }
        inserted = getNumberOfSuccessInBatch(insertSt.executeBatch());
        insertedTotal += inserted;
        conn.commit();
        batchCb.afterBatchInsert(batchSize, inserted);
        insertSt.close();
        conn.close();
        return insertedTotal;
    }

    public int addEntries(ArrayList<LogEntry> entries) throws SQLException {
        return addEntries(entries, (b, i) -> {});
    }

    private int getNumberOfSuccessInBatch(int[] results) {
        int success = 0;
        for (var res : results)
            if (res > 0 || res == Statement.SUCCESS_NO_INFO) success++;
        return success;
    }

    public interface LoadEntriesCB {
        public void afterEntryLoaded(LogEntry entry);
    }

    public ArrayList<LogEntry> getAll(LoadEntriesCB rowCb) throws SQLException {
        var conn = DriverManager.getConnection(connString);
        var selectAllSt = conn.prepareStatement("SELECT * FROM 'log_entries';");
        selectAllSt.setFetchSize(1000);
        var rs = selectAllSt.executeQuery();
        var res = new ArrayList<LogEntry>();
        LogEntry ent;
        while(rs.next()) {
            ent = readEntryFromRs(rs);
            res.add(ent);
            rowCb.afterEntryLoaded(ent);
        }
        conn.close();
        return res;
    }

    public ArrayList<LogEntry> getAll() throws SQLException {
        return getAll(e -> {});
    }

    public int getAllCount() throws SQLException {
        var conn = DriverManager.getConnection(connString);
        var allCountSt = conn.prepareStatement("SELECT COUNT(*) AS count FROM 'log_entries';");
        var rs = allCountSt.executeQuery();
        rs.next();
        var res = rs.getInt("count");
        conn.close();
        return res;
    }

    public void truncate() throws SQLException {
        var conn = DriverManager.getConnection(connString);
        var truncateSt = conn.prepareStatement("DELETE FROM 'log_entries';");
        truncateSt.execute();
        conn.close();
    }

    private LogEntry readEntryFromRs(ResultSet rs) throws SQLException {
        var lane = rs.getInt("lane");
        var gap = rs.getDouble("gap");
        var speed = rs.getDouble("speed");
        var datetime = new Date(rs.getLong("datetime"));
        var volume = rs.getInt("volume");
        var occupancy = rs.getDouble("occupancy");
        return new LogEntry(lane, datetime, speed, gap, volume, occupancy);
    }

    private void addEntryToBatch(LogEntry e, PreparedStatement ps) throws SQLException {
        ps.setInt(1, e.getLaneNumber());
        ps.setDouble(2, e.getGap());
        ps.setDouble(3, e.getSpeed());
        ps.setLong(4, e.getDatetime().getTime());
        ps.setInt(5, e.getVolume());
        ps.setDouble(6, e.getOccupancy());
        ps.addBatch();
    }
}
