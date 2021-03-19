package ru.geekbrains;

import ru.geekbrains.allcommands.*;
import ru.geekbrains.allcommands.MoveFileCommandData;

import java.io.Serializable;
import java.util.ArrayList;

public class Command implements Serializable {
    private CommandsType type;
    private Object data;

    public Command regCommand(String login, String password) {
        Command command = new Command();
        command.type = CommandsType.REG;
        command.data = new RegCommandData(login,password);
        return command;
    }

    public Command authCommand(String login, String password) {
        Command command = new Command();
        command.type = CommandsType.AUTH;
        command.data = new AuthCommandData(login,password);
        return command;
    }

    public Command listFilesCommand(){
        Command command = new Command();
        command.type=CommandsType.LS;
        command.data= new ListFilesCommandData();
        return command;
    }
    public Command changeDirectory(String directory){
        Command command = new Command();
        command.type=CommandsType.CD;
        command.data= new ChangeDirectoryCommandData(directory);
        return command;
    }
    public Command unknownCommand(String error){
        Command command= new Command();
        command.type= CommandsType.UNKNOWN;
        command.data= new UnknownCommandData(error);
        return command;
    }

    public Command successReg (String login){
        Command command = new Command();
        command.type = CommandsType.REG_OK;
        command.data = new CommandResOk("Успешная регистрация нового пользователя", login);
        return command;
    }

    public Command successAuth (String login){
        Command command = new Command();
        command.type = CommandsType.AUTH_OK;
        command.data = new CommandResOk("Авторизация прошла успешно!", login);
        return command;
    }

    public Command sendListFiles(ArrayList<String> filesList, String serverDir){
        Command command = new Command();
        command.type=CommandsType.LS_OK;
        command.data=new SendListFilesCommandData(filesList, serverDir);
        return command;
    }

    public Command getFileFromServer (String fileName){
        Command command = new Command();
        command.type = CommandsType.GET;
        command.data = new GetFileCommandData(fileName);
        return command;
    }

    public Command sendFile (String fileName, Long fileSize, boolean fromDir){
        Command command = new Command();
        command.type = CommandsType.SEND;
        command.data = new SendFileCommandData(fileName, fileSize, fromDir);
        return  command;
    }

    public Command sendDirWithFiles (String fileName, Long fileSize){
        Command command = new Command();
        command.type = CommandsType.SEND_DIR;
        command.data=new SendFileCommandData(fileName, fileSize, true);
        return command;
    }

    public Command getDirWithFiles (String fileName){
        Command command = new Command();
        command.type = CommandsType.GET_DIR;
        command.data = new GetFileCommandData(fileName);
        return  command;
    }
    public Command file (byte[] buffer, int ptr){
        Command command= new Command();
        command.type = CommandsType.FILE;
        command.data = new FileInBuffer(buffer,ptr);
        return command;
    }
    public Command fileToServer (String filename, byte[] buffer, int ptr){
        Command command= new Command();
        command.type = CommandsType.FILE;
        command.data = new FileInBuffer(buffer,ptr, filename);
        return command;
    }
    public Command error (String error){
        Command command = new Command();
        command.type = CommandsType.ERROR;
        command.data = new ErrorCommandData(error);
        return command;
    }

    public Command createNewDir(String dirName){
        Command command = new Command();
        command.type = CommandsType.CREATE;
        command.data = new CreateDirCommandData(dirName);
        return command;
    }

    public Command success(String message){
        Command command = new Command();
        command.type = CommandsType.OK;
        command.data = new CommandResOk("Операция успешно выполнена!", message);
        return command;
    }

    public Command deleteFile(String fileName){
        Command command = new Command();
        command.type = CommandsType.DELETE;
        command.data = new DeleteFileCommandData(fileName);
        return command;
    }

    public Command closeConnection (){
        Command command = new Command();
        command.type = CommandsType.END;
        return command;
    }

    public Command moveFile(String oldPlaceFile, String newPlaceFile){
        Command command = new Command();
        command.type = CommandsType.MOVE;
        command.data = new MoveFileCommandData(oldPlaceFile, newPlaceFile);
        return  command;
    }
    public CommandsType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
