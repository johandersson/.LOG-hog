import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javax.swing.DefaultListModel;
import java.util.Objects;

public class LogTextEditor extends Application {

    private final TextArea textArea = new TextArea();
    private final ListView<String> logList = new ListView<>();
    private final TextArea entryArea = new TextArea();
    private final LogFileHandler logFileHandler = new LogFileHandler();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(".LOG hog");


        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("Entry", createEntryPanel()));
        tabPane.getTabs().add(new Tab("Log Entries", createLogPanel()));

        MenuBar menuBar = new MenuBar();


        MenuItem saveMenuItem = new MenuItem("Save all CTRL+S");
        saveMenuItem.setOnAction(e -> saveLogEntry());

        MenuItem loadMenuItem = new MenuItem("Reload all CTRL+R");
        loadMenuItem.setOnAction(e -> loadLogEntries());


        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(menuBar);
        mainLayout.setCenter(tabPane);
        mainLayout.setBottom(new Label("Press Ctrl+S to save and Ctrl+R to load"));

        Scene scene = new Scene(mainLayout, 600, 400);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("dark-theme.css")).toExternalForm());

        // ✅ Add global keyboard shortcut handling
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown()) {
                switch (e.getCode()) {
                    case S -> saveLogEntry();
                    case R -> loadLogEntries();
                }
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(textArea::requestFocus);
        loadLogEntries();
    }

    private BorderPane createLogPanel() {
        BorderPane pane = new BorderPane();
        logList.setStyle("-fx-font-size: 14;");

        // ✅ Single-click selection for log entries
        logList.setOnMouseClicked(e -> {
            String selectedItem = logList.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                entryArea.setText(logFileHandler.loadEntry(selectedItem));
            }
        });

        // ✅ Instant selection update without clicking
        logList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                entryArea.setText(logFileHandler.loadEntry(newVal));
            }
        });

        pane.setCenter(logList);
        pane.setBottom(entryArea);
        return pane;
    }

    private BorderPane createEntryPanel() {
        BorderPane pane = new BorderPane();
        textArea.setStyle("-fx-font-size: 14;");
        pane.setCenter(textArea);
        return pane;
    }

    private void saveLogEntry() {
        logFileHandler.saveText(textArea.getText(), listModel);
        textArea.clear();
        updateLogListView();
    }

    private void loadLogEntries() {
        logFileHandler.loadLogEntries(listModel);
        updateLogListView();
    }

    private void updateLogListView() {
        Platform.runLater(() -> {
            logList.getItems().setAll(java.util.Collections.list(listModel.elements()));
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
