package ru.geekbrains.allcommands;

import java.io.Serializable;

public class CommandResOk implements Serializable {
    String result;
    String login;

    public CommandResOk(String result, String login) {
        this.result = result;
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public String getResult() {
        return result;
    }
}
