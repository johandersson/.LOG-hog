import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(tabPane);
        mainLayout.setBottom(new Label("Press Ctrl+S to save and Ctrl+R to load"));

        Scene scene = new Scene(mainLayout, 600, 400);
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

    private BorderPane createEntryPanel() {
        BorderPane pane = new BorderPane();
        textArea.setStyle("-fx-font-size: 14;");
        pane.setCenter(textArea);
        return pane;
    }

    private BorderPane createLogPanel() {
        BorderPane pane = new BorderPane();
        logList.setStyle("-fx-font-size: 14;");

        logList.setOnMouseClicked(e -> {
            String selectedItem = logList.getSelectionModel().getSelectedItem();
            System.out.println("Selected Timestamp Clicked: " + selectedItem);
            if (selectedItem != null) {
                String logContent = logFileHandler.loadEntry(selectedItem);
                System.out.println("Setting UI Text:\n" + logContent);
                entryArea.setText(logContent);
            }
        });

        //add a context menu to the log list, to delete entries

        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = getDeleteItem();

        contextMenu.getItems().add(deleteItem);
        logList.setContextMenu(contextMenu);



        pane.setCenter(logList);
        pane.setBottom(entryArea);
        return pane;
    }

    private MenuItem getDeleteItem() {
        MenuItem deleteItem = new MenuItem("Delete Entry");
        deleteItem.setOnAction(e -> {
            String selectedItem = logList.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                // Confirm deletion
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this entry?", ButtonType.YES, ButtonType.NO);
                alert.setTitle("Delete Entry");
                alert.setHeaderText("Delete Log Entry");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        logFileHandler.deleteEntry(selectedItem, listModel);
                        updateLogListView();
                        entryArea.clear();
                    }
                });
            }
        });
        return deleteItem;
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
            System.out.println("ListView contents: " + logList.getItems());
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
