import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ACO.ACOEngine;
import Models.ContentType;
import Models.Post;
import Models.Schedule;
import Models.TimeSlot;
import Simulation.EngagementSimulator;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AppUI extends Application {

    private TextArea outputArea;
    private TableView<ScheduleEntry> scheduleTable;
    private XYChart.Series<Number, Number> fitnessSeries;
    private XYChart.Series<Number, Number> averageFitnessSeries;
    private Button runBtn;
    private Label statusLabel;
    private Label bestFitnessLabel;
    private Label iterationLabel;
    private ScaleTransition pulse;
    
    // Configuration fields
    private TextField antsField, alphaField, betaField, iterationsField;
    private TextArea postsArea, slotsArea, pheromoneArea;

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

        VBox metricsBox = createMetricsDashboard();
        
        header.getChildren().addAll(title, runBtn, spacer, metricsBox, statusLabel);
        root.setTop(header);

        // --- TAB PANE ---
        javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
        tabPane.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // TAB 1: Live Analytics
        javafx.scene.control.Tab analyticsTab = new javafx.scene.control.Tab("Live Analytics 📈");
        VBox analyticsBox = new VBox(15);
        analyticsBox.setPadding(new Insets(15));
        
        // Analytics Chart
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
        lineChart.setAnimated(false); 
        lineChart.getStyleClass().add("chart");
        
        fitnessSeries = new XYChart.Series<>();
        fitnessSeries.setName("Best Engagement");
        averageFitnessSeries = new XYChart.Series<>();
        averageFitnessSeries.setName("Average Engagement");
        lineChart.getData().addAll(fitnessSeries, averageFitnessSeries);
        
        VBox chartContainer = new VBox(lineChart);
        chartContainer.getStyleClass().add("card");
        VBox.setVgrow(chartContainer, Priority.ALWAYS);
        
        // Logs
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(200);
        outputArea.getStyleClass().add("text-area");
        
        VBox logsContainer = new VBox(10);
        logsContainer.getStyleClass().add("card");
        Label outputTitle = new Label("Execution Logs 🧾");
        outputTitle.getStyleClass().add("label-subtitle");
        logsContainer.getChildren().addAll(outputTitle, outputArea);
        
        analyticsBox.getChildren().addAll(chartContainer, logsContainer);
        analyticsTab.setContent(analyticsBox);
        
        // TAB 2: Configuration & Data
        javafx.scene.control.Tab configTab = new javafx.scene.control.Tab("Configuration⚙️");
        HBox configBox = new HBox(20);
        configBox.setPadding(new Insets(15));
        
        VBox leftPanel = new VBox(20);
        leftPanel.setPrefWidth(300);
        leftPanel.getStyleClass().add("left-panel");
        
        TitledPane configPane = new TitledPane("Configuration", createConfigGrid());
        configPane.setCollapsible(false);
        leftPanel.getChildren().add(configPane);
        
        HBox dataPanes = createDataDisplay();
        HBox.setHgrow(dataPanes, Priority.ALWAYS);
        
        configBox.getChildren().addAll(leftPanel, dataPanes);
        configTab.setContent(configBox);

        // TAB 3: Results & Pheromones
        javafx.scene.control.Tab resultsTab = new javafx.scene.control.Tab("Results & Matrix 🏆");
        VBox resultsBox = new VBox(20);
        resultsBox.setPadding(new Insets(15));
        
        scheduleTable = createScheduleTable();
        VBox scheduleContainer = new VBox(10);
        scheduleContainer.getStyleClass().add("card");
        Label scheduleTitle = new Label("Best Schedule 🏆");
        scheduleTitle.getStyleClass().add("label-subtitle");
        scheduleContainer.getChildren().addAll(scheduleTitle, scheduleTable);
        VBox.setVgrow(scheduleTable, Priority.ALWAYS);
        VBox.setVgrow(scheduleContainer, Priority.ALWAYS);
        
        TitledPane pheromonePane = new TitledPane("Pheromone Matrix", createPheromoneDisplay());
        pheromonePane.setCollapsible(false);
        VBox.setVgrow(pheromonePane, Priority.ALWAYS);
        
        resultsBox.getChildren().addAll(scheduleContainer, pheromonePane);
        resultsTab.setContent(resultsBox);
        
        tabPane.getTabs().addAll(analyticsTab, configTab, resultsTab);
        root.setCenter(tabPane);

        // --- SCENE ---
        Scene scene = new Scene(root, 1000, 750); // Clean, slightly smaller size since it's tabbed
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        
        primaryStage.setTitle("ACO Social Media Optimizer");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        log("[SYSTEM] Ready. Configure parameters and click 'Run ACO' to begin.");
    }

    private VBox createMetricsDashboard() {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER_RIGHT);
        bestFitnessLabel = new Label("Best Fitness: N/A");
        bestFitnessLabel.getStyleClass().add("label-metric");
        iterationLabel = new Label("Iteration: 0 / 0");
        iterationLabel.getStyleClass().add("label-metric");
        box.getChildren().addAll(bestFitnessLabel, iterationLabel);
        return box;
    }

    private GridPane createConfigGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        antsField = new TextField("10");
        alphaField = new TextField("1.0");
        betaField = new TextField("2.0");
        iterationsField = new TextField("100");

        grid.add(new Label("Ants:"), 0, 0);
        grid.add(antsField, 1, 0);
        grid.add(new Label("Alpha:"), 0, 1);
        grid.add(alphaField, 1, 1);
        grid.add(new Label("Beta:"), 0, 2);
        grid.add(betaField, 1, 2);
        grid.add(new Label("Iterations:"), 0, 3);
        grid.add(iterationsField, 1, 3);
        
        return grid;
    }

    private HBox createDataDisplay() {
        HBox box = new HBox(20);
        
        VBox postsBox = new VBox(5);
        postsArea = new TextArea();
        postsArea.setEditable(false);
        postsArea.getStyleClass().add("text-area");
        VBox.setVgrow(postsArea, Priority.ALWAYS);

        StringBuilder postText = new StringBuilder();
        for (Post post : posts) {
            postText.append("Post ").append(post.getId()).append(": ").append(post.getType()).append("\n");
        }
        postsArea.setText(postText.toString());
        postsBox.getChildren().add(postsArea);
        
        TitledPane postsPane = new TitledPane("Posts Data", postsBox);
        postsPane.setCollapsible(false);
        HBox.setHgrow(postsPane, Priority.ALWAYS);

        VBox slotsBox = new VBox(5);
        slotsArea = new TextArea();
        slotsArea.setEditable(false);
        slotsArea.getStyleClass().add("text-area");
        VBox.setVgrow(slotsArea, Priority.ALWAYS);

        StringBuilder slotText = new StringBuilder();
        for (TimeSlot slot : slots) {
            slotText.append(slot.getName()).append("\n");
        }
        slotsArea.setText(slotText.toString());
        slotsBox.getChildren().add(slotsArea);
        
        TitledPane slotsPane = new TitledPane("Time Slots Data", slotsBox);
        slotsPane.setCollapsible(false);
        HBox.setHgrow(slotsPane, Priority.ALWAYS);

        box.getChildren().addAll(postsPane, slotsPane);
        return box;
    }

    private VBox createPheromoneDisplay() {
        VBox box = new VBox(5);
        pheromoneArea = new TextArea("Pheromone levels will appear here during the run.");
        pheromoneArea.setEditable(false);
        pheromoneArea.setPrefHeight(160); // Adjusted height
        box.getChildren().add(pheromoneArea);
        return box;
    }

    private TableView<ScheduleEntry> createScheduleTable() {
        TableView<ScheduleEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<ScheduleEntry, String> postCol = new TableColumn<>("Post ID");
        postCol.setCellValueFactory(new PropertyValueFactory<>("postId"));
        
        TableColumn<ScheduleEntry, String> typeCol = new TableColumn<>("Content Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("contentType"));

        TableColumn<ScheduleEntry, String> slotCol = new TableColumn<>("Scheduled Time");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));

        table.getColumns().addAll(postCol, typeCol, slotCol);
        return table;
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
        scheduleTable.getItems().clear();
        pheromoneArea.clear();
        
        // Get config from UI
        int numAnts = Integer.parseInt(antsField.getText());
        double alpha = Double.parseDouble(alphaField.getText());
        double beta = Double.parseDouble(betaField.getText());
        int numIterations = Integer.parseInt(iterationsField.getText());
        ((NumberAxis)fitnessSeries.getChart().getXAxis()).setUpperBound(numIterations);

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
            ACOEngine engine = new ACOEngine(posts, slots, numAnts, alpha, beta, numIterations);
            
            Schedule best = engine.runUI(
                (iteration, bestF, avgF, pheromones) -> {
                    Platform.runLater(() -> {
                        fitnessSeries.getData().add(new XYChart.Data<>(iteration, bestF));
                        averageFitnessSeries.getData().add(new XYChart.Data<>(iteration, avgF));
                        bestFitnessLabel.setText(String.format("Best Fitness: %.2f", bestF));
                        iterationLabel.setText("Iteration: " + iteration + " / " + numIterations);
                        updatePheromoneDisplay(pheromones);
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
                displayScheduleInTable(best);
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

    private void updatePheromoneDisplay(double[][] pheromones) {
        StringBuilder sb = new StringBuilder();
        sb.append("Slot:\t");
        for (int i = 0; i < slots.size(); i++) {
            sb.append(i + 1).append("\t");
        }
        sb.append("\n----------------------------------------\n");

        for (int i = 0; i < posts.size(); i++) {
            sb.append("Post ").append(i + 1).append(":\t");
            for (int j = 0; j < slots.size(); j++) {
                sb.append(String.format("%.2f", pheromones[i][j])).append("\t");
            }
            sb.append("\n");
        }
        pheromoneArea.setText(sb.toString());
    }

    private void displayScheduleInTable(Schedule schedule) {
        ObservableList<ScheduleEntry> data = FXCollections.observableArrayList();
        for (Map.Entry<Post, TimeSlot> entry : schedule.getAssignments().entrySet()) {
            data.add(new ScheduleEntry(entry.getKey(), entry.getValue()));
        }
        scheduleTable.setItems(data);
    }
    
    private void log(String msg) {
        outputArea.appendText(msg + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class ScheduleEntry {
        private final SimpleStringProperty postId;
        private final SimpleStringProperty contentType;
        private final SimpleStringProperty timeSlot;

        public ScheduleEntry(Post post, TimeSlot slot) {
            this.postId = new SimpleStringProperty(String.valueOf(post.getId()));
            this.contentType = new SimpleStringProperty(post.getType().toString());
            this.timeSlot = new SimpleStringProperty(slot.getName());
        }

        public String getPostId() { return postId.get(); }
        public String getContentType() { return contentType.get(); }
        public String getTimeSlot() { return timeSlot.get(); }
    }
}
