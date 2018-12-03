package ru.mtuci.tp_cw;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;

public class TrafficLogsParser {
    private Pattern linePattern;

    public TrafficLogsParser() {
        String f = "(\\d+\\,\\d+)", i = "(\\d+)", s = "\\s+", laneNumber = "LANE_" + i;
        String dashOrInt = "(-|\\d+)", datetime = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})";

        String[] full = {"^\\ufeff?\\s*", laneNumber, s, i, s, f, s, f, s, f, s,
            dashOrInt, s, dashOrInt, s, dashOrInt, s, dashOrInt, s,
            dashOrInt, s, dashOrInt, s, dashOrInt, s, dashOrInt, s,
            f, s, f, s, datetime, ".+$"};
        linePattern = Pattern.compile(String.join("", full));
    }

    private LogEntry parseEntry(String line) {
        var matcher = linePattern.matcher(line);
        if (!matcher.find()) return null;
        try {
            var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var doubleFormat = NumberFormat.getInstance(Locale.FRANCE);
            var ln = Integer.parseInt(matcher.group(1));
            var vol = Integer.parseInt(matcher.group(2));
            var occ = doubleFormat.parse(matcher.group(3)).doubleValue();
            var speed = doubleFormat.parse(matcher.group(4)).doubleValue() * 5 / 18;
            var gap = doubleFormat.parse(matcher.group(15)).doubleValue();
            var datetime = dateFormat.parse(matcher.group(16));
            return new LogEntry(ln, datetime, speed, gap, vol, occ);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public ArrayList<LogEntry> parseFile(String filePath) {
        var res = new ArrayList<LogEntry>();
        LogEntry parsed;
        try (var br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) continue;
                parsed = parseEntry(line);
                if (parsed != null) res.add(parsed);
            }
            return res;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public ArrayList<LogEntry> parseFilesParallel(String[] filePaths) throws InterruptedException, ExecutionException {
        List<Callable<ArrayList<LogEntry>>> tasks = new ArrayList<Callable<ArrayList<LogEntry>>>();
        for (var path : filePaths) tasks.add(() -> parseFile(path));

        var exec = Executors.newCachedThreadPool();
        try {
            List<Future<ArrayList<LogEntry>>> futures = exec.invokeAll(tasks);
            var res = new ArrayList<LogEntry>();
            ArrayList<LogEntry> fRes;
            for (var f : futures) {
                fRes = f.get();
                if (fRes != null) res.addAll(fRes);
            }
            return res;
        } finally {
            exec.shutdown();
        }
    }

    public ArrayList<LogEntry> parseFiles(String[] filePaths) {
        var res = new ArrayList<LogEntry>();
        ArrayList<LogEntry> pRes;
        for (var path : filePaths) {
            pRes = parseFile(path);
            if (pRes != null) res.addAll(pRes);
        }
        return res;
    }
}
