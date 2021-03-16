package ru.geekbrains;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import ru.geekbrains.allcommands.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ClientNetwork {
    private static final String HOST = "localhost";
    private static final int PORT = 8189;
    private static final int BUFFER_SIZE = 8189;
    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;
    private static String clientDir = "Client" + File.separator + "src" + File.separator + "Files";
    private Socket socket;
    public static ClientNetwork instance;
    private static byte[] buffer;
    private static String clientNick;
    public boolean authOk;
    private String serverDir;


    public static ClientNetwork getInstance() {
        if (instance == null) {
            instance = new ClientNetwork();
        }
        return instance;
    }


    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());
            buffer = new byte[BUFFER_SIZE];
            authOk = false;
            return true;
        } catch (IOException e) {
            System.out.println("Соединение не было установлено!");
            e.printStackTrace();
            return false;
        }
    }

    public void start(StorageController storageController) {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Command message = readObject();
                    switch (message.getType()) {
                        case AUTH_OK: {
                            CommandResOk dataAuthOk = (CommandResOk) message.getData();
                            String login = dataAuthOk.getLogin();
                            setClientNick(login);
                            authOk = true;
                            break;
                        }

                        case OK: {
                            CommandResOk success = (CommandResOk) message.getData();
                            String result = success.getResult();
                            String info = success.getLogin();
                            storageController.showText(result, info);
                            sendCommand("/ls", storageController);

                            break;
                        }
                        case LS_OK: {
                            SendListFilesCommandData listFilesFromServer = (SendListFilesCommandData) message.getData();
                            ArrayList<String> listFiles = listFilesFromServer.getFiles();
                            serverDir = listFilesFromServer.getServerDir();
                            storageController.showFilesOnCloud(listFiles);
                            break;
                        }
                        case SEND: {
                            SendFileCommandData sendFileCommandData = (SendFileCommandData) message.getData();
                            getFile(sendFileCommandData, storageController);
                            break;
                        }
                        case ERROR: {
                            ErrorCommandData errorCommandData = (ErrorCommandData) message.getData();
                            storageController.showError("Операция не может быть выполнена!", errorCommandData.getError());
                            break;
                        }
                        case UNKNOWN: {
                            UnknownCommandData unknownCommandData = (UnknownCommandData) message.getData();
                            storageController.showError("Неизвестная команда от сервера!", unknownCommandData.getError());
                            break;
                        }
                        case GET: {
                            GetFileCommandData getFile = (GetFileCommandData) message.getData();
                            SendFileToStorage sendFileToCloud = new SendFileToStorage(clientDir + File.separator
                                                                                        + getFile.getFileName(), this);
                            sendFileToCloud.sendFile(os);


                            break;
                        }
                        case GET_DIR: {

                            GetFileCommandData getFile = (GetFileCommandData) message.getData();
                            String fileName = getFile.getFileName();
                            File dir = new File(clientDir + File.separator + fileName);
                            SendDirAndFileFromClient dirToSend = new SendDirAndFileFromClient(dir, this);
                            dirToSend.sendDir(os);

                            break;
                        }
                        case CREATE: {
                            CreateDirCommandData createNewDir = (CreateDirCommandData) message.getData();
                            String newDirName = clientDir + File.separator + createNewDir.getDirName();
                            File newDir = new File(newDirName);
                            newDir.mkdir();
                            break;
                        }
                        case END: {
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            close();
                            break;
                        }
                    }

                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    public void sendCommand(String textFromClient, StorageController storageController) {
        try {
            String[] msg = textFromClient.split(" ");
            String commandType = msg[0];
            String data;
            if (msg.length > 1) {
                data = textFromClient.split(" ", 2)[1];
            } else {
                data = "";
            }
            Command commandFromClient;

            switch (commandType) {
                case "/auth": {
                    if (data.split(" ").length < 2) {
                        storageController.showError("Команда не может быть выполнена!", "Неверно введена комманда - укажите /auth логин пароль");
                    } else {
                        String login = data.split(" ", 2)[0];
                        String password = data.split(" ", 2)[1];
                        commandFromClient = new Command().authCommand(login, password);
                        os.writeObject(commandFromClient);
                    }
                    break;
                }
                case "/ls": {
                    commandFromClient = new Command().listFilesCommand();
                    os.writeObject(commandFromClient);
                    ArrayList<String> fileNames = createListFiles();
                    storageController.showFilesOnClient(fileNames);
                    break;
                }

                case "/cd": {
                    if (data.equals("")) {
                        storageController.showError("Команда не может быть выполнена!", "Неверно введена комманда. Укажите путь (/cd директория)");
                    } else {
                        String directory;
                        if (data.trim().equals("...")) {
                            directory = data;
                        } else {
                            directory = data.split(" ")[0];
                        }
                        commandFromClient = new Command().changeDirectory(directory);
                        os.writeObject(commandFromClient);
                    }
                    break;
                }

                case "/get": {
                    if (data.equals("")) {
                        storageController.showError("Команда не может быть выполнена!", "Выберите файл из списка (Мое облако) и выделите его, щелкнув мышью.");
                    } else {
                        String fileName = data;
                        File file = new File(clientDir + File.separator + fileName);
                        if (file.exists()) {
                            storageController.showError("Команда не выполнена!", "Файл уже существует!");
                        } else {
                            commandFromClient = new Command().getFileFromServer(fileName);
                            os.writeObject(commandFromClient);
                        }
                    }
                    break;
                }

                case "/send": {
                    if (data.equals("")) {
                        storageController.showError("Команда не может быть выполнена!", "Выберите файл из списка (Мои файлы) и выделите его, щелкнув мышью.");
                    } else {
                        String fileName = data;
                        File fileToServer = new File(clientDir + File.separator + fileName);
                        if (!fileToServer.exists()) {
                            storageController.showError("Команда не может быть выполнена!", "Файл не существует!");
                        } else if (fileToServer.isDirectory()) {
                            Command dirToSend = new Command().sendDirWithFiles(fileName, 1L);
                            os.writeObject(dirToSend);
                        } else {
                            Long fileSize = fileToServer.length();
                            Command fileToSend = new Command().sendFile(fileName, fileSize, false);
                            os.writeObject(fileToSend);
                        }
                    }
                    break;
                }

                case "/mkdir": {
                    if (data.equals("")) {
                        storageController.showError("Неверно введена команда.", "Укажите имя новой директории.");
                    } else if (data.split(" ").length > 1) {
                        storageController.showError("Неверно введена команда. ", "Неверно указано имя новой директории.");

                    } else {
                        String dirName = data;
                        Command createNewDir = new Command().createNewDir(dirName);
                        os.writeObject(createNewDir);
                        File newDir = new File(clientDir + File.separator + dirName);
                    }
                    break;
                }

                case "/del": {
                    if (data.equals("")) {
                        storageController.showError("Команда не может быть выполнена!", "Выберите файл из списка (Мое облако) и выделите его, щелкнув мышью.");
                    } else {
                        String fileName = data.split(" ")[0];
                        Command fileToDelete = new Command().deleteFile(fileName);
                        os.writeObject(fileToDelete);
                    }
                    break;
                }

                case "/end": {
                    Command endCommand = new Command().closeConnection();
                    os.writeObject(endCommand);
                    break;
                }
                case "/move": {
                    String oldPlaceFile = data.split(" ")[0];
                    String newPlaceFile = data.split(" ")[1];
                    Command moveCommand = new Command().moveFile(oldPlaceFile, newPlaceFile);
                    os.writeObject(moveCommand);
                    break;
                }

                default:
                    storageController.showError("Неизвестная команда.", " Повторите ввод. Для справки - Help/About.");
                    break;
            }

            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public ArrayList<String> createListFiles() {
        File dir = new File(clientDir);
        File[] files = dir.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        fileNames.add(" ...  \n");
        StringBuilder sb = new StringBuilder();
        if (files != null) {
            for (File file : files) {
                sb.append(file.getName()).append(" ");

                if (file.isFile()) {
                    long modify = file.lastModified();
                    Date lm = new Date(modify);
                    String lastModify = new SimpleDateFormat("dd-MM-yyyy").format(lm);
                    sb.append("[FILE] | ").append(file.length()).append(" bytes | ").append(lastModify + "\n");

                } else {
                    sb.append("[DIR]\n");
                }
                fileNames.add(sb.toString());
                sb = new StringBuilder();
            }
        }
        return fileNames;
    }

    public void setClientDir(String dir) {
        ClientNetwork.clientDir = clientDir + File.separator + dir;
    }

    public void setClientDirDirect(String dir) {
        ClientNetwork.clientDir = dir;
    }

    public String getClientDir() {
        return clientDir;
    }

    public void setClientNick(String clientNick) {
        this.clientNick = clientNick;
    }

    public String getClientNick() {
        return clientNick;
    }

    public String getServerDir() {
        return serverDir;
    }

    public void getFile(SendFileCommandData sendFileCommandData, StorageController storageController) throws IOException {
        int ptr = 0;
        Long fileSize = sendFileCommandData.getFileSize();
        String fileName = sendFileCommandData.getFileName();
        File newFile = new File(clientDir + File.separator + fileName);
        try {
            try (FileOutputStream fos = new FileOutputStream(newFile, false)) {
                if (fileSize > buffer.length) {
                    while (fileSize > ptr) {
                        Command message = readObject();
                        FileInBuffer fileFromServer = (FileInBuffer) message.getData();
                        ptr = fileFromServer.getPtr();
                        buffer = fileFromServer.getBuffer();
                        fos.write(buffer, 0, ptr);
                        fileSize -= ptr;
                    }
                }
                byte[] bufferLast;
                while (fileSize > 0) {
                    Command message = readObject();
                    FileInBuffer fileFromServer = (FileInBuffer) message.getData();
                    ptr = fileFromServer.getPtr();
                    bufferLast = fileFromServer.getBuffer();
                    fos.write(bufferLast, 0, ptr);
                    fileSize -= ptr;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }


    public Command readObject() throws IOException, ClassNotFoundException {
        return (Command) is.readObject();
    }


    public void close() {
        try {
            Thread.sleep(10);
            is.close();
            os.close();
            socket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
