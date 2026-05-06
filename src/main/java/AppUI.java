import Models.*;
import ACO.ACOEngine;
import Simulation.EngagementSimulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.ScaleTransition;
import javafx.animation.Animation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class AppUI extends Application {

    private TextArea outputArea;
    private TextArea scheduleArea;
    private XYChart.Series<Number, Number> fitnessSeries;
    private XYChart.Series<Number, Number> averageFitnessSeries;
    private Button runBtn;
    private Label statusLabel;
    private ScaleTransition pulse;
    
    // Data setup
    private List<Post> posts;
    private List<TimeSlot> slots;

    @Override
    public void start(Stage primaryStage) {
        setupData();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        
        // --- HEADER ---
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 0 0 20 0;");
        
        Label title = new Label("ACO Optimizer ⚡");
        title.getStyleClass().add("label-title");
        
        runBtn = new Button("Run ACO 🔄");
        runBtn.setOnAction(e -> runACO());
        
        pulse = new ScaleTransition(Duration.seconds(1), runBtn);
        pulse.setFromX(1);
        pulse.setToX(1.05);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
        
        statusLabel = new Label("Status: READY");
        statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(title, runBtn, spacer, statusLabel);
        root.setTop(header);
        
        // --- CENTER: GRAPH ---
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Iteration");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(200);
        xAxis.setTickUnit(20);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fitness Score");
        yAxis.setAutoRanging(true);
        
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Live Optimization Analytics 📈");
        lineChart.setAnimated(false); // We handle animation manually
        lineChart.getStyleClass().add("chart");
        
        fitnessSeries = new XYChart.Series<>();
        fitnessSeries.setName("Best Engagement");
        
        averageFitnessSeries = new XYChart.Series<>();
        averageFitnessSeries.setName("Average Engagement");
        
        lineChart.getData().addAll(fitnessSeries, averageFitnessSeries);
        
        VBox chartContainer = new VBox(lineChart);
        chartContainer.getStyleClass().add("card");
        VBox.setVgrow(lineChart, Priority.ALWAYS);
        root.setCenter(chartContainer);
        BorderPane.setMargin(chartContainer, new Insets(0, 0, 20, 0));
        
        // --- BOTTOM: OUTPUT & SCHEDULE ---
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(150);
        outputArea.getStyleClass().add("text-area");
        VBox.setVgrow(outputArea, Priority.ALWAYS);
        
        VBox logsContainer = new VBox(10);
        logsContainer.getStyleClass().add("card");
        Label outputTitle = new Label("Execution Logs 🧾");
        outputTitle.getStyleClass().add("label-subtitle");
        logsContainer.getChildren().addAll(outputTitle, outputArea);
        
        scheduleArea = new TextArea();
        scheduleArea.setEditable(false);
        scheduleArea.setPrefHeight(150);
        scheduleArea.getStyleClass().add("text-area");
        VBox.setVgrow(scheduleArea, Priority.ALWAYS);
        
        VBox scheduleContainer = new VBox(10);
        scheduleContainer.getStyleClass().add("card");
        Label scheduleTitle = new Label("Best Schedule 🏆");
        scheduleTitle.getStyleClass().add("label-subtitle");
        scheduleContainer.getChildren().addAll(scheduleTitle, scheduleArea);
        
        HBox bottomPanel = new HBox(20);
        HBox.setHgrow(logsContainer, Priority.ALWAYS);
        HBox.setHgrow(scheduleContainer, Priority.ALWAYS);
        logsContainer.setMaxWidth(Double.MAX_VALUE);
        scheduleContainer.setMaxWidth(Double.MAX_VALUE);
        bottomPanel.getChildren().addAll(logsContainer, scheduleContainer);
        
        root.setBottom(bottomPanel);
        
        // --- SCENE ---
        Scene scene = new Scene(root, 950, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        
        primaryStage.setTitle("ACO Social Media Optimizer");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        log("[SYSTEM] Ready. Click 'Run ACO' to begin optimization.");
    }
    
    private void setupData() {
        posts = new ArrayList<>();
        posts.add(new Post(1, ContentType.REEL));
        posts.add(new Post(2, ContentType.MEME));
        posts.add(new Post(3, ContentType.EDUCATIONAL));
        posts.add(new Post(4, ContentType.REEL));
        posts.add(new Post(5, ContentType.MEME));
        posts.add(new Post(6, ContentType.EDUCATIONAL));
        posts.add(new Post(7, ContentType.REEL));
        posts.add(new Post(8, ContentType.MEME));

        slots = new ArrayList<>();
        slots.add(new TimeSlot(1, "Morning (9 AM)"));
        slots.add(new TimeSlot(2, "Afternoon (1 PM)"));
        slots.add(new TimeSlot(3, "Evening (6 PM)"));
        slots.add(new TimeSlot(4, "Night (9 PM)"));
    }

    private void runACO() {
        runBtn.setDisable(true);
        pulse.stop();
        runBtn.setScaleX(1);
        runBtn.setScaleY(1);
        
        statusLabel.setText("Status: RUNNING...");
        statusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        fitnessSeries.getData().clear();
        averageFitnessSeries.getData().clear();
        outputArea.clear();
        scheduleArea.clear();
        
        // Baseline: Random Scheduling
        Schedule randomSchedule = new Schedule();
        java.util.Random rand = new java.util.Random();
        for (Post post : posts) {
            randomSchedule.assign(post, slots.get(rand.nextInt(slots.size())));
        }
        randomSchedule.calculateFitness();
        log("--- BASELINE (RANDOM SCHEDULING) ---");
        log(randomSchedule.toStringSchedule());
        log("\n--- Running ACO ---\n");
        
        // Reset trends for fresh run
        EngagementSimulator.resetTrends();

        Thread acoThread = new Thread(() -> {
            ACOEngine engine = new ACOEngine(posts, slots);
            
            Schedule best = engine.runUI(
                (iteration, bestF, avgF) -> {
                    Platform.runLater(() -> {
                        fitnessSeries.getData().add(new XYChart.Data<>(iteration, bestF));
                        averageFitnessSeries.getData().add(new XYChart.Data<>(iteration, avgF));
                    });
                },
                (phaseMsg) -> {
                    Platform.runLater(() -> {
                        log(phaseMsg);
                        if(phaseMsg.contains("SHIFT")) {
                            statusLabel.setText("Status: ADAPTING ⚠️");
                            statusLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-size: 16px;");
                        } else if(phaseMsg.contains("Convergence")) {
                            statusLabel.setText("Status: CONVERGED");
                            statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 16px;");
                        }
                    });
                }
            );
            
            Platform.runLater(() -> {
                log("\n[SUCCESS] --- FINAL BEST SCHEDULE ---");
                scheduleArea.setText(best.toStringSchedule());
                statusLabel.setText("Status: COMPLETE ✨");
                statusLabel.setStyle("-fx-text-fill: #a855f7; -fx-font-weight: bold; -fx-font-size: 16px;");
                runBtn.setDisable(false);
                runBtn.setText("Run Again 🔄");
                pulse.play();
            });
        });
        
        acoThread.setDaemon(true);
        acoThread.start();
    }
    
    private void log(String msg) {
        outputArea.appendText(msg + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
