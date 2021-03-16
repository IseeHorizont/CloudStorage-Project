package ru.geekbrains;

import java.sql.*;
import java.util.logging.Level;

public class BaseAuthService {

    private Connection connection;
    private PreparedStatement prepStatOfReg;
    private PreparedStatement prepStatAuth;

    public BaseAuthService(){
        try {
            connect();
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:CloudUsers.db");
    }

    public void disconnect(){
        try {
            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void statementReg() throws SQLException {
        prepStatOfReg = connection.prepareStatement("INSERT * FROM users WHERE login= ? AND password= ?;");
    }

    public void statementAuth() throws SQLException{
        prepStatAuth = connection.prepareStatement("SELECT * FROM users WHERE login= ? AND password= ?;");
    }

    public String  checkAuth(String login, String password){
        try {
            statementAuth();
            prepStatAuth.setString(1,login);
            prepStatAuth.setString(2,password);
            ResultSet rez = prepStatAuth.executeQuery();
            if (rez.next()) {
                MainServer.logger.log(Level.FINE,login + " авторизован");
                return login;
            }
            else {
                return null;
            }
        } catch (SQLException ex) {
            MainServer.logger.log(Level.SEVERE,"Ошибка базы данных");
            ex.printStackTrace();
        }
        finally {
            try {
                prepStatAuth.close();
            } catch (SQLException ex) {
                MainServer.logger.log(Level.SEVERE,"Ошибка базы данных");
                ex.printStackTrace();
            }
        }
        return null;
    }
}
