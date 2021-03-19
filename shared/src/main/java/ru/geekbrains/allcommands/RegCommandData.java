package ru.geekbrains.allcommands;

import java.io.Serializable;

public class RegCommandData implements Serializable {
    String login, password;

    public RegCommandData(String login, String password){
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
