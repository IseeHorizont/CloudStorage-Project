package ru.geekbrains;

import javafx.application.Platform;
import javafx.event.ActionEvent;
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

    @FXML
    public TextField newLoginField;
    @FXML
    public PasswordField newPassField;

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
        String authMessage = "/auth " + login + " " + password;
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

    @FXML
    public void signUpNewUser(){
        String login = newLoginField.getText();
        String password = newPassField.getText();
        if (login.isEmpty()|| password.isEmpty()) {
            clientApp.showErrorMessage("Поля не должны быть пустыми", "Ошибка ввода логина/пароля");
            return;
        }
        String regMessage = "/reg " + login + " " + password;
        network.sendCommand(regMessage,clientApp.getStorageController());

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(network.regOk){
            clientApp.showMessage("Successful registration");
            clientApp.closeRegForm();
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

    public void signUpBtn(ActionEvent actionEvent){
        Platform.runLater(() ->{
            clientApp.showRegisterForm();
        });
    }
}
