package ru.geekbrains.allcommands;

import java.io.Serializable;

public class CreateDirCommandData implements Serializable {
    private String dirName;

    public CreateDirCommandData(String dirName) {
        this.dirName = dirName;
    }

    public String getDirName() {
        return dirName;
    }
}
