import javafx.application.Application;
        import javafx.application.Platform;
        import javafx.scene.Scene;
        import javafx.scene.control.*;
        import javafx.scene.layout.BorderPane;
        import javafx.stage.Stage;

        import javax.swing.DefaultListModel;

        public class LogTextEditor extends Application {

            private final TextArea textArea = new TextArea();
            private final ListView<String> logList = new ListView<>();
            private final TextArea entryArea = new TextArea();
            private final LogFileHandler logFileHandler = new LogFileHandler();
            private final DefaultListModel<String> listModel = new DefaultListModel<>();

            @Override
            public void start(Stage primaryStage) {
                primaryStage.setTitle("Log Text Editor");

                TabPane tabPane = new TabPane();
                tabPane.getTabs().add(new Tab("Entry", createEntryPanel()));
                tabPane.getTabs().add(new Tab("Log Entries", createLogPanel()));

                MenuBar menuBar = new MenuBar();
                Menu fileMenu = new Menu("File");
                MenuItem saveMenuItem = new MenuItem("Save all CTRL+S");
                saveMenuItem.setOnAction(e -> {
                    logFileHandler.saveText(textArea.getText(), listModel);
                    textArea.clear();
                    updateLogListView();
                });
                MenuItem loadMenuItem = new MenuItem("Reload all CTRL+R");
                loadMenuItem.setOnAction(e -> {
                    logFileHandler.loadLogEntries(listModel);
                    updateLogListView();
                });
                fileMenu.getItems().addAll(saveMenuItem, loadMenuItem);
                menuBar.getMenus().add(fileMenu);

                BorderPane mainLayout = new BorderPane();
                mainLayout.setTop(menuBar);
                mainLayout.setCenter(tabPane);
                mainLayout.setBottom(new Label("Press Ctrl+S to save and Ctrl+R to load"));

                Scene scene = new Scene(mainLayout, 600, 400);
                primaryStage.setScene(scene);
                primaryStage.show();

                // Initial load
                logFileHandler.loadLogEntries(listModel);
                updateLogListView();
            }

            private BorderPane createLogPanel() {
                BorderPane pane = new BorderPane();
                logList.setStyle("-fx-font-size: 14;");
                logList.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        String selectedItem = logList.getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            String entry = logFileHandler.loadEntry(selectedItem);
                            entryArea.setText(entry);
                        }
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

            private void updateLogListView() {
                Platform.runLater(() -> {
                    logList.getItems().setAll(java.util.Collections.list(listModel.elements()));
                });
            }

            public static void main(String[] args) {
                launch(args);
            }
        }