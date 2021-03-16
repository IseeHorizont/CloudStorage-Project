package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SendDirAndFileFromStorage {
    File dirFromCloud;
    FileHandler fileHandler;

    public SendDirAndFileFromStorage (File dirFromCloud, FileHandler fileHandler) {
        this.dirFromCloud = dirFromCloud;
        this.fileHandler= fileHandler;
    }

    public void execute(ChannelHandlerContext ctx) {
        String dirPath =dirFromCloud.getPath().replace(fileHandler.getServerDirect(), "");
        Command createDirOnClient = new Command().createNewDir(dirPath);
        ctx.writeAndFlush(createDirOnClient);
        File[] contents = dirFromCloud.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    try {
                        File fileFromServer = new File(dirFromCloud + File.separator + f.getName());
                        if (fileFromServer.isFile()) {
                            Long fileSize =fileFromServer.length();
                            Command commandFile = new Command().sendFile(dirPath + File.separator
                                                                        + fileFromServer.getName(), fileSize,false);
                            ctx.writeAndFlush(commandFile);
                            SendFileFromStorageToClient sendFileFromCloudToClient = new SendFileFromStorageToClient(fileFromServer);
                            sendFileFromCloudToClient.createCommandAndSend(ctx);

                        }
                        if (fileFromServer.isDirectory()) {

                            SendDirAndFileFromStorage sendDir = new SendDirAndFileFromStorage(fileFromServer, fileHandler);
                            sendDir.execute(ctx);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
