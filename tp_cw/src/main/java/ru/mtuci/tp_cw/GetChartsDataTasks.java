package ru.mtuci.tp_cw;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.*;
import javafx.concurrent.Task;
import javafx.scene.chart.XYChart.*;

public class GetChartsDataTasks extends Task<ChartData> {
    ArrayList<LogEntry> allEntries;
    ArrayList<Integer> allLanes;
    int lIndex, steps;
    String xName, yName;

    GetChartsDataTasks(ArrayList<LogEntry> entries, String chartType,
        HashSet<Integer> lanes, int laneIndex, int chartSteps) throws Exception {
        allEntries = entries;
        allLanes = new ArrayList<Integer>(lanes);
        lIndex = laneIndex;
        steps = chartSteps;
        var m = Pattern.compile("(.*)\\((.*)\\)").matcher(chartType);
        if (!m.find())
            throw new Exception("Can't get chart type");
        xName = m.group(1);
        yName = m.group(2);
    }

    @Override
    protected ChartData call() throws Exception {
        ArrayList<Integer> lanesForApprox;
        if (lIndex == -1) lanesForApprox = allLanes;
        else {
            lanesForApprox = new ArrayList<Integer>();
            lanesForApprox.add(allLanes.get(lIndex));
        }
        var approxFuncs = getApproxFuncs(lanesForApprox);
        updateMessage("Generating serieses");
        return new ChartData(getApproxSeries(approxFuncs), approxFuncs);
    }

    private double getDistance(double gap, double speed) {
        return gap * speed;
    }

    private double getDensity(double occupancy) {
        return 1000 * occupancy / 5.7;
    }

    private PolynomialFunction getApproxFunc(int lane) throws Exception {
        var pts = new WeightedObservedPoints();
        var fitt = PolynomialCurveFitter.create(3);
        double x, y;
        for (var entry : allEntries) {
            if (entry.getLaneNumber() != lane) continue;

            if (xName.equals("Volume")) x = entry.getVolume();
            else if (xName.equals("Speed")) x = entry.getSpeed();
            else throw new Exception("Unknown x field");

            if (yName.equals("Density")) y = getDensity(entry.getOccupancy());
            else if (yName.equals("Distance")) y = getDistance(entry.getGap(), entry.getSpeed());
            else throw new Exception("Unknown y field");

            pts.add(x, y);
        }
        return new PolynomialFunction(fitt.fit(pts.toList()));
    }

    private HashMap<Integer, PolynomialFunction> getApproxFuncs(ArrayList<Integer> lanes)
        throws Exception {
        var funcs = new HashMap<Integer, PolynomialFunction>();
        int totalLanes = lanes.size(), processed = 0;
        updateProgress(0, totalLanes);
        for (var lane : lanes) {
            updateMessage("Approximation of lane " + lane);
            funcs.put(lane, getApproxFunc(lane));
            updateProgress(++processed, totalLanes);
        }
        return funcs;
    }

    private ArrayList<Series<Number, Number>> getApproxSeries(HashMap<Integer, PolynomialFunction> approxFuncs)
        throws Exception {
        var series = new ArrayList<Series<Number, Number>>();
        Series<Number, Number> ser;
        Number[] x;
        if (xName.equals("Volume")) {
            var volumes = new ArrayList<Integer>(
                allEntries.stream().map(LogEntry::getVolume).collect(Collectors.toList())
            );
            x = getNValues(volumes, steps).toArray(new Number[0]);
        }
        else if (xName.equals("Speed")) {
            var speeds = new ArrayList<Number>(
                allEntries.stream().map(LogEntry::getSpeed).collect(Collectors.toList())
            );
            x = getNValues(speeds, steps).toArray(new Number[0]);
        }
        else throw new Exception("Unknown x field");

        for (var funcEntry : approxFuncs.entrySet()) {
            ser = new Series<Number, Number>();
            ser.setName("Lane " + funcEntry.getKey());
            for (int i = 0; i < x.length; i++)
                ser.getData().add(new Data<Number, Number>(
                    x[i],
                    funcEntry.getValue().value(x[i].doubleValue())
                ));
            series.add(ser);
        }

        return series;
    }

    private <T> ArrayList<T> getNValues(ArrayList<T> vals, int n) {
        var result = new ArrayList<T>();
        double step = ((double)vals.size() - 1) / ((double)n - 1);
        for (int i = 0; i < n; i++)
            result.add(vals.get((int)Math.round(step * i)));
        return result;
    }
}
