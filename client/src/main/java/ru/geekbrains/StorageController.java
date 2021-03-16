package ru.geekbrains;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Lighting;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

public class StorageController implements Initializable {
    public ClientNetwork network;
    public ListView<String> filesClientList;
    public ListView<String> filesCloudList;
    public ImageView sendBtn;
    public ImageView getBtn;
    public ImageView updateBtn;
    public ImageView deleteBtn;
    public Label clientPath;
    public Label serverPath;
    public ImageView addOnClient;
    public ImageView addOnServer;
    private String selectedFile;
    private String selectedFileOnCloud;
    private static final String clientParent = "Client" + File.separator + "src" + File.separator + "Files";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filesClientList.setItems(FXCollections.observableArrayList());
        filesClientList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = filesClientList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                filesClientList.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    selectionModel.select(index);
                    selectedFile = cell.getItem();
                    if (event.getClickCount() == 2) {
                        changeDir(selectedFile);
                        selectedFile = null;
                    }
                    if (event.getButton() == MouseButton.SECONDARY) {
                        showSelectAction(selectedFile, "myFiles");
                    }
                    event.consume();
                }
            });

            return cell;
        });

        filesCloudList.setItems(FXCollections.observableArrayList());
        filesCloudList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = filesCloudList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                filesCloudList.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    selectionModel.select(index);
                    selectedFileOnCloud = cell.getItem();
                    if (event.getClickCount() == 2) {
                        try {
                            changeDirOnCloud(selectedFileOnCloud);
                            selectedFileOnCloud = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (event.getButton() == MouseButton.SECONDARY) {
                        showSelectAction(selectedFileOnCloud, "myCloud");
                    }
                    event.consume();
                }
            });

            return cell;
        });
    }

    private void changeDirOnCloud(String selectedFileOnCloud) throws IOException {
        network.sendCommand("/cd " + selectedFileOnCloud, this);
    }

    private void showSelectAction(String name, String place) {
        Alert selectAction = new Alert(Alert.AlertType.CONFIRMATION);
        selectAction.setTitle("Select.");
        selectAction.setHeaderText("Choose the required action with selected file.");
        ButtonType delete = new ButtonType("Delete");
        ButtonType move = new ButtonType("Move");
        ButtonType exit = new ButtonType("Back");
        selectAction.getButtonTypes().clear();
        selectAction.getButtonTypes().addAll(delete, move, exit);
        Optional<ButtonType> option = selectAction.showAndWait();
        if (place.equals("myFiles")) {
            if (option.get() == exit) {
                selectAction.close();
            } else if (option.get() == delete) {
                deleteFile(name);
            } else if (option.get() == move) {
                move(selectedFile);
            }
        }
        if (place.equals("myCloud")) {
            if (option.get() == exit) {
                selectAction.close();
            } else if (option.get() == delete) {
                network.sendCommand("/del " + name, this);
            } else if (option.get() == move) {
                TextInputDialog textInputDialog = new TextInputDialog("");
                textInputDialog.setHeaderText("Укажите директорию, в которую вы хотите переместить выбранный файл.");
                textInputDialog.showAndWait();
                String nameDir = textInputDialog.getResult();
                if (nameDir == null) {
                    return;
                } else {
                    String fileOldPlaceName = selectedFileOnCloud.split(" ")[0];
                    network.sendCommand("/move " + fileOldPlaceName + " " + nameDir, this);
                }
            }
        }
    }

    private void move(String selectedFile) {
        TextInputDialog textInputDialog = new TextInputDialog("");
        textInputDialog.setHeaderText("Укажите директорию, в которую вы хотите переместить выбранный файл.");
        textInputDialog.showAndWait();
        String nameDir = textInputDialog.getResult();
        if (nameDir == null) {
            return;
        } else {
            File changedDir = new File(nameDir.replaceAll("/", File.separator));
            if (changedDir.isDirectory() && changedDir.exists()) {
                File newPlaceFile = new File(nameDir + File.separator + selectedFile.split(" ")[0]);
                if (newPlaceFile.exists() && newPlaceFile.isFile()) {
                    showError("Операция не может быть выполнена!", "Файл с таким именем уже есть в выбранной директории!");
                    return;
                }
                File oldFile = new File(network.getClientDir() + File.separator + selectedFile.split(" ")[0]);
                if (oldFile.isFile()) {
                    try {
                        MoveFile moveFile = new MoveFile(oldFile, newPlaceFile);
                        moveFile.execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    oldFile.delete();
                    showText("Команда выполнена!", "Файл успешно перемещен в новую директорию!");
                } else if (oldFile.isDirectory()) {

                    File newDir = new File(nameDir + File.separator + selectedFile.split(" ")[0]);
                    newDir.mkdir();
                    MoveDirectory moveDirectory = new MoveDirectory(oldFile, newDir);
                    String result = moveDirectory.execute();
                    if (result == null) {
                        showText("Команда выполнена!", "Выбранная папка с файлами успешно перемещена в новую директорию!");
                    } else {
                        showError("Операция не может быть выполнена!", result);
                    }
                }
            } else {
                showError("Команда не выполнена!", "Не верно указан путь.");
            }
        }
        network.sendCommand("/ls", this);
    }


    private void deleteFile(String selectedFile) {
        File fileToDelete = new File(network.getClientDir() + File.separator + selectedFile.split(" ")[0]);
        if (fileToDelete.exists() && !fileToDelete.isDirectory()) {
            fileToDelete.delete();
        } else if (fileToDelete.isDirectory()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Вы выбрали директорию.");
            alert.setContentText("Вы уверены, что хотите удалить директорию вмете со всеми файлами?");
            ButtonType delete = new ButtonType("Delete");
            ButtonType exit = new ButtonType("Cancel");
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(delete, exit);
            Optional<ButtonType> option = alert.showAndWait();
            if (option.get() == exit) {
                alert.close();
            } else if (option.get() == delete) {
                deleteDirectory(fileToDelete);
            }
        }
        network.sendCommand("/ls", this);
    }

    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDirectory(f);
                }
            }
        }
        file.delete();
        network.sendCommand("/ls", this);
    }

    private void changeDir(String selectedFile) {
        File file = new File(network.getClientDir());
        if (selectedFile.trim().equals("...")) {
            if (network.getClientDir().equals(clientParent)) {
                return;
            }
            File parent = new File(file.getParent());
            if (parent.exists()) {
                network.setClientDirDirect(parent.getPath());
            }
        } else {
            String dirName = selectedFile.replace(" [DIR]", "").trim();
            File newFile = new File(network.getClientDir() + File.separator + dirName);
            if (newFile.exists() && newFile.isDirectory()) {
                network.setClientDir(dirName);
            }
        }
        clientPath.setText(network.getClientDir());
        ArrayList<String> files = network.createListFiles();
        showFilesOnClient(files);
    }

    public void showText(String type, String text) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(type);
            alert.setContentText(text);
            alert.showAndWait();
        });
    }

    public void showError(String type, String text) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(type);
            alert.setContentText(text);
            alert.showAndWait();
        });
    }

    public void showFilesOnClient(List<String> files) {
        Platform.runLater(() -> {
            filesClientList.getItems().clear();
            filesClientList.setItems(FXCollections.observableArrayList(files));
        });
    }

    public void showFilesOnCloud(List<String> files) {
        Platform.runLater(() -> {
            serverPath.setText(network.getServerDir());
            filesCloudList.getItems().clear();
            filesCloudList.setItems(FXCollections.observableArrayList(files));
        });
    }

    public void sendCommand(ActionEvent mouseEvent) throws IOException {
        if (selectedFile == null) {
            showError("Команда не выполнена", "Выберите файл");
            return;
        }
        String fileName = selectedFile.split(" ")[0];

        network.sendCommand("/send " + fileName, this);
    }

    public void getCommand(ActionEvent actionEvent) throws IOException {
        if (selectedFileOnCloud == null) {
            showError("Команда не выполнена", "Выберите файл");
            return;
        }
        String fileName = selectedFileOnCloud.split(" ")[0];
        network.sendCommand("/get " + fileName, this);
    }

    public void updateCommand(ActionEvent actionEvent) {
        network.sendCommand("/ls", this);
    }

    public void changeStyleOnMouseEnterBtnGet(MouseEvent mouseEvent) {
        setActiveButtonStyle(getBtn);
    }

    public void changeStyleOnMouseExitBtnGet(MouseEvent mouseEvent) {
        getBtn.setEffect(null);
    }

    public void changeStyleOnMouseEnterBtnSend(MouseEvent mouseEvent) {
        setActiveButtonStyle(sendBtn);
    }

    public void changeStyleOnMouseExitBtnSend(MouseEvent mouseEvent) {
        sendBtn.setEffect(null);
    }

    public void changeStyleOnMouseEnterBtnUpdate(MouseEvent mouseEvent) {
        setActiveButtonStyle(updateBtn);
    }

    public void changeStyleOnMouseExitBtnUpdate(MouseEvent mouseEvent) {
        updateBtn.setEffect(null);
    }

    public void setActiveButtonStyle(ImageView imageView) {
        Glow glow = new Glow();
        glow.setLevel(0.9);
        imageView.setEffect(glow);
        Lighting lighting = new Lighting();
        imageView.setEffect(lighting);
    }

    public void setNetwork(ClientNetwork network) {
        this.network = network;
    }

    public void addDirOnClient(ActionEvent actionEvent) {
        TextInputDialog textInputDialog = new TextInputDialog("");
        textInputDialog.setHeaderText("Введите имя директории.");
        textInputDialog.showAndWait();
        if (textInputDialog.getResult() != null) {
            String nameDir = textInputDialog.getResult();
            if (!nameDir.trim().equals("")) {
                File file = new File(network.getClientDir() + File.separator + nameDir);
                if (file.exists() && file.isDirectory()) {
                    showError("Невозможно выполнить операцию", "Директория с таким именем уже создана!");
                } else {
                    file.mkdir();
                }
            } else {
                showError("Невозможно выполнить операцию", "Не указано имя новой директории");
            }
        }
        network.sendCommand("/ls", this);

    }

    public void addDirOnCloud(ActionEvent actionEvent) {
        TextInputDialog textInputDialog = new TextInputDialog("");
        textInputDialog.setHeaderText("Введите имя директории.");
        textInputDialog.showAndWait();
        if (textInputDialog.getResult() != null) {
            String nameDir = textInputDialog.getResult();
            if (!nameDir.trim().equals("")) {
                network.sendCommand("/mkdir " + nameDir, this);
            } else {
                showError("Невозможно выполнить операцию", "Не указано имя новой директории");
            }
        }
        network.sendCommand("/ls", this);
    }

    public void changeStyleOnMouseEnterBtnAddClient(MouseEvent mouseEvent) {
        setActiveButtonStyle(addOnClient);
    }

    public void changeStyleOnMouseExitBtnAddClient(MouseEvent mouseEvent) {
        addOnClient.setEffect(null);
    }

    public void changeStyleOnMouseEnterBtnAddCloud(MouseEvent mouseEvent) {
        setActiveButtonStyle(addOnServer);
    }

    public void changeStyleOnMouseExitBtnAddCloud(MouseEvent mouseEvent) {
        addOnServer.setEffect(null);
    }

    public void showHelp(ActionEvent actionEvent) throws FileNotFoundException {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setHeaderText("Помощь");
        info.setResizable(true);
        info.getDialogPane().setMinWidth(500);
        Scanner scanner = new Scanner(new File("help.txt"));
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.nextLine()).append("\n");
        }
        info.setContentText(sb.toString());
        scanner.close();
        info.show();
    }

    public void showAbout(ActionEvent actionEvent) throws FileNotFoundException {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setHeaderText("О приложении");
        info.setResizable(true);
        info.getDialogPane().setMinWidth(500);
        Scanner scanner = new Scanner(new File("aboutApp.txt"));
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) {
            sb.append(scanner.nextLine()).append("\n");
        }
        info.setContentText(sb.toString());
        scanner.close();
        info.show();
    }

    public void exitBtn(ActionEvent actionEvent){
        Platform.runLater(() ->{
            System.exit(0);
        });
    }
}
