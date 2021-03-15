package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.geekbrains.allcommands.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;

public class FileHandler extends SimpleChannelInboundHandler<Command> {

    private final String SERVER_DIRECTORY = "MainServer" + File.separator + "src" + File.separator + "Files";//TODO what's this
    private String serverDirect = "MainServer" + File.separator + "src" + File.separator + "Files";//TODO what's this
    private static MainServer server;
    private String userName;
    private byte[] buffer = new byte[8189];
    private String fileName;
    private Long fileSize;

    public FileHandler(MainServer server, String userName) {
        this.server = server;
        this.userName = userName;
        serverDirect =  serverDirect + File.separator + userName;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        MainServer.logger.log(Level.INFO,"Client successfully passed authorization!");
        File file = new File(serverDirect);
        if(!file.exists())
        {
            new File(serverDirect).mkdir();
        }
        Command command=new Command().successAuth(userName);
        ctx.writeAndFlush(command);
        server.getClients().put(ctx, userName);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        MainServer.logger.log(Level.INFO,"Client disconnect!");
        server.getClients().remove(ctx);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        MainServer.logger.log(Level.SEVERE,"Channel throws exception");
        cause.printStackTrace();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) throws Exception {
        Command commandFromClient = command;
        switch (commandFromClient.getType()){

            case CD:{
                ChangeDirectoryCommandData changeDirectoryCommandData = (ChangeDirectoryCommandData)commandFromClient.getData();
                String directory = changeDirectoryCommandData.getPath();
                MainServer.logger.log(Level.INFO,"Получена команда CD "+ directory);
                File file = new File(serverDirect);
                if (directory.trim().equals("...")) {
                    if (serverDirect.equals(SERVER_DIRECTORY + File.separator + userName)) {
                        return;
                    }
                    File parent = new File(file.getParent());
                    if (parent.exists()) {
                        serverDirect = parent.getPath();
                    }
                }
                else {
                    file = new File(serverDirect + File.separator + directory);
                    if (file.exists() && file.isDirectory()) {
                        serverDirect = serverDirect + File.separator + directory;
                    }
                }
                ArrayList<String> filesList = createListFiles();
                String serverDirToClient = serverDirect.replace(SERVER_DIRECTORY + File.separator,"");
                Command commandToClient = new Command().sendListFiles(filesList,serverDirToClient);
                ctx.writeAndFlush(commandToClient);
                break;
            }

            case LS:{
                MainServer.logger.log(Level.INFO,"Получена команда LS ");
                ArrayList<String>filesList = createListFiles();
                String serverDirToClient = serverDirect.replace(SERVER_DIRECTORY + File.separator, "");
                Command commandToClient = new Command().sendListFiles(filesList,serverDirToClient);
                ctx.writeAndFlush(commandToClient);
                break;
            }

            case GET: {
                MainServer.logger.log(Level.INFO,"Получена команда GET ");
                GetFileCommandData getFileCommandData = (GetFileCommandData) commandFromClient.getData();
                String fileName = getFileCommandData.getFileName();
                File fileToSend = new File(serverDirect + File.separator + fileName);
                if (fileToSend.exists()&&fileToSend.isFile()) {
                    Long fileSize = fileToSend.length();
                    Command commandFile = new Command().sendFile(fileName, fileSize,false);
                    ctx.writeAndFlush(commandFile);
                    SendFileFromStorageToClient sendFileFromCloud = new SendFileFromStorageToClient(fileToSend);
                    sendFileFromCloud.createCommandAndSend(ctx);
                    Command result = new Command().success("Файл успешно отправлен клиенту!");
                    ctx.writeAndFlush(result);
                }

                else if(fileToSend.isDirectory()){
                    SendDirAndFileFromStorage sendDirWithFilesFromCloud= new SendDirAndFileFromStorage(fileToSend,this);
                    sendDirWithFilesFromCloud.execute(ctx);
                    Command result = new Command().success("Директория с файлами успешно отправлена клиенту!");
                    ctx.writeAndFlush(result);
                }
                else {
                    Command commandToClient = new Command().error("Файла не существует!");
                    ctx.writeAndFlush(commandToClient);
                }
                break;
            }

            case SEND: {
                MainServer.logger.log(Level.INFO,"Получена команда SEND");
                SendFileCommandData sendFileCommandData = (SendFileCommandData) commandFromClient.getData();
                fileName = sendFileCommandData.getFileName();
                fileSize = sendFileCommandData.getFileSize();
                File newFile = new File(serverDirect + File.separator + fileName);
                if (newFile.exists()) {
                    Command commandToClient = new Command().error("Файл с таким именем уже есть на сервере!");
                    ctx.writeAndFlush(commandToClient);
                } else {
                    Command commandToClient = new Command().getFileFromServer(fileName);
                    ctx.writeAndFlush(commandToClient);
                }
                break;
            }

            case SEND_DIR:{
                MainServer.logger.log(Level.INFO,"Получена команда SEND_DIR");
                SendFileCommandData sendFileCommandData = (SendFileCommandData) commandFromClient.getData();
                fileName = sendFileCommandData.getFileName();
                fileSize = sendFileCommandData.getFileSize();
                File newDir= new File(serverDirect + File.separator + fileName);
                if (newDir.exists()&&newDir.isDirectory()) {
                    Command commandToClient = new Command().error("Папка с таким именем уже есть на сервере!");
                    ctx.writeAndFlush(commandToClient);
                } else {
                    newDir.mkdir();
                    Command commandToClient = new Command().getDirWithFiles(fileName);
                    ctx.writeAndFlush(commandToClient);
                }
                break;
            }

            case FILE: {
                FileInBuffer file = (FileInBuffer) commandFromClient.getData();
                String fileName = file.getFileName();
                File newFile = new File(serverDirect + File.separator + fileName);
                int ptr = 0;
                try {
                    try (FileOutputStream fos = new FileOutputStream(newFile, true)) {

                        ptr = file.getPtr();
                        buffer = file.getBuffer();
                        fos.write(buffer, 0, ptr);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            case CREATE:{
                MainServer.logger.log(Level.INFO,"Получена команда CREATE");
                CreateDirCommandData createDidCommandData = (CreateDirCommandData) commandFromClient.getData();
                String dirName = createDidCommandData.getDirName();
                String fullDirName = serverDirect + File.separator + dirName;
                File file = new File(fullDirName);
                if(!file.exists()||(file.exists()&&!file.isDirectory()))
                {
                    file.mkdir();
                    Command commandToClient = new Command().success(" Создана директория "+ dirName);
                    ctx.writeAndFlush(commandToClient);
                }
                else {
                    Command commandToClient = new Command().error("Директория с таким именем уже существует на сервере!");
                    ctx.writeAndFlush(commandToClient);
                }
                break;
            }

            case DELETE:{
                MainServer.logger.log(Level.INFO,"Получена команда DELETE");
                DeleteFileCommandData deleteFileCommandData = (DeleteFileCommandData) commandFromClient.getData();
                String fileName = deleteFileCommandData.getFileName();
                File fileToDelete = new File(serverDirect + File.separator + fileName);
                if (fileToDelete.exists()) {
                    if (!fileToDelete.isDirectory()) {
                        fileToDelete.delete();
                    } else if (fileToDelete.isDirectory()) {
                        deleteDirectory(fileToDelete);
                    }
                    Command commandToClient = new Command().success("Файл удален из хранилища!");
                    ctx.writeAndFlush(commandToClient);
                }
                else {
                    Command commandToClient = new Command().error("Файл не существует!");
                    ctx.writeAndFlush(commandToClient);
                }
                break;
            }
            case MOVE:{
                MainServer.logger.log(Level.INFO,"Получена команда MOVE");
                MoveFileCommandData moveFileCommandData = (MoveFileCommandData) commandFromClient.getData();
                String fileName = moveFileCommandData.getOldFile();
                String oldFilePath = serverDirect + File.separator + fileName;
                String newPathForFileFromClient = moveFileCommandData.getNewPLaceFile();
                String exactNewPath = SERVER_DIRECTORY + File.separator + newPathForFileFromClient;
                File oldFile = new File(oldFilePath);
                if (oldFile.exists()&&!oldFile.isDirectory()){
                    File newPath = new File (exactNewPath);
                    if (newPath.exists()&&newPath.isDirectory()){
                        File newFile = new File(exactNewPath + File.separator + fileName);
                        if(!newFile.exists()) {
                            MoveFile moveFile = new MoveFile(oldFile, newFile);
                            try {
                                moveFile.execute();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            oldFile.delete();
                            Command commandToClient = new Command().success("Файл успешно перемещен в новую директорию!");
                            ctx.writeAndFlush(commandToClient);
                        }
                        else if(newFile.exists()){
                            Command commandToClient = new Command().error("Файл с таким именем уже существует в выбранной папке!");
                            ctx.writeAndFlush(commandToClient);
                        }
                    }
                    if(!newPath.exists()||!newPath.isDirectory()){
                        Command commandToClient = new Command().error("Неверно указан путь!");
                        ctx.writeAndFlush(commandToClient);
                    }


                }
                else if(oldFile.isDirectory()){
                    File newDir = new File (exactNewPath + File.separator + fileName);
                    newDir.mkdir();
                    MoveDirectory moveDirectory = new MoveDirectory(oldFile,newDir);
                    String result = moveDirectory.execute();
                    Command commandToClient;
                    if (result==null){
                        commandToClient = new Command().success("Папка с файлами успешно перенесена в новую директорию!");
                    }
                    else {
                        commandToClient = new Command().error(result);
                    }
                    ctx.writeAndFlush(commandToClient);
                }
                break;
            }
            case ERROR:{
                ErrorCommandData errorCommandData = (ErrorCommandData) commandFromClient.getData();
                String error = errorCommandData.getError();
                MainServer.logger.log(Level.WARNING,"Ошибка "+ error);
                break;
            }
            case END:{
                Command commandEndToClient = new Command().closeConnection();
                //NettyServer.logger.log(Level.INFO,"Получена неизвестная команда END");
                ctx.writeAndFlush(commandEndToClient);
                ctx.close();
                break;
            }
            default:{
                MainServer.logger.log(Level.WARNING,"Получена неизвестная команда");
                break;
            }
        }
    }

    public String getServerDirect(){
        return serverDirect;
    }

    public ArrayList<String > createListFiles(){
        File dir = new File(serverDirect);
        File[] files = dir.listFiles();
        ArrayList<String> filesList = new ArrayList<>();
        filesList.add(" ... ");
        if (files!=null) {
            for (File file : files) {
                StringBuilder sb = new StringBuilder();
                sb.append(file.getName()).append(" ");
                if (file.isFile()) {
                    sb.append("[FILE} | ").append(file.length()).append(" bytes.\n");
                } else {
                    sb.append("[DIR]\n");
                }
                filesList.add(sb.toString());
                sb = new StringBuilder();
            }
        }
        return filesList;
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
    }
}
