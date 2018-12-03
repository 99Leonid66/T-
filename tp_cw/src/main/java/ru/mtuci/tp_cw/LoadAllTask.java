package ru.mtuci.tp_cw;

import java.sql.SQLException;
import java.util.ArrayList;
import javafx.concurrent.Task;

public class LoadAllTask extends Task<ArrayList<LogEntry>> {

    private DbHandler dbHandler;
    private int added, total;

    LoadAllTask(String dbPath) throws Exception {
        try {
            dbHandler = new DbHandler(dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new Exception("Can't open database");
        }
    }

    @Override
    protected ArrayList<LogEntry> call() throws Exception {
        try {
            updateMessage("Loading all entries from DB");
            added = 0;
            total = dbHandler.getAllCount();
            updateProgress(added, total);
            return dbHandler.getAll(e -> updateProgress(++added, total));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Can't load all entries");
        }
    }
}
