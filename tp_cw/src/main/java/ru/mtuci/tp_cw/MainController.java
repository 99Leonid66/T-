package ru.mtuci.tp_cw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

public class MainController {
    @FXML private Label activeChartTitleLabel;
    @FXML private Label entriesNumberLabel;
    @FXML private TextField approximateToField;
    @FXML private ChoiceBox<String> chartTypeChoiceBox;
    @FXML private ChoiceBox<String> laneChoiceBox;
    @FXML private Label analysisTextOutputLabel;
    @FXML private Label currentTaskLabel;
    @FXML private ProgressBar progressBar;
    @FXML private LineChart<Number, Number> chart;

    private static final String DB_PATH = "entries.sqlite3";
    private static int  CHART_STEPS = 30;
    protected ArrayList<LogEntry> entries = new ArrayList<>();
    private boolean taskInProgress;
    private HashSet<Integer> lanes;

    public void initialize() {
        approximateToField.setTextFormatter(new TextFormatter<>(ch -> ch.getText().matches("[0-9]*") ? ch : null));
        loadAllEntries();
    }

    @FXML
    private void onAddFromLogs() {
        if (taskInProgress) return;
        try {
            var task = new AddFromLogsTask(DB_PATH);
            runTask(task,
                e -> {
                    if (task.getValue() == 0) showAlert(
                        AlertType.WARNING,
                        "No records have been added",
                        "Log files do not contain valid records or " +
                            "all records have been added to the database early"
                    );
                    else if (task.getValue() != -1) loadAllEntries();
                },
                e -> showAlert("Error Adding Entries", task.getException().getMessage())
            );
        } catch (Exception e) {
            showAlert("Error during task initialization", "Possibly can't open DB");
        }
    }

    @FXML
    void onApproximateTo() {
        if (taskInProgress) return;
        if (approximateToField.getText().length() == 0) {
            showAlert("Invalid 'approximate to' number", "You have not entered any number");
            return;
        }
        var approxNumb = Integer.parseInt(approximateToField.getText());
        if (entries.size() == 0) {
            showAlert("Can't approximate", "Nothing to approximate. First add entries from log files");
            return;
        }
        if (approxNumb <= entries.size()) {
            showAlert("Invalid 'approximate to' number", "Please enter a number greater than " +
                "the current number of database entries (" + entries.size() + ")");
            return;
        }
        if ((approxNumb - entries.size()) % lanes.size() != 0) {
            showAlert("Invalid 'approximate to' number", "The provided number of entries " +
                "is not a multiple of the number of lanes (" + lanes.size() + ")");
            return;
        }
        if (entries.size() >= 100_000) {
            var res = showAlert(AlertType.CONFIRMATION, "The number of records is large",
                "The number of records is large, their processing can take a long time. " +
                "Are you sure you want to continue?");
            if (!res) return;
        }
        try {
            var task = new ApproximateToTask(DB_PATH, approxNumb, lanes);
            runTask(task,
                e -> loadAllEntries(),
                e -> showAlert("Error Generating Entries", task.getException().getMessage())
            );
        } catch (Exception e) {
            showAlert("Error during task initialization", "Possibly can't open DB");
        }
    }

    @FXML
    void onDraw() {
        if (taskInProgress) return;
        var chartType = chartTypeChoiceBox.getValue();
        var laneIndex = laneChoiceBox.getSelectionModel().getSelectedIndex() - 1;
        try {
            var task = new GetChartsDataTasks(entries, chartType, lanes, laneIndex, CHART_STEPS);
            runTask(task,
                e -> {
                    drawCharts(chartType, task.getValue().series);
                    var polinomsCoeffs = new ArrayList<double[]>();
                    for (var funcEntry : task.getValue().polinomials.entrySet())
                        polinomsCoeffs.add(funcEntry.getValue().getCoefficients());
                    showApproximationPolynomials(polinomsCoeffs);
                },
                e -> {
                    task.getException().printStackTrace();
                    showAlert("Error Drawing Chart", task.getException().getMessage());
                }
            );
        } catch (Exception e) {
            showAlert("Error during task initialization", e.getMessage());
        }
    }

    @FXML
    void onChartExport() {
        if (taskInProgress) return;
    }

    @FXML
    void onClear() {
        if (taskInProgress) return;
        try {
            entriesNumberLabel.setText("...");
            var task = new TruncateTask(DB_PATH);
            runTask(task,
                e -> {
                    entries.clear();
                    entriesNumberLabel.setText("0");
                },
                e -> {
                    entriesNumberLabel.setText(Integer.toString(entries.size()));
                    showAlert("Error Truncating Table", task.getException().getMessage());
                }
            );
        } catch (Exception e) {
            showAlert("Error during task initialization", "Possibly can't open DB");
        }
    }

    private void loadAllEntries() {
        if (taskInProgress) return;
        try {
            entriesNumberLabel.setText("...");
            var task = new LoadAllTask(DB_PATH);
            runTask(task,
                e -> {
                    entries = task.getValue();
                    entriesNumberLabel.setText(Integer.toString(entries.size()));
                    lanes = findLanes(entries);
                    laneChoiceBox.getItems().clear();
                    if (lanes.size() == 0) return;
                    laneChoiceBox.getItems().add("All");
                    for (var laneNumber : lanes)
                        laneChoiceBox.getItems().add("Lane " + laneNumber);
                    laneChoiceBox.setValue("All");
                },
                e -> {
                    entriesNumberLabel.setText(Integer.toString(entries.size()));
                    showAlert("Error Loading Entries", task.getException().getMessage());
                }
            );
        } catch (Exception e) {
            showAlert("Error during task initialization", "Possibly can't open DB");
        }
    }

    private HashSet<Integer> findLanes(ArrayList<LogEntry> entries) {
        var lanes = new HashSet<Integer>();
        for (var entry : entries) lanes.add(entry.getLaneNumber());
        return lanes;
    }

    private <T> void runTask(Task<T> task, EventHandler<WorkerStateEvent> onSuccess, EventHandler<WorkerStateEvent> onFail) {
        Runnable after = () -> {
            progressBar.progressProperty().unbind();
            currentTaskLabel.textProperty().unbind();
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            progressBar.setVisible(false);
            currentTaskLabel.setText("None");
            taskInProgress = false;
        };
        task.setOnSucceeded(e -> {
            after.run();
            onSuccess.handle(e);
        });
        task.setOnFailed(e -> {
            after.run();
            onFail.handle(e);
        });
        progressBar.setVisible(true);
        progressBar.progressProperty().bind(task.progressProperty());
        currentTaskLabel.textProperty().bind(task.messageProperty());
        taskInProgress = true;
        var th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private boolean showAlert(AlertType type, String header, String msg) {
        var a = new Alert(type, msg);
        a.setHeaderText(header);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        var resBtn = a.showAndWait();
        return resBtn.get() == ButtonType.OK;
    }

    private void showAlert(String header, String msg) {
        showAlert(AlertType.ERROR, header, msg);
    }

    private void showApproximationPolynomials(ArrayList<double[]> polinomials) {
        String[] powersMap = { "", "x", "x²", "x³", "x⁴", "x⁵", "x⁶", "x⁷", "x⁸", "x⁹" };
        var polinomialsStrs = new ArrayList<String>();
        for (var coeffs : polinomials) {
            var coeffsStr = new ArrayList<String>();
            for (int i = 0; i < coeffs.length; i++)
                coeffsStr.add(String.format("%.2f", coeffs[i]) + powersMap[i]);
            Collections.reverse(coeffsStr);
            polinomialsStrs.add("y = " + String.join(" + ", coeffsStr));
        }
        analysisTextOutputLabel.setText(String.join(",\n", polinomialsStrs));
    }

    private void drawCharts(String title, ArrayList<Series<Number, Number>> series) {
        chart.getData().clear();
        for (var ser : series) chart.getData().add(ser);
        activeChartTitleLabel.setText(title);
    }
}
