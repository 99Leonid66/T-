package ru.mtuci.tp_cw;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import javafx.concurrent.Task;

public class AddFromLogsTask extends Task<Integer> {

    private DbHandler dbHandler;
    private TrafficLogsParser parser;

    AddFromLogsTask(String dbPath) throws Exception {
        try {
            dbHandler = new DbHandler(dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new Exception("Can't open database");
        }
        parser = new TrafficLogsParser();
    }

    @Override
    protected Integer call() throws Exception {
        updateMessage("Getting files from user");
        var paths = getFilesPaths();
        if (paths == null) return -1;

        ArrayList<LogEntry> entries;
        String path;
        int addedTotal = 0;

        for (int i = 0; i < paths.size(); i++) {
            path = paths.get(i);
            updateMessage("Parsing file " + path);
            entries = parser.parseFile(path);
            if (entries == null || entries.size() == 0) {
                updateProgress(i+1, paths.size());
                continue;
            }
            try {
                updateMessage("Adding " + entries.size() + " entries from " + path);
                addedTotal += dbHandler.addEntries(entries);
                updateProgress(i+1, paths.size());
            } catch (SQLException e) {
                e.printStackTrace();
                throw new Exception("Error writing entries of " + path + " to DB");
			}
        }
        return addedTotal;
    }

    private ArrayList<String> getFilesPaths() throws Exception {
        var chooser = new JFileChooser();
        var filter = new FileNameExtensionFilter("Log files", "log", "txt");
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(true);
        var workingDirectory = new File(System.getProperty("user.dir"));
        chooser.setCurrentDirectory(workingDirectory);
        var result = chooser.showOpenDialog(null);
        switch (result) {
            case JFileChooser.APPROVE_OPTION:
                var files = chooser.getSelectedFiles();
                var res = new ArrayList<String>();
                for (var file : files) res.add(file.getCanonicalPath());
                return res;
            case JFileChooser.CANCEL_OPTION:
                return null;
            default:
                throw new Exception("Error during getting files occured");
        }
    }

}
