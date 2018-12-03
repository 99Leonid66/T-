package ru.mtuci.tp_cw;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import javafx.scene.chart.XYChart.Series;

public class ChartData {
    ArrayList<Series<Number, Number>> series;
    HashMap<Integer, PolynomialFunction> polinomials;
    ChartData(ArrayList<Series<Number, Number>> sers, HashMap<Integer, PolynomialFunction> polinoms) {
        series = sers;
        polinomials = polinoms;
    }
}
