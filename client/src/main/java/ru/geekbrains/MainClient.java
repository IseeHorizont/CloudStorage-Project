package ru.geekbrains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MainClient extends Application {

    public Stage primaryStage;
    private Stage authStage;
    private Stage registerStage;
    private ClientNetwork network;
    private StorageController storageController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        network = ClientNetwork.getInstance();

        if (!network.connect()) {
            showErrorMessage("","Ошибка подключения к серверу");
            return;
        }

        openAuthDialog(primaryStage);
        createFileMessenger(primaryStage);
    }

    private void createFileMessenger(Stage primaryStage) throws IOException {
        FXMLLoader mainLoader = new FXMLLoader();
        mainLoader.setLocation(MainClient.class.getResource("/Cloud.fxml"));
        Parent root = mainLoader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("CloudApp");
        primaryStage.setResizable(false);
        ClientNetwork network = ClientNetwork.getInstance();
        storageController = mainLoader.getController();
        storageController.setNetwork(network);
        network.start(storageController);
        primaryStage.setOnCloseRequest(event -> {
                    network.sendCommand("/end ", storageController);
                }
        );
    }


    private void openAuthDialog(Stage primaryStage) throws IOException {
        FXMLLoader authLoader = new FXMLLoader();
        authLoader.setLocation(MainClient.class.getResource("/authorization.fxml"));
        Parent page = authLoader.load();
        authStage = new Stage();
        authStage.setTitle("Авторизация");
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(primaryStage);
        Scene scene = new Scene(page);
        authStage.setScene(scene);
        authStage.show();
        AuthClientController authClientController = authLoader.getController();
        authClientController.setClientNetwork(network);
        authClientController.setClientApp(this);
        authStage.setOnCloseRequest(event -> {
            network.sendCommand("/end ", storageController);
        });

    }

    public void showFileMessenger() {
        authStage.close();
        primaryStage.show();
        primaryStage.setTitle(network.getClientNick());
        network.sendCommand("/ls", storageController);
        storageController.clientPath.setText(network.getClientDir());
        storageController.serverPath.setText(network.getServerDir());
    }

    public void showErrorMessage(String message, String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Проблемы с соединением");
        alert.setHeaderText(errorMessage);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public StorageController getStorageController() {
        return storageController;
    }
}
