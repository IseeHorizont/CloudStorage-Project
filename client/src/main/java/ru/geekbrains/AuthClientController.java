package ru.geekbrains;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthClientController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passField;

    private ClientNetwork network;
    private MainClient clientApp;

    @FXML
    public void checkAuth() {
        String login = loginField.getText();
        String password = passField.getText();
        if (login.isEmpty()|| password.isEmpty()) {
            clientApp.showErrorMessage("Поля не должны быть пустыми", "Ошибка ввода");
            return;
        }
        String authMessage ="/auth "+ login + " " + password;
        network.sendCommand(authMessage,clientApp.getStorageController());

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(network.authOk){
            clientApp.showFileMessenger();
        }
    }

    public void setClientNetwork(ClientNetwork network) {
        this.network = network;
    }

    public PasswordField getPassField() {
        return passField;
    }

    public TextField getLoginField() {
        return loginField;
    }

    public void setClientApp(MainClient cloudApp) {
        this.clientApp = cloudApp;
    }
}
