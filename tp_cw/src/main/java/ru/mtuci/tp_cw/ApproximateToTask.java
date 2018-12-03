package ru.mtuci.tp_cw;

import javafx.concurrent.Task;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.*;

public class ApproximateToTask extends Task<Void> {

    private DbHandler dbHandler;
    private int approximateNumber;
    private HashMap<Integer, PolynomialFunction> speedApprFuncs, gapApprFuncs,
        volumeApprFuncs, occupancyApprFuncs;
    private int processed;
    private HashSet<Integer> lanes;

    ApproximateToTask(String dbPath, int apprNumb, HashSet<Integer> laneNumbers) throws Exception {
        try {
            dbHandler = new DbHandler(dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new Exception("Can't open database");
        }
        approximateNumber = apprNumb;
        lanes = laneNumbers;
    }

    @Override
    protected Void call() throws Exception {
        ArrayList<LogEntry> allEntries;
        try {
            allEntries = dbHandler.getAll();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Can't load all entries");
        }
        updateMessage("Generating new timestamps");
        var count = approximateNumber - allEntries.size();
        var countForLane = count / lanes.size();
        var dates = getNewDates(findLastDate(allEntries), countForLane);
        updateMessage("Approximation of multiple columns in parallel");
        generateAllApproxFuncsInParallel(allEntries, lanes);
        var newEntries = new ArrayList<LogEntry>();
        int generated = 0, total = dates.size() * lanes.size();
        updateProgress(generated, total);
        updateMessage("Generating new entries");
        for (var time : dates) {
            for (var lane : lanes) {
                newEntries.add(new LogEntry(
                    lane,
                    time,
                    speedApprFuncs.get(lane).value((double)time.getTime()),
                    gapApprFuncs.get(lane).value((double)time.getTime()),
                    (int)volumeApprFuncs.get(lane).value((double)time.getTime()),
                    occupancyApprFuncs.get(lane).value((double)time.getTime())
                ));
                updateProgress(++generated, total);
            }
        }
        updateMessage("Adding new entries to DB");
        final var entriesToProcess = newEntries.size();
        processed = 0;
        dbHandler.addEntries(newEntries, (b, i) -> {
            processed += b;
            updateProgress(processed, entriesToProcess);
        });
        return null;
    }

    private void generateAllApproxFuncsInParallel(ArrayList<LogEntry> entries, HashSet<Integer> lanes)
        throws InterruptedException, ExecutionException {
        var executor = Executors.newFixedThreadPool(4);
        var tasks = new ArrayList<Callable<HashMap<Integer, PolynomialFunction>>>();
        tasks.add(() -> speedApprFuncs = generateApprFuncs(entries, lanes, "getSpeed"));
        tasks.add(() -> gapApprFuncs = generateApprFuncs(entries, lanes, "getGap"));
        tasks.add(() -> volumeApprFuncs = generateApprFuncs(entries, lanes, "getVolume"));
        tasks.add(() -> occupancyApprFuncs = generateApprFuncs(entries, lanes, "getOccupancy"));
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private Date findLastDate(ArrayList<LogEntry> entries) {
        return Collections.max(entries, LogEntry.logDatetimeComparator).getDatetime();
    }

    private ArrayList<Date> getNewDates(Date from, int count) {
        var dates = new ArrayList<Date>();
        var cal = Calendar.getInstance();
        for (int i = 1; i <= count; i++) {
            cal.setTime(from);
            cal.add(Calendar.MINUTE, i);
            dates.add(cal.getTime());
        }
        return dates;
    }

    private <T extends Number> PolynomialFunction getApprFunc(ArrayList<Long> x, ArrayList<T> y) {
        var pts = new WeightedObservedPoints();
        var fitt = PolynomialCurveFitter.create(3);
        for (int i = 0; i < x.size(); i++)
            pts.add(x.get(i).doubleValue(), y.get(i).doubleValue());
        return new PolynomialFunction(fitt.fit(pts.toList()));
    }

    private HashMap<Integer, PolynomialFunction> generateApprFuncs(ArrayList<LogEntry> entries,
            HashSet<Integer> lanes, String getterName)
            throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        var uniqueDates = new HashSet<Long>();
        for (var entry : entries) uniqueDates.add(entry.getDatetime().getTime());
        var dateList = new ArrayList<Long>(uniqueDates);
        Collections.sort(dateList);
        var valsByLanes = new HashMap<Integer, Number[]>();
        for (var lane : lanes) valsByLanes.put(lane, new Number[entries.size()]);
        var getter = Class.forName("ru.mtuci.tp_cw.LogEntry").getDeclaredMethod(getterName);
        for (var entry : entries) {
            var laneVals = valsByLanes.get(entry.getLaneNumber());
            laneVals[dateList.indexOf(entry.getDatetime().getTime())] = (Number)getter.invoke(entry);
        }
        var approxFuncs = new HashMap<Integer, PolynomialFunction>();
        for (var laneEntry : valsByLanes.entrySet()) {
            var valsList = new ArrayList<>(Arrays.asList(valsByLanes.get(laneEntry.getKey())));
            valsList.trimToSize();
            approxFuncs.put(laneEntry.getKey(), getApprFunc(dateList, valsList));
        }
        return approxFuncs;
    }
}
