package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.geekbrains.allcommands.*;
import java.util.logging.Level;

public class AuthHandler extends SimpleChannelInboundHandler<Command> {

    private MainServer server;
    private BaseAuthService authService;
    private FileHandler fileHandler;

    public AuthHandler(MainServer server){
        this.server = server;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) throws Exception {
        if (command.getType().equals(CommandsType.AUTH)) {
            AuthCommandData authCommand = (AuthCommandData) command.getData();
            String login = authCommand.getLogin();
            String password = authCommand.getPassword();
            authService = server.getAuthService();
            String successAuth = authService.checkAuth(login, password);
            if (successAuth != null) {
                ctx.pipeline().remove(AuthHandler.class);
                fileHandler = new FileHandler(server, login);
                ctx.pipeline().addLast(fileHandler);
                ctx.pipeline().get(FileHandler.class).channelActive(ctx);
            } else {
                Command errorAuth = new Command().error("Неверно введены логин и пароль!");
                ctx.writeAndFlush(errorAuth);
            }
        }
        if (command.getType().equals(CommandsType.END)){
            MainServer.logger.log(Level.INFO,"Получена команда END");
            Command commandEndToClient = new Command().closeConnection();
            ctx.writeAndFlush(commandEndToClient);
            ctx.close();
        }
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        MainServer.logger.log(Level.INFO,"Сервер запущен");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        MainServer.logger.log(Level.INFO,"Клиент отключился");
        ctx.close();

    }
}
