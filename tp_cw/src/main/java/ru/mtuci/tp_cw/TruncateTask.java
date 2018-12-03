package ru.mtuci.tp_cw;

import java.sql.SQLException;
import javafx.concurrent.Task;

public class TruncateTask extends Task<Void> {

    private DbHandler dbHandler;

    TruncateTask(String dbPath) throws Exception {
        try {
            dbHandler = new DbHandler(dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new Exception("Can't open database");
        }
    }

    @Override
    protected Void call() throws Exception {
        try {
            updateMessage("Truncating DB");
            dbHandler.truncate();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("DB Error during truncating");
        }
    }
}
