package ru.geekbrains;

import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SendFileToStorage {
    String fileToSend;
    byte[] buffer ;
    ClientNetwork network;


    public SendFileToStorage(String fileToServer, ClientNetwork network) {
        this.fileToSend=fileToServer;
        this.network=network;
        buffer=new byte[8189];


    }

    public void sendFile(ObjectEncoderOutputStream os) throws IOException {
        File fileToServer = new File(fileToSend);
        String fileNameToServer = fileToSend.replace(network.getClientDir(), "");
        Long fileSize = fileToServer.length();
        try (InputStream fis = new FileInputStream(fileToServer)) {
            int ptr;
            while (fileSize > buffer.length) {
                ptr = fis.read(buffer);
                Command file = new Command().fileToServer(fileNameToServer, buffer, ptr);
                fileSize -= ptr;
                os.writeObject(file);
                os.flush();
            }
            byte[] bufferLast = new byte[Math.toIntExact(fileSize)];
            ptr = fis.read(bufferLast);
            Command file = new Command().fileToServer(fileNameToServer, bufferLast, ptr);
            os.writeObject(file);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Command update = new Command().listFilesCommand();
        os.writeObject(update);
        os.flush();

    }
}
